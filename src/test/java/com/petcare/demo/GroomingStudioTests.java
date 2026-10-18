package com.petcare.demo;

import com.pawcare.demo.PawCareApplication;
import com.pawcare.demo.entity.GroomingService;
import com.pawcare.demo.repository.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=PawCareApplication.class,properties={
 "spring.datasource.url=jdbc:h2:mem:groomstudio;MODE=MySQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.driverClassName=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
 "spring.jpa.hibernate.ddl-auto=create-drop","app.upload.dir=target/test-uploads","spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@Transactional
class GroomingStudioTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired SpeciesRepository species;
    @Autowired GroomingServiceRepository services;

    JsonNode call(String method,String path,String token,Object body,int expected) throws Exception {
        var req=request(HttpMethod.valueOf(method),path);
        if(token!=null)req.header("Authorization","Bearer "+token);
        if(body!=null)req.contentType("application/json").content(json.writeValueAsString(body));
        String content=mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return content.isBlank()?json.nullNode():json.readTree(content);
    }
    String login(String email) throws Exception {
        return call("POST","/api/auth/login",null,Map.of("email",email,"password","Admin@123"),200).path("token").asText();
    }
    String owner() throws Exception {
        return call("POST","/api/auth/register",null,Map.of("firstName","Studio","lastName","Tester","email",UUID.randomUUID()+"@example.com","password","Testing@123","phone","0712345678","address","123 Test Road","city","Kandy","role","PET_OWNER"),200).path("token").asText();
    }
    long pet(String token,String kind) throws Exception {
        var type=species.findAll().stream().filter(s->s.getName().equalsIgnoreCase(kind)).findFirst().orElseThrow();
        return call("POST","/api/pets",token,Map.of("name","Studio Pet","speciesId",type.getId(),"gender","UNKNOWN"),200).path("id").asLong();
    }
    GroomingService service(String name) { return services.findAll().stream().filter(s->s.getName().equals(name)).findFirst().orElseThrow(); }
    Map<String,Object> booking(long pet) {
        return new LinkedHashMap<>(Map.of("petId",pet,"serviceIds",List.of(service("Basic Bath").getId()),
                "groomerId",users.findByEmail("lisa@pawcare.com").orElseThrow().getId(),
                "appointmentDate",LocalDate.now().plusDays(60).toString(),"appointmentTime","10:00","comfortProfile","NERVOUS"));
    }
    Map<String,Object> plan(long pet,int budget,int minutes,String goal,String comfort) {
        return Map.of("petId",pet,"budget",budget,"maxMinutes",minutes,"goal",goal,"comfort",comfort);
    }
    @Test void plannerHonoursBudgetTimeAndComfortWithoutDuplicateBaths() throws Exception {
        String owner=owner();long pet=pet(owner,"Dog");
        JsonNode fresh=call("POST","/api/grooming/plan",owner,plan(pet,3500,60,"FRESH_COAT","CALM"),200);
        assertThat(fresh.path("services").size()).isEqualTo(1);
        assertThat(fresh.path("services").get(0).path("name").asText()).isEqualTo("Basic Bath");
        assertThat(fresh.path("totalPrice").decimalValue()).isEqualByComparingTo("2800");
        JsonNode tidy=call("POST","/api/grooming/plan",owner,plan(pet,2000,45,"QUICK_TIDY","FIRST_VISIT"),200);
        assertThat(tidy.path("services").size()).isEqualTo(2);
        assertThat(tidy.path("totalMinutes").asInt()).isEqualTo(45);
        JsonNode nervous=call("POST","/api/grooming/plan",owner,plan(pet,10000,180,"FULL_STYLE","NERVOUS"),200);
        assertThat(nervous.path("visitLimitMinutes").asInt()).isEqualTo(45);
        assertThat(nervous.path("services").isEmpty()).isTrue();
        assertThat(nervous.path("explanations").toString()).contains("No matching service");
        JsonNode low=call("POST","/api/grooming/plan",owner,plan(pet,1,15,"FRESH_COAT","CALM"),200);
        assertThat(low.path("services").isEmpty()).isTrue();
    }
    @Test void plannerUsesActiveCurrentCatalogueAndDeclinesUnsupportedSpecies() throws Exception {
        String owner=owner();long pet=pet(owner,"Cat");
        var bath=service("Basic Bath");bath.setIsActive(false);services.saveAndFlush(bath);
        JsonNode result=call("POST","/api/grooming/plan",owner,plan(pet,3500,60,"FRESH_COAT","CALM"),200);
        assertThat(result.path("services").get(0).path("name").asText()).isEqualTo("De-shedding Treatment");
        assertThat(result.path("totalPrice").decimalValue()).isEqualByComparingTo(service("De-shedding Treatment").getPrice());
        JsonNode bird=call("POST","/api/grooming/plan",owner,plan(pet(owner,"Bird"),10000,180,"FULL_STYLE","CALM"),200);
        assertThat(bird.path("services").isEmpty()).isTrue();
        assertThat(bird.path("explanations").toString()).contains("dogs and cats");
    }
    @Test void plannerValidatesInputsAndPetOwnership() throws Exception {
        String owner=owner();long pet=pet(owner,"Dog");
        call("POST","/api/grooming/plan",owner(),plan(pet,3500,60,"FRESH_COAT","CALM"),403);
        call("POST","/api/grooming/plan",login("lisa@pawcare.com"),plan(pet,3500,60,"FRESH_COAT","CALM"),403);
        call("POST","/api/grooming/plan",owner,plan(pet,-1,60,"FRESH_COAT","CALM"),400);
        call("POST","/api/grooming/plan",owner,plan(pet,3500,181,"FRESH_COAT","CALM"),400);
        call("POST","/api/grooming/plan",owner,plan(pet,3500,60,"UNKNOWN","CALM"),400);
    }
    @Test void bookingDetailsArePrivateAndComfortTravelsWithVisit() throws Exception {
        String owner=owner();long pet=pet(owner,"Dog");
        var created=call("POST","/api/grooming/appointments",owner,booking(pet),200);
        String path="/api/grooming/appointments/"+created.path("id").asLong();
        assertThat(created.path("comfortProfile").asText()).isEqualTo("NERVOUS");
        call("GET",path,owner(),null,403);
        for(String email:List.of("emily@pawcare.com","david@pawcare.com","karen@pawcare.com"))call("GET",path,login(email),null,403);
        String groomer=login("lisa@pawcare.com");
        assertThat(call("GET",path,groomer,null,200).path("comfortProfile").asText()).isEqualTo("NERVOUS");
        call("PUT",path,groomer,Map.of("notes","Handled calmly. Bath completed."),200);
        assertThat(call("GET",path,owner,null,200).path("notes").asText()).contains("Handled calmly");
        call("PUT",path,owner,Map.of("notes","Forged staff recap"),400);
        var forged=booking(pet);forged.put("notes","Forged on creation");
        call("POST","/api/grooming/appointments",owner,forged,400);
        call("PUT",path,owner,Map.of("comfortProfile",""),200);
        assertThat(call("GET",path,owner,null,200).path("comfortProfile").asText()).isEmpty();
    }
    @Test void terminalVisitsCannotBeReopenedAndCompletedVisitCanReceiveRecap() throws Exception {
        String owner=owner(),groomer=login("lisa@pawcare.com"),admin=login("admin@pawcare.com");
        var created=call("POST","/api/grooming/appointments",owner,booking(pet(owner,"Dog")),200);
        String path="/api/grooming/appointments/"+created.path("id").asLong();
        call("PUT",path,groomer,Map.of("status","COMPLETED"),400);
        call("PUT",path,groomer,Map.of("status","CONFIRMED"),200);
        call("PUT",path,groomer,Map.of("status","SCHEDULED"),400);
        call("PUT",path,groomer,Map.of("status","IN_PROGRESS"),200);
        call("PUT",path,groomer,Map.of("status","COMPLETED","notes","A relaxed visit.","completedTasks",List.of("CHECK_IN","BATH","DRY","BRUSH","FINAL_CHECK")),200);
        for(String actor:List.of(groomer,admin,owner))call("PUT",path,actor,Map.of("status","IN_PROGRESS"),400);
        call("PUT",path,groomer,Map.of("appointmentTime","12:00"),400);
        call("PUT",path,groomer,Map.of("notes","Recap with agreed follow-up."),200);
        assertThat(call("GET",path,owner,null,200).path("status").asText()).isEqualTo("COMPLETED");
        var cancelled=booking(pet(owner,"Dog"));cancelled.put("appointmentTime","14:00");
        String cancelPath="/api/grooming/appointments/"+call("POST","/api/grooming/appointments",owner,cancelled,200).path("id").asLong();
        call("PUT",cancelPath,groomer,Map.of("status","CANCELLED"),200);
        call("PUT",cancelPath,admin,Map.of("status","SCHEDULED"),400);
    }
    @Test void unchangedServicesRetainAgreedPriceAndDurationAndCannotOverlap() throws Exception {
        String owner=owner();long pet=pet(owner,"Dog");var body=booking(pet);
        var created=call("POST","/api/grooming/appointments",owner,body,200);
        var bath=service("Basic Bath");bath.setPrice(new BigDecimal("9000"));bath.setDurationMinutes(120);services.saveAndFlush(bath);
        String path="/api/grooming/appointments/"+created.path("id").asLong();
        var result=call("PUT",path,owner,Map.of("serviceIds",body.get("serviceIds"),"appointmentTime","11:00"),200);
        assertThat(result.path("totalPrice").decimalValue()).isEqualByComparingTo(created.path("totalPrice").decimalValue());
        assertThat(result.path("totalDurationMinutes").asInt()).isEqualTo(45);
        var conflict=booking(pet);conflict.put("appointmentTime","11:30");
        call("POST","/api/grooming/appointments",owner,conflict,400);
    }
    @Test void dayQueueIsDateFilteredAndScopedToSignedInGroomer() throws Exception {
        String owner=owner();var body=booking(pet(owner,"Dog"));
        call("POST","/api/grooming/appointments",owner,body,200);
        String path="/api/grooming/appointments/groomer/day?date="+body.get("appointmentDate");
        assertThat(call("GET",path,login("lisa@pawcare.com"),null,200).size()).isEqualTo(1);
        var lisa=users.findByEmail("lisa@pawcare.com").orElseThrow();
        String otherEmail=UUID.randomUUID()+"@example.com";
        users.saveAndFlush(com.pawcare.demo.entity.User.builder().email(otherEmail).password(lisa.getPassword())
                .firstName("Other").lastName("Groomer").role(lisa.getRole()).enabled(true).build());
        assertThat(call("GET",path,login(otherEmail),null,200).isEmpty()).isTrue();
        call("GET",path,owner,null,403);
        assertThat(call("GET","/api/grooming/appointments/groomer/day?date="+LocalDate.now().plusDays(61),login("lisa@pawcare.com"),null,200).isEmpty()).isTrue();
    }

    @Test void lateEveningAvailabilityTerminatesWithoutWrappingIntoAnotherDay() throws Exception {
        String groomer=login("lisa@pawcare.com");LocalDate date=LocalDate.now().plusDays(65);
        call("PUT","/api/availability/me",groomer,Map.of("dayOfWeek",date.getDayOfWeek().name(),"startTime","23:00","endTime","23:59","active",true),200);
        long provider=users.findByEmail("lisa@pawcare.com").orElseThrow().getId();
        var result=call("GET","/api/availability/slots?providerId="+provider+"&date="+date+"&kind=groomer&durationMinutes=15",groomer,null,200);
        assertThat(result.toString()).isEqualTo("[\"23:00\",\"23:30\"]");
    }
    @Test void checklistReflectsServicesAndOwnerCannotForgeProgressOrReports() throws Exception {
        String owner=owner(),groomer=login("lisa@pawcare.com");
        var body=booking(pet(owner,"Dog"));body.put("serviceIds",List.of(service("Nail Trimming").getId()));
        var created=call("POST","/api/grooming/appointments",owner,body,200);
        String path="/api/grooming/appointments/"+created.path("id").asLong();
        assertThat(created.path("visitTasks").findValuesAsText("code")).containsExactly("CHECK_IN","NAILS","FINAL_CHECK");
        call("PUT",path,owner,Map.of("completedTasks",List.of("CHECK_IN")),400);
        call("PUT",path,owner,Map.of("productsUsed","Forged"),400);
        call("PUT",path,groomer,Map.of("completedTasks",List.of("CHECK_IN")),400);
        call("PUT",path,groomer,Map.of("status","IN_PROGRESS"),400);
        call("PUT",path,groomer,Map.of("status","CONFIRMED"),200);
        var started=call("PUT",path,groomer,Map.of("status","IN_PROGRESS"),200);
        assertThat(started.path("progressLabel").asText()).isEqualTo("Nail care");
        call("PUT",path,groomer,Map.of("completedTasks",List.of("CHECK_IN","BATH")),400);
        call("PUT",path,groomer,Map.of("completedTasks",List.of("FINAL_CHECK")),400);
        call("PUT",path,groomer,Map.of("status","COMPLETED"),400);
        var done=call("PUT",path,groomer,Map.of("completedTasks",List.of("CHECK_IN","NAILS","FINAL_CHECK")),200);
        assertThat(done.path("progressLabel").asText()).isEqualTo("Ready for pickup");
        call("PUT",path,groomer,Map.of("status","COMPLETED"),200);
        call("PUT",path,groomer,Map.of("completedTasks",List.of("CHECK_IN")),400);
        assertThat(call("GET",path,owner,null,200).path("completedTasks").size()).isEqualTo(3);
    }
    @Test void finishPreferenceRequiresMatchingServiceAndReportStoresProductsAndNextVisit() throws Exception {
        String owner=owner(),groomer=login("lisa@pawcare.com");var body=booking(pet(owner,"Dog"));
        body.put("preferredFinish","SHORT_NEAT");
        call("POST","/api/grooming/appointments",owner,body,400);
        body.put("serviceIds",List.of(service("Nail Trimming").getId()));
        call("POST","/api/grooming/appointments",owner,body,400);
        body.put("serviceIds",List.of(service("Hygiene Trim").getId()));
        var created=call("POST","/api/grooming/appointments",owner,body,200);
        assertThat(created.path("preferredFinish").asText()).isEqualTo("SHORT_NEAT");
        String path="/api/grooming/appointments/"+created.path("id").asLong();
        LocalDate next=LocalDate.parse(created.path("appointmentDate").asText()).plusWeeks(6);
        call("PUT",path,owner,Map.of("nextVisitDate",next.toString()),400);
        call("PUT",path,groomer,Map.of("nextVisitDate",created.path("appointmentDate").asText()),400);
        call("PUT",path,groomer,Map.of("notes","Trim completed. Owner preference followed.","productsUsed","Paw balm","nextVisitDate",next.toString()),200);
        var report=call("GET",path,owner,null,200);
        assertThat(report.path("productsUsed").asText()).isEqualTo("Paw balm");
        assertThat(report.path("nextVisitDate").asText()).isEqualTo(next.toString());
        call("PUT",path,groomer,Map.of("productsUsed","","clearNextVisitDate",true),200);
        assertThat(call("GET",path,owner,null,200).path("nextVisitDate").isNull()).isTrue();
    }
    @Test void deactivatedBookedServicesCanBeDeletedWithoutLosingPrimaryAndSecondaryVisitRecords() throws Exception {
        String owner=owner(),admin=login("admin@pawcare.com");
        var body=booking(pet(owner,"Dog"));
        var bath=service("Basic Bath");var nails=service("Nail Trimming");
        body.put("serviceIds",List.of(bath.getId(),nails.getId()));
        var booked=call("POST","/api/grooming/appointments",owner,body,200);
        for(Long id:List.of(bath.getId(),nails.getId())) {
            var service=services.findById(id).orElseThrow();
            var details=Map.of("name",service.getName(),"description","Disabled for new bookings","durationMinutes",service.getDurationMinutes(),"price",service.getPrice(),"isActive",false);
            call("PUT","/api/grooming/services/"+id,admin,details,200);
            call("DELETE","/api/grooming/services/"+id,admin,null,204);
            assertThat(services.findById(id).orElseThrow().isDeleted()).isTrue();
            call("PUT","/api/grooming/services/"+id,admin,details,404);
            assertThat(call("GET","/api/grooming/services/manage",admin,null,200).findValuesAsText("name")).doesNotContain(service.getName());
            assertThat(call("GET","/api/grooming/services",null,null,200).findValuesAsText("name")).doesNotContain(service.getName());
        }
        var retained=call("GET","/api/grooming/appointments/"+booked.path("id").asLong(),owner,null,200);
        assertThat(retained.path("serviceIds").size()).isEqualTo(2);
        assertThat(retained.path("totalPrice").decimalValue()).isEqualByComparingTo(booked.path("totalPrice").decimalValue());
        body.put("appointmentDate",LocalDate.now().plusDays(67).toString());
        call("POST","/api/grooming/appointments",owner,body,400);
    }

    @Test void rebookedVisitUsesCurrentPriceAndDoesNotCopyOldTasksOrReports() throws Exception {
        String owner=owner(),groomer=login("lisa@pawcare.com");var body=booking(pet(owner,"Dog"));body.put("preferredFinish","KEEP_LENGTH");
        var created=call("POST","/api/grooming/appointments",owner,body,200);
        String path="/api/grooming/appointments/"+created.path("id").asLong();
        call("PUT",path,groomer,Map.of("status","CONFIRMED"),200);
        call("PUT",path,groomer,Map.of("status","IN_PROGRESS"),200);
        call("PUT",path,groomer,Map.of("status","COMPLETED","completedTasks",List.of("CHECK_IN","BATH","DRY","BRUSH","FINAL_CHECK"),"notes","First visit report"),200);
        var bath=service("Basic Bath");bath.setPrice(new BigDecimal("3100"));services.saveAndFlush(bath);
        body.put("appointmentDate",LocalDate.now().plusDays(67).toString());
        var repeated=call("POST","/api/grooming/appointments",owner,body,200);
        assertThat(repeated.path("totalPrice").decimalValue()).isEqualByComparingTo("3100");
        assertThat(repeated.path("preferredFinish").asText()).isEqualTo("KEEP_LENGTH");
        assertThat(repeated.path("completedTasks").isEmpty()).isTrue();
        assertThat(repeated.path("notes").isNull()).isTrue();
        assertThat(repeated.path("status").asText()).isEqualTo("SCHEDULED");
    }

}
