package com.petcare.demo;

import com.pawcare.demo.PawCareApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes=PawCareApplication.class, properties={
    "spring.datasource.url=jdbc:h2:mem:serviceadmin;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "app.upload.dir=target/test-uploads"})
@AutoConfigureMockMvc
@Transactional
class GroomingServiceManagementTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired com.pawcare.demo.repository.GroomingServiceRepository services;
    @Autowired com.pawcare.demo.config.CatalogSeeder seeder;
    final java.util.List<java.nio.file.Path> uploadedFiles = new java.util.ArrayList<>();
    @org.junit.jupiter.api.AfterEach void removeTestImages() throws Exception {
        for(var file:uploadedFiles) java.nio.file.Files.deleteIfExists(file);
    }
    byte[] imageBytes() throws Exception {
        var bytes=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(4,4,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
        return bytes.toByteArray();
    }
    org.springframework.mock.web.MockMultipartFile servicePart(String body) {
        return new org.springframework.mock.web.MockMultipartFile("service","service.json","application/json",body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    java.nio.file.Path uploadedPath(String url) {
        assertThat(url).matches("/uploads/[a-f0-9-]+\\.png");
        var root=java.nio.file.Path.of("target/test-uploads").toAbsolutePath().normalize();
        var file=root.resolve(url.substring("/uploads/".length())).normalize();
        assertThat(file.startsWith(root)).isTrue();uploadedFiles.add(file);return file;
    }
    String service(String name, int duration, double price, boolean active) throws Exception {
        return json.writeValueAsString(Map.of("name",name,"description","Gentle coat care","durationMinutes",duration,"price",price,"isActive",active));
    }
    @Test void adminCanCreateEditAndDisablePersistedServices() throws Exception {
        String response=mvc.perform(post("/api/grooming/services").with(user("admin").roles("ADMIN"))
                .contentType("application/json").content(service("  Test Coat Care  ",45,1800.50,true)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Test Coat Care"))
                .andReturn().getResponse().getContentAsString();
        long id=json.readTree(response).path("id").asLong();
        String active=mvc.perform(get("/api/grooming/services")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(active).findValuesAsText("name")).contains("Test Coat Care");
        mvc.perform(put("/api/grooming/services/"+id).with(user("admin").roles("ADMIN"))
                .contentType("application/json").content(service("Updated Coat Care",60,2100,false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isActive").value(false));
        active=mvc.perform(get("/api/grooming/services")).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(active).findValuesAsText("name")).doesNotContain("Updated Coat Care");
        String managed=mvc.perform(get("/api/grooming/services/manage").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(managed).findValuesAsText("name")).contains("Updated Coat Care");
    }
    @Test void managementRejectsUnauthorizedUsersAndInvalidValues() throws Exception {
        mvc.perform(post("/api/grooming/services").contentType("application/json").content(service("Bath",30,1000,true))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/grooming/services").with(user("owner").roles("PET_OWNER"))
                .contentType("application/json").content(service("Bath",30,1000,true))).andExpect(status().isForbidden());
        mvc.perform(get("/api/grooming/services/manage").with(user("groomer").roles("GROOMER"))).andExpect(status().isForbidden());
        for(String invalid: new String[]{service(" ",30,1000,true),service("Bath",0,1000,true),service("Bath",481,1000,true),service("Bath",30,-1,true),service("Bath",30,1.234,true)}) {
            mvc.perform(post("/api/grooming/services").with(user("admin").roles("ADMIN"))
                    .contentType("application/json").content(invalid)).andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/grooming/services/999999").with(user("admin").roles("ADMIN"))
                .contentType("application/json").content(service("Bath",30,1000,true))).andExpect(status().isNotFound());
    }
    @Test void uploadsPersistAndExistingServicesCanReplaceOrRemoveImages() throws Exception {
        var image=new org.springframework.mock.web.MockMultipartFile("image","groom.png","image/png",imageBytes());
        String response=mvc.perform(multipart("/api/grooming/services").file(image).file(servicePart(service("Photo Service",30,1400,true))).with(user("admin").roles("ADMIN")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var created=json.readTree(response);long id=created.path("id").asLong();String first=created.path("image").asText();
        var firstPath=uploadedPath(first);assertThat(java.nio.file.Files.exists(firstPath)).isTrue();
        mvc.perform(get(first)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(put("/api/grooming/services/"+id).with(user("admin").roles("ADMIN")).contentType("application/json").content(service("Updated Photo Service",35,1500,true)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.image").value(first));
        response=mvc.perform(multipart("/api/grooming/services/"+id).file(image).file(servicePart(service("Updated Photo Service",35,1500,true)))
                .with(request->{request.setMethod("PUT");return request;}).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second=json.readTree(response).path("image").asText();var secondPath=uploadedPath(second);
        assertThat(second).isNotEqualTo(first);assertThat(java.nio.file.Files.exists(firstPath)).isFalse();assertThat(java.nio.file.Files.exists(secondPath)).isTrue();
        var data=json.readTree(service("Updated Photo Service",35,1500,true));((com.fasterxml.jackson.databind.node.ObjectNode)data).put("removeImage",true);
        mvc.perform(put("/api/grooming/services/"+id).with(user("admin").roles("ADMIN")).contentType("application/json").content(data.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.image").doesNotExist());
        assertThat(java.nio.file.Files.exists(secondPath)).isFalse();
        String managed=mvc.perform(get("/api/grooming/services/manage").with(user("admin").roles("ADMIN"))).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(managed).findValuesAsText("name")).contains("Updated Photo Service");
    }
    @Test void existingServiceAcceptsFirstImageAndBadUploadsDoNotChangeIt() throws Exception {
        String created=mvc.perform(post("/api/grooming/services").with(user("admin").roles("ADMIN")).contentType("application/json").content(service("Existing Service",30,1000,true)))
                .andReturn().getResponse().getContentAsString();
        long id=json.readTree(created).path("id").asLong();
        var photo=new org.springframework.mock.web.MockMultipartFile("image","../photo.png","image/png",imageBytes());
        var request=multipart("/api/grooming/services/"+id).file(photo).file(servicePart(service("Existing Service",30,1000,true))).with(req->{req.setMethod("PUT");return req;});
        mvc.perform(request.with(user("groomer").roles("GROOMER"))).andExpect(status().isForbidden());
        String updated=mvc.perform(request.with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String url=json.readTree(updated).path("image").asText();assertThat(java.nio.file.Files.exists(uploadedPath(url))).isTrue();
        for(var bad:java.util.List.of(
                new org.springframework.mock.web.MockMultipartFile("image","fake.png","image/png","not an image".getBytes()),
                new org.springframework.mock.web.MockMultipartFile("image","large.png","image/png",new byte[5*1024*1024+1]))) {
            mvc.perform(multipart("/api/grooming/services/"+id).file(bad).file(servicePart(service("Existing Service",30,1000,true)))
                    .with(req->{req.setMethod("PUT");return req;}).with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
        }
        String managed=mvc.perform(get("/api/grooming/services/manage").with(user("admin").roles("ADMIN"))).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(managed).findValuesAsText("image")).contains(url);
    }

    @Test void onlyAdminCanDeleteUnusedServices() throws Exception {
        String response=mvc.perform(post("/api/grooming/services").with(user("admin").roles("ADMIN"))
                .contentType("application/json").content(service("Temporary Service",20,500,true)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id=json.readTree(response).path("id").asLong();
        mvc.perform(delete("/api/grooming/services/"+id)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/grooming/services/"+id).with(user("groomer").roles("GROOMER"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/grooming/services/"+id).with(user("admin").roles("ADMIN"))).andExpect(status().isNoContent());
        mvc.perform(delete("/api/grooming/services/"+id).with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
        String publicServices=mvc.perform(get("/api/grooming/services")).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(publicServices).findValuesAsText("name")).doesNotContain("Temporary Service");
    }
    @Test void renderingLibrariesArePublicAndCachedForNavigation() throws Exception {
        for(String asset:java.util.List.of("/vendor/bootstrap/bootstrap.min.css","/vendor/bootstrap/bootstrap.bundle.min.js",
                "/vendor/bootstrap-icons/bootstrap-icons.css","/vendor/chart/chart.umd.min.js","/vendor/fonts/fonts.css")) {
            mvc.perform(get(asset)).andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("public")))
                    .andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("max-age=2592000")));
        }
    }
    @Test void restartingCatalogueDoesNotRestoreDeletedServicesOrOverwriteAdminEdits() throws Exception {
        var bath=services.findAll().stream().filter(s->s.getName().equals("Basic Bath")).findFirst().orElseThrow();
        bath.setPrice(new java.math.BigDecimal("3333.00"));bath.setDescription("Admin description");
        bath.setDurationMinutes(60);bath.setImage("/images/grooming-services/Basic_Bath.png");
        services.saveAndFlush(bath);
        mvc.perform(delete("/api/grooming/services/"+bath.getId()).with(user("admin").roles("ADMIN"))).andExpect(status().isNoContent());
        seeder.run();
        var retained=services.findById(bath.getId()).orElseThrow();
        assertThat(retained.isDeleted()).isTrue();assertThat(retained.getIsActive()).isFalse();
        assertThat(retained.getPrice()).isEqualByComparingTo("3333.00");
        assertThat(retained.getDescription()).isEqualTo("Admin description");assertThat(retained.getDurationMinutes()).isEqualTo(60);
        assertThat(retained.getImage()).isEqualTo("/images/grooming-services/Basic_Bath.png");
    }
}
