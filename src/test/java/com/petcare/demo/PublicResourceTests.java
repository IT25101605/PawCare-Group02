package com.petcare.demo;

import com.pawcare.demo.PawCareApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=PawCareApplication.class,properties={
    "spring.datasource.url=jdbc:h2:mem:publicresources;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.upload.dir=target/test-uploads"})
@AutoConfigureMockMvc
class PublicResourceTests {
    @Autowired MockMvc mvc;

    @Test void missingImagesAndScriptsReturnNotFoundInsteadOfServerError() throws Exception {
        for(String path:new String[]{"/uploads/absent-profile-for-resource-test.png","/images/absent-resource-test.png","/js/absent-resource-test.js"})
            mvc.perform(get(path)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested file is not available"));
    }

    @Test void cursorAndBookingAssetsRemainPublic() throws Exception {
        for(String path:new String[]{"/js/paw-cursor.js","/css/paw-cursor.css","/js/grooming-booking.js","/images/paw.svg"})
            mvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles="PET_OWNER")
    void invalidRequestsReturnClientErrorsInsteadOfInternalServerErrors() throws Exception {
        mvc.perform(get("/api/availability/slots").param("providerId","1").param("date","2999-01-01")
                        .param("kind","vet").param("durationMinutes","1"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/site/stats")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/pets").contentType("text/plain").content("invalid request"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/api/site/stats").accept("image/png"))
                .andExpect(status().isNotAcceptable()).andExpect(jsonPath("$.status").value(406));
    }
}
