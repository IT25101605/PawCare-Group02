package com.petcare.demo;
import com.pawcare.demo.PawCareApplication;
import com.pawcare.demo.config.CatalogSeeder;
import com.pawcare.demo.repository.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import java.time.LocalDate;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(classes=PawCareApplication.class,properties={
 "spring.datasource.url=jdbc:h2:mem:pawcaretest;MODE=MySQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.driverClassName=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
 "spring.jpa.hibernate.ddl-auto=create-drop","app.upload.dir=target/test-uploads","spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
class DemoApplicationTests {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired SpeciesRepository species;
 @Autowired BreedRepository breeds; @Autowired ProductRepository products; @Autowired ProductCategoryRepository categories;
 @Autowired UserRepository users; @Autowired StakeholderProfileRepository profiles;
 @Autowired ContactMessageRepository messages; @Autowired CatalogSeeder seeder;
 @Autowired GroomingServiceRepository grooming;
 String login(String email) throws Exception {
  return json.readTree(mvc.perform(post("/api/auth/login").contentType("application/json")
   .content(json.writeValueAsString(Map.of("email",email,"password","Admin@123"))))
   .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
 }
 Map<String,Object> registration(String role) {
  var r=new LinkedHashMap<String,Object>();
  r.put("firstName","Test");r.put("lastName","Person");r.put("email",UUID.randomUUID()+"@example.com");
  r.put("phone","0712345678");r.put("password","Testing@123");r.put("role",role);
  r.put("address","123 Test Road");r.put("city","Kandy");return r;
 }
 long speciesId(String name) {return species.findAll().stream().filter(s->s.getName().equals(name)).findFirst().orElseThrow().getId();}
 Map<String,Object> pet() {return new LinkedHashMap<>(Map.of("name","Buddy","speciesId",speciesId("Dog"),"gender","UNKNOWN","weight",4.2));}
 JsonNode createPet(String token) throws Exception {
  return json.readTree(mvc.perform(post("/api/pets").header("Authorization","Bearer "+token).contentType("application/json")
   .content(json.writeValueAsString(pet()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
 }
 @Test void referenceDataAndCatalogAreIdempotentAndSerializable() throws Exception {
  long sp=species.count(),br=breeds.count(),pr=products.count(); seeder.run();
  assertThat(species.count()).isEqualTo(sp);assertThat(breeds.count()).isEqualTo(br);assertThat(products.count()).isEqualTo(pr);
  for(String path:List.of("/api/species","/api/breeds","/api/breeds/species/"+speciesId("Dog"),"/api/products","/api/categories","/images/products/dog-food.svg"))
   mvc.perform(get(path)).andExpect(status().isOk());
 }
 @Test void ownerRegistrationNormalizesEmailAndRejectsDuplicates() throws Exception {
  var r=registration("PET_OWNER");r.put("email",((String)r.get("email")).toUpperCase(Locale.ROOT));
  mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
  mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",r.get("email"),"password","Testing@123")))).andExpect(status().isOk());
  mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isBadRequest());
 }
 @Test void eachProfessionalRoleRequiresItsDetailsAndApproval() throws Exception {
  for(String role:List.of("VETERINARIAN","GROOMER","STORE_MANAGER","SERVICE_STAFF")) {
   var r=registration(role);
   mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").exists());
   r.put("organization","Test Practice");r.put("experienceYears",2);r.put("licenseNumber","VET-123");r.put("qualification","BVSc");
   r.put("specialty","Dog grooming");r.put("businessRegistration","BR-123");r.put("employeeId","EMP-123");r.put("department","Billing");
   var result=json.readTree(mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
   assertThat(result.get("token").isNull()).isTrue();
   assertThat(profiles.findById(result.get("userId").asLong()).orElseThrow().getOrganization()).isEqualTo("Test Practice");
   mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",r.get("email"),"password","Testing@123")))).andExpect(status().isForbidden());
  }
 }
 @Test void registrationRejectsAdminWeakPasswordAndInvalidExperience() throws Exception {
  var r=registration("ADMIN");
  mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isBadRequest());
  r.put("role","PET_OWNER");r.put("password","abcdefgh");
  mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isBadRequest());
  r.put("password","Testing@123");r.put("experienceYears",-1);
  mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isBadRequest());
 }
 @Test void petsPersistListAndValidatePartialUpdatesAndOwnership() throws Exception {
  String token=login("john@pawcare.com");var saved=createPet(token);
  mvc.perform(get("/api/pets/my").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$[0].speciesName").value("Dog"));
  mvc.perform(put("/api/pets/"+saved.get("id").asLong()).header("Authorization","Bearer "+token).header("X-Confirm-Password","Admin@123").contentType("application/json").content("{\"name\":\"Updated\"}")).andExpect(status().isOk());
  for(var bad:List.of(Map.of("name","  "),Map.of("weight",-1),Map.of("dateOfBirth",LocalDate.now().plusDays(1).toString()),Map.of("gender","INVALID"))) {
   var p=pet();p.putAll(bad);
   mvc.perform(post("/api/pets").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(p))).andExpect(status().isBadRequest());
  }
  long catBreed=breeds.findBySpeciesId(speciesId("Cat")).getFirst().getId();var p=pet();p.put("breedId",catBreed);
  mvc.perform(post("/api/pets").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(p))).andExpect(status().isBadRequest());
  mvc.perform(put("/api/pets/"+saved.get("id").asLong()).header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(Map.of("breedId",catBreed)))).andExpect(status().isBadRequest());
  mvc.perform(put("/api/pets/"+saved.get("id").asLong()).header("Authorization","Bearer "+login("sarah@pawcare.com")).contentType("application/json").content("{\"name\":\"Stolen\"}")).andExpect(status().isForbidden());
  mvc.perform(put("/api/users/"+users.findByEmail("john@pawcare.com").orElseThrow().getId()).header("Authorization","Bearer "+token).contentType("application/json").content("{\"role\":\"ADMIN\"}")).andExpect(status().isForbidden());
 }
 @Test void petImageUploadChecksContentsAndSizeAndServesStoredImage() throws Exception {
  String token=login("john@pawcare.com");var out=new ByteArrayOutputStream();
  ImageIO.write(new BufferedImage(20,20,BufferedImage.TYPE_INT_RGB),"png",out);
  var request=new MockMultipartFile("pet","","application/json",json.writeValueAsBytes(pet()));
  var image=new MockMultipartFile("photo","pet.png","image/png",out.toByteArray());
  var result=json.readTree(mvc.perform(multipart("/api/pets").file(request).file(image).header("Authorization","Bearer "+token))
   .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  mvc.perform(get(result.get("photo").asText())).andExpect(status().isOk());
  for(byte[] bytes:List.of("not an image".getBytes(),new byte[5*1024*1024+1]))
   mvc.perform(multipart("/api/pets").file(request).file(new MockMultipartFile("photo","bad.png","image/png",bytes)).header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
 }
 @Test void appointmentRequiresVetAndFutureDateAndAllowsStatusOnlyUpdates() throws Exception {
  String token=login("john@pawcare.com");long id=createPet(token).get("id").asLong();
  var a=new LinkedHashMap<String,Object>(Map.of("petId",id,"appointmentDate",LocalDate.now().plusDays(2).toString(),"appointmentTime","09:00","reason","Annual checkup"));
  mvc.perform(post("/api/appointments").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(a))).andExpect(status().isBadRequest());
  a.put("veterinarianId",users.findByEmail("emily@pawcare.com").orElseThrow().getId());
  var result=json.readTree(mvc.perform(post("/api/appointments").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(a))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  mvc.perform(put("/api/appointments/"+result.get("id").asLong()).header("Authorization","Bearer "+login("emily@pawcare.com")).contentType("application/json").content("{\"status\":\"CONFIRMED\"}")).andExpect(status().isOk());
  a.put("appointmentDate",LocalDate.now().minusDays(1).toString());
  mvc.perform(post("/api/appointments").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(a))).andExpect(status().isBadRequest());
  mvc.perform(get("/api/appointments/my").header("Authorization","Bearer "+token)).andExpect(status().isOk());
 }
 @Test void productValidationIncludesSaleStockSkuAndImageUpload() throws Exception {
  String token=login("karen@pawcare.com");
  var p=new LinkedHashMap<String,Object>(Map.of("name","New product","price",12.50,"stockQuantity",4,"categoryId",categories.findAll().getFirst().getId(),"sku","TEST-"+UUID.randomUUID()));
  p.put("salePrice",15);
  mvc.perform(post("/api/products").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(p))).andExpect(status().isBadRequest());
  p.remove("salePrice");p.put("stockQuantity",-1);
  mvc.perform(post("/api/products").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(p))).andExpect(status().isBadRequest());
  p.put("stockQuantity",4);var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB),"png",out);
  mvc.perform(multipart("/api/products").file(new MockMultipartFile("product","","application/json",json.writeValueAsBytes(p)))
   .file(new MockMultipartFile("image","product.png","image/png",out.toByteArray())).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.image").isString());
  mvc.perform(post("/api/products").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(p))).andExpect(status().isBadRequest());
 }
 @Test void cartChecksQuantityStockAndShippingAddress() throws Exception {
  String token=login("john@pawcare.com");long id=products.findAll().getFirst().getId();
  mvc.perform(delete("/api/cart/clear").header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
  mvc.perform(post("/api/cart/add").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(Map.of("productId",id,"quantity",-1)))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/cart/add").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(Map.of("productId",id,"quantity",1)))).andExpect(status().isOk()).andExpect(jsonPath("$.itemCount").value(1));
  mvc.perform(post("/api/cart/add").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(Map.of("productId",id,"quantity",1000000)))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/orders/checkout").header("Authorization","Bearer "+token).contentType("application/json").content("{\"shippingAddress\":\" \"}")).andExpect(status().isBadRequest());
 }
 @Test void contactMessagesAreValidatedAndPersisted() throws Exception {
  long before=messages.count();
  mvc.perform(post("/api/contact").contentType("application/json").content("{\"name\":\"Test\",\"email\":\"bad\",\"subject\":\"Help\",\"message\":\"Hello there, I need help.\"}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/contact").contentType("application/json").content("{\"name\":\"Test\",\"email\":\"test@example.com\",\"subject\":\"Help\",\"message\":\"Hello there, I need help.\"}")).andExpect(status().isOk());
  assertThat(messages.count()).isEqualTo(before+1);
 }
 @Test void groomingRequiresTimeAndPersistsWithServerPrice() throws Exception {
  String token=login("john@pawcare.com");
  var data=new LinkedHashMap<String,Object>(Map.of("petId",createPet(token).get("id").asLong(),
   "serviceId",grooming.findAll().getFirst().getId(),"groomerId",users.findByEmail("lisa@pawcare.com").orElseThrow().getId(),"appointmentDate",LocalDate.now().plusDays(2).toString()));
  mvc.perform(post("/api/grooming/appointments").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isBadRequest());
  data.put("appointmentTime","10:00");data.put("totalPrice",0);
  mvc.perform(post("/api/grooming/appointments").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isOk()).andExpect(jsonPath("$.totalPrice").value(grooming.findAll().getFirst().getPrice().doubleValue()));
  mvc.perform(get("/api/grooming/appointments/my").header("Authorization","Bearer "+token)).andExpect(status().isOk());
 }
 @Test void administratorCanReviewAndApproveProfessionalApplication() throws Exception {
  var data=registration("GROOMER");data.put("organization","Test Salon");data.put("specialty","Dog grooming");data.put("experienceYears",0);
  var result=json.readTree(mvc.perform(post("/api/auth/register").contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  String admin=login("admin@pawcare.com");long id=result.get("userId").asLong();
  mvc.perform(get("/api/users/"+id+"/professional-profile").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.specialty").value("Dog grooming"));
  mvc.perform(put("/api/users/"+id).header("Authorization","Bearer "+admin).contentType("application/json").content("{\"enabled\":true}")).andExpect(status().isOk());
  var authenticated=json.readTree(mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",data.get("email"),"password","Testing@123")))).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("GROOMER")).andReturn().getResponse().getContentAsString());
  mvc.perform(put("/api/users/"+id).header("Authorization","Bearer "+admin).contentType("application/json").content("{\"enabled\":false}")).andExpect(status().isOk());
  mvc.perform(get("/api/grooming/appointments/groomer").header("Authorization","Bearer "+authenticated.get("token").asText())).andExpect(status().isUnauthorized());
 }
 @Test void medicalRecordValidatesMeasurementsAndDate() throws Exception {
  long petId=createPet(login("john@pawcare.com")).get("id").asLong();String vet=login("emily@pawcare.com");
  var record=new LinkedHashMap<String,Object>(Map.of("petId",petId,"diagnosis","Checkup","treatment","Routine care","recordDate",LocalDate.now().toString()));
  record.put("weightAtVisit",-2);
  mvc.perform(post("/api/medical-records").header("Authorization","Bearer "+vet).contentType("application/json").content(json.writeValueAsString(record))).andExpect(status().isBadRequest());
  record.put("weightAtVisit",5);record.put("recordDate",LocalDate.now().plusDays(1).toString());
  mvc.perform(post("/api/medical-records").header("Authorization","Bearer "+vet).contentType("application/json").content(json.writeValueAsString(record))).andExpect(status().isBadRequest());
  record.put("recordDate",LocalDate.now().toString());
  mvc.perform(post("/api/medical-records").header("Authorization","Bearer "+vet).contentType("application/json").content(json.writeValueAsString(record))).andExpect(status().isOk());
  mvc.perform(get("/api/medical-records/pet/"+petId).header("Authorization","Bearer "+vet)).andExpect(status().isOk());
 }
}
