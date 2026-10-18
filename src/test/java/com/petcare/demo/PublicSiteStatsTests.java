package com.petcare.demo;

import com.pawcare.demo.PawCareApplication;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.repository.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=PawCareApplication.class,properties={
    "spring.datasource.url=jdbc:h2:mem:publicstats;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.upload.dir=target/test-uploads"})
@AutoConfigureMockMvc
@Transactional
class PublicSiteStatsTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PetRepository pets;
    @Autowired SpeciesRepository species;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired GroomingServiceRepository grooming;
    @Autowired ReviewRepository reviews;
    JsonNode stats() throws Exception {
        String response=mvc.perform(get("/api/site/stats")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("email","ownerId","revenue");return json.readTree(response);
    }
    @Test void publicCountsFollowDatabaseChangesAndOnlyCountActiveCatalogueAndProfessionals() throws Exception {
        var before=stats();
        var owner=users.findByRoleName("PET_OWNER").getFirst();
        pets.saveAndFlush(Pet.builder().name("Live statistics pet").owner(owner).species(species.findAll().getFirst()).build());
        var vet=users.findByRoleName("VETERINARIAN").stream().filter(u->Boolean.TRUE.equals(u.getEnabled())).findFirst().orElseThrow();
        vet.setEnabled(false);users.saveAndFlush(vet);
        var groomer=users.findByRoleName("GROOMER").stream().filter(u->Boolean.TRUE.equals(u.getEnabled())).findFirst().orElseThrow();
        groomer.setEnabled(false);users.saveAndFlush(groomer);
        var product=products.findAll().stream().filter(p->Boolean.TRUE.equals(p.getIsActive())).findFirst().orElseThrow();
        product.setIsActive(false);products.saveAndFlush(product);
        var service=grooming.findByIsActiveTrue().getFirst();service.setIsActive(false);grooming.saveAndFlush(service);
        var after=stats();
        assertThat(after.path("pets").asLong()).isEqualTo(before.path("pets").asLong()+1);
        for(String key:java.util.List.of("veterinarians","groomers","products","groomingServices"))
            assertThat(after.path(key).asLong()).as(key).isEqualTo(before.path(key).asLong()-1);
        assertThat(after.path("updatedAt").asText()).isNotBlank();
    }
    @Test void ratingsExcludeHiddenAndUnverifiedLegacyReviewsAndHandleNoReviews() throws Exception {
        for(var review:reviews.findAll()){review.setHidden(true);reviews.saveAndFlush(review);}
        assertThat(stats().path("reviewCount").asLong()).isZero();
        assertThat(stats().path("averageRating").isNull()).isTrue();
        var owner=users.findByRoleName("PET_OWNER").getFirst();var groomer=users.findByRoleName("GROOMER").getFirst();
        reviews.saveAndFlush(Review.builder().user(owner).groomer(groomer).rating(5).reviewType(Review.ReviewType.SERVICE).build());
        reviews.saveAndFlush(Review.builder().user(owner).groomer(groomer).rating(1).hidden(true).reviewType(Review.ReviewType.SERVICE).build());
        reviews.saveAndFlush(Review.builder().user(owner).product(products.findAll().getFirst()).rating(1).reviewType(Review.ReviewType.PRODUCT).build());
        var current=stats();assertThat(current.path("reviewCount").asLong()).isEqualTo(1);
        assertThat(current.path("averageRating").asDouble()).isEqualTo(5);
    }
}
