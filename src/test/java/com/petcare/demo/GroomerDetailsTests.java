package com.petcare.demo;

import com.pawcare.demo.PawCareApplication;
import com.pawcare.demo.repository.*;
import com.pawcare.demo.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=PawCareApplication.class,properties={
    "spring.datasource.url=jdbc:h2:mem:groomdetails;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.upload.dir=target/test-uploads"})
@AutoConfigureMockMvc @Transactional
class GroomerDetailsTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired StakeholderProfileRepository profiles;
    Map<String,Object> details(){return Map.of("organization","  My independent salon  ","qualification","  My grooming apprenticeship  ","specialty","  Nail trimming and gentle baths  ","experienceYears",0);}
    @Test void adminCanSaveCustomDetailsAndTheyAppearInPublicChooserData() throws Exception {
        var groomer=users.findByRoleName("GROOMER").getFirst();
        mvc.perform(put("/api/users/"+groomer.getId()+"/groomer-details").with(user("admin").roles("ADMIN"))
                .contentType("application/json").content(json.writeValueAsString(details())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.organization").value("My independent salon"));
        var stored=profiles.findById(groomer.getId()).orElseThrow();
        assertThat(stored.getSpecialty()).isEqualTo("Nail trimming and gentle baths");
        assertThat(stored.getQualification()).isEqualTo("My grooming apprenticeship");assertThat(stored.getExperienceYears()).isZero();
        String response=mvc.perform(get("/api/groomers")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(response).contains("Nail trimming and gentle baths","My independent salon");
        mvc.perform(get("/api/users/me/groomer-details").with(user(CustomUserDetails.build(groomer))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.qualification").value("My grooming apprenticeship"));
    }
    @Test void groomerWithoutLegacyProfileCanCreateOwnDetails() throws Exception {
        var groomer=users.findByRoleName("GROOMER").getFirst();profiles.deleteById(groomer.getId());profiles.flush();
        mvc.perform(get("/api/users/me/groomer-details").with(user(CustomUserDetails.build(groomer))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.organization").isEmpty());
        assertThat(profiles.findById(groomer.getId())).isEmpty();
        mvc.perform(put("/api/users/me/groomer-details").with(user(CustomUserDetails.build(groomer)))
                .contentType("application/json").content(json.writeValueAsString(details())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.specialty").value("Nail trimming and gentle baths"));
        assertThat(profiles.findById(groomer.getId())).isPresent();
    }
    @Test void onlyAdminCanEditAnotherGroomerAndOtherRolesCannotUseTheseFields() throws Exception {
        var groomer=users.findByRoleName("GROOMER").getFirst();String path="/api/users/"+groomer.getId()+"/groomer-details";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        for(String role:java.util.List.of("PET_OWNER","GROOMER"))mvc.perform(put(path).with(user("other").roles(role)).contentType("application/json").content(json.writeValueAsString(details()))).andExpect(status().isForbidden());
        mvc.perform(put("/api/users/me/groomer-details").with(user("owner").roles("PET_OWNER")).contentType("application/json").content(json.writeValueAsString(details()))).andExpect(status().isForbidden());
        var owner=users.findByRoleName("PET_OWNER").getFirst();
        mvc.perform(put("/api/users/"+owner.getId()+"/groomer-details").with(user("admin").roles("ADMIN")).contentType("application/json").content(json.writeValueAsString(details()))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/999999/groomer-details").with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
    }
    @Test void invalidExperienceAndTooLongQualificationsDoNotChangeSavedDetails() throws Exception {
        var groomer=users.findByRoleName("GROOMER").getFirst();String before=json.writeValueAsString(profiles.findById(groomer.getId()).map(p->p.getSpecialty()).orElse(null));
        var invalid=new java.util.LinkedHashMap<>(details());invalid.put("experienceYears",61);
        String path="/api/users/"+groomer.getId()+"/groomer-details";
        mvc.perform(put(path).with(user("admin").roles("ADMIN")).contentType("application/json").content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
        invalid.put("experienceYears",2);invalid.put("qualification","x".repeat(201));
        mvc.perform(put(path).with(user("admin").roles("ADMIN")).contentType("application/json").content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
        assertThat(json.writeValueAsString(profiles.findById(groomer.getId()).map(p->p.getSpecialty()).orElse(null))).isEqualTo(before);
    }
}
