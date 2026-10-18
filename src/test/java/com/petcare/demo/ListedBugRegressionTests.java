package com.petcare.demo;

import com.pawcare.demo.PawCareApplication;
import com.pawcare.demo.repository.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=PawCareApplication.class,properties={
 "spring.datasource.url=jdbc:h2:mem:listedbugs;MODE=MySQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.driverClassName=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
 "spring.jpa.hibernate.ddl-auto=create-drop","app.upload.dir=target/test-uploads","spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
class ListedBugRegressionTests {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users;
 @Autowired PetRepository pets; @Autowired SpeciesRepository species; @Autowired ProductRepository products;
 @Autowired ProductCategoryRepository categories; @Autowired GroomingServiceRepository grooming;
 @Autowired AppointmentRepository appointments; @Autowired MedicalRecordRepository records;
 @Autowired PasswordEncoder encoder; @Autowired OrderRepository orders;

 JsonNode call(String method,String path,String token,Object body,int expected) throws Exception {
  var req=request(HttpMethod.valueOf(method),path);
  if(token!=null)req.header("Authorization","Bearer "+token);
  if(body!=null)req.contentType("application/json").content(json.writeValueAsString(body));
  String content=mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
  return content.isBlank()?json.nullNode():json.readTree(content);
 }
 JsonNode callConfirmed(String method,String path,String token,Object body,String password,int expected) throws Exception {
  var req=request(HttpMethod.valueOf(method),path);
  if(token!=null)req.header("Authorization","Bearer "+token);
  req.header("X-Confirm-Password",password);
  if(body!=null)req.contentType("application/json").content(json.writeValueAsString(body));
  String content=mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
  return content.isBlank()?json.nullNode():json.readTree(content);
 }
 String login(String email,String password) throws Exception {return call("POST","/api/auth/login",null,Map.of("email",email,"password",password),200).path("token").asText();}
 String login(String email) throws Exception {return login(email,"Admin@123");}
 Map<String,Object> registration(String role) {
  return new LinkedHashMap<>(Map.of("firstName","Regression","lastName","Tester","email",UUID.randomUUID()+"@example.com","password","Testing@123","phone","0712345678","address","123 Test Road","city","Kandy","role",role));
 }
 JsonNode owner() throws Exception{return call("POST","/api/auth/register",null,registration("PET_OWNER"),200);}
 JsonNode pet(String token) throws Exception{return call("POST","/api/pets",token,Map.of("name","Test Pet","speciesId",species.findAll().getFirst().getId(),"gender","UNKNOWN","weight",5),200);}
 JsonNode appointment(String token,long pet) throws Exception {
  return call("POST","/api/appointments",token,Map.of("petId",pet,"veterinarianId",users.findByEmail("emily@pawcare.com").orElseThrow().getId(),"appointmentDate",LocalDate.now().plusDays(3+appointments.count()).toString(),"appointmentTime","10:00","reason","Routine examination"),200);
 }
 JsonNode product(String token,long cat) throws Exception {
  return call("POST","/api/products",token,Map.of("name","Test Product","sku","TEST-"+UUID.randomUUID(),"price",20,"salePrice",15,"stockQuantity",20,"reorderLevel",3,"categoryId",cat),200);
 }
 @Test void registrationApprovalAndPasswordChangesForEveryRequestedRole() throws Exception {
  String admin=login("admin@pawcare.com");
  for(String role:List.of("PET_OWNER","VETERINARIAN","GROOMER","STORE_MANAGER","SERVICE_STAFF")) {
   var input=registration(role);input.put("organization","Test Organization");input.put("licenseNumber","VET-123");input.put("qualification","BVSc");input.put("experienceYears",2);input.put("specialty","Bathing");input.put("businessRegistration","BR-123");input.put("employeeId","EMP-123");input.put("department","Billing");
   JsonNode created=call("POST","/api/auth/register",null,input,200);long id=created.path("userId").asLong();
   var user=users.findById(id).orElseThrow();assertThat(encoder.matches("Testing@123",user.getPassword())).isTrue();assertThat(user.getPassword()).isNotEqualTo("Testing@123");
   if(!role.equals("PET_OWNER")) {
    call("POST","/api/auth/login",null,Map.of("email",input.get("email"),"password","Testing@123"),403);
    call("PUT","/api/users/"+id,admin,Map.of("enabled",true),200);
   }
   String token=login((String)input.get("email"),"Testing@123");
   JsonNode profile=call("GET","/api/auth/me",token,null,200);assertThat(profile.path("id").asLong()).isEqualTo(id);assertThat(profile.path("address").asText()).isEqualTo("123 Test Road");assertThat(profile.has("password")).isFalse();
   call("PUT","/api/auth/change-password",token,Map.of("currentPassword","Wrong@123","newPassword","Updated@456"),400);
   call("PUT","/api/auth/change-password",token,Map.of("currentPassword","Testing@123","newPassword","weak"),400);
   call("PUT","/api/auth/change-password",token,Map.of("currentPassword","Testing@123","newPassword","Updated@456","userId",users.findByEmail("admin@pawcare.com").orElseThrow().getId()),200);
   call("POST","/api/auth/login",null,Map.of("email",input.get("email"),"password","Testing@123"),401);
   assertThat(login((String)input.get("email"),"Updated@456")).isNotBlank();
   assertThat(encoder.matches("Updated@456",users.findById(id).orElseThrow().getPassword())).isTrue();
   call("POST","/api/auth/register",null,input,400);
  }
  assertThat(login("admin@pawcare.com")).isNotBlank();
  call("PUT","/api/auth/change-password",null,Map.of("currentPassword","Testing@123","newPassword","Updated@456"),401);
 }
 @Test void allExistingRolesStillLoginAndSeeTheirProfiles() throws Exception {
  for(String email:List.of("admin@pawcare.com","john@pawcare.com","sarah@pawcare.com","emily@pawcare.com","michael@pawcare.com","lisa@pawcare.com","karen@pawcare.com","david@pawcare.com","tom@pawcare.com"))
   assertThat(call("GET","/api/auth/me",login(email),null,200).path("email").asText()).isEqualTo(email);
 }
 @Test void phoneValidationOnRegistrationAndProfileUpdate() throws Exception {
  for(String role:List.of("PET_OWNER","VETERINARIAN","GROOMER"))for(String phone:List.of("071234567","07123456789","07123abcde","+712345678","071 234567","")) {
   var input=registration(role);input.put("phone",phone);call("POST","/api/auth/register",null,input,400);
  }
  JsonNode owner=owner();String token=owner.path("token").asText();long id=owner.path("userId").asLong();
  for(String phone:List.of("123456789","12345678901","abcdefghij","12345-7890"))call("PUT","/api/users/"+id,token,Map.of("phone",phone),400);
  assertThat(call("PUT","/api/users/"+id,token,Map.of("phone","0771234567"),200).path("phone").asText()).isEqualTo("0771234567");
 }
 @Test void petUpdateDeleteOwnershipAndLinkedRecordProtection() throws Exception {
  String token=owner().path("token").asText(),other=owner().path("token").asText();long id=pet(token).path("id").asLong();long count=pets.count();
  JsonNode updated=callConfirmed("PUT","/api/pets/"+id,token,Map.of("name","Updated Pet","color","Brown","clearWeight",true),"Testing@123",200);
  assertThat(updated.path("id").asLong()).isEqualTo(id);assertThat(updated.path("weight").isNull()).isTrue();assertThat(pets.count()).isEqualTo(count);
  call("PUT","/api/pets/"+id,other,Map.of("name","Stolen Pet"),403);call("DELETE","/api/pets/"+id,other,null,403);
  long linked=pet(token).path("id").asLong();JsonNode appt=appointment(token,linked);
  callConfirmed("DELETE","/api/pets/"+linked,token,null,"Testing@123",204);
  assertThat(appointments.existsById(appt.path("id").asLong())).isTrue();
  assertThat(appointments.findById(appt.path("id").asLong()).orElseThrow().getStatus().name()).isEqualTo("CANCELLED");
  assertThat(pets.findById(linked).orElseThrow().getIsActive()).isFalse();
  callConfirmed("DELETE","/api/pets/"+id,token,null,"Testing@123",204);
  assertThat(pets.findById(id).orElseThrow().getIsActive()).isFalse();
  assertThat(call("GET","/api/pets/my",token,null,200).toString()).doesNotContain("Updated Pet");
 }
 @Test void appointmentOwnerEditsCancellationsAndCompletedStatusLock() throws Exception {
  String token=owner().path("token").asText(),other=owner().path("token").asText(),vet=login("emily@pawcare.com"),admin=login("admin@pawcare.com");
  long pet=pet(token).path("id").asLong(),id=appointment(token,pet).path("id").asLong();String path="/api/appointments/"+id;
  call("PUT",path,other,Map.of("reason","Unauthorized"),403);call("DELETE",path,other,null,403);
  call("PUT",path,token,Map.of("appointmentTime","11:30","reason","Updated examination"),200);
  assertThat(call("GET",path,token,null,200).path("appointmentTime").asText()).startsWith("11:30");
  call("PUT",path,token,Map.of("status","COMPLETED"),400);
  call("PUT",path,login("michael@pawcare.com"),Map.of("status","IN_PROGRESS"),403);
  call("PUT",path,vet,Map.of("status","IN_PROGRESS"),400);
  call("PUT",path,vet,Map.of("status","CONFIRMED"),200);
  call("PUT",path,vet,Map.of("status","IN_PROGRESS"),200);
  call("PUT",path,vet,Map.of("status","COMPLETED"),200);
  for(String actor:List.of(vet,admin,token))call("PUT",path,actor,Map.of("status","IN_PROGRESS"),400);
  JsonNode result=call("GET",path,vet,null,200);assertThat(result.path("status").asText()).isEqualTo("COMPLETED");assertThat(result.path("petId").asLong()).isEqualTo(pet);assertThat(result.path("id").asLong()).isEqualTo(id);
  call("DELETE",path,token,null,400);
  long cancel=appointment(token,pet).path("id").asLong();callConfirmed("PUT","/api/appointments/"+cancel,token,Map.of("status","CANCELLED"),"Testing@123",200);
  long remove=appointment(token,pet).path("id").asLong();callConfirmed("DELETE","/api/appointments/"+remove,token,null,"Testing@123",204);
  call("GET","/api/appointments/my",token,null,200);call("GET","/api/appointments/vet",vet,null,200);
 }
 @Test void groomingQueueDetailsEditsAndOwnership() throws Exception {
  String owner=owner().path("token").asText(),other=owner().path("token").asText(),groomer=login("lisa@pawcare.com");long pet=pet(owner).path("id").asLong();
  var body=Map.of("petId",pet,"serviceId",grooming.findAll().getFirst().getId(),"groomerId",users.findByEmail("lisa@pawcare.com").orElseThrow().getId(),"appointmentDate",LocalDate.now().plusDays(2).toString(),"appointmentTime","13:00","specialInstructions","Gentle handling");
  JsonNode a=call("POST","/api/grooming/appointments",owner,body,200);long id=a.path("id").asLong();String path="/api/grooming/appointments/"+id;
  JsonNode queue=call("GET","/api/grooming/appointments/groomer?size=100",groomer,null,200);
  assertThat(queue.path("content").findValuesAsText("id")).contains(String.valueOf(id));
  for(String key:List.of("petId","petName","ownerName","serviceName","appointmentDate","appointmentTime","status"))assertThat(a.path(key).isMissingNode()).isFalse();
  call("PUT",path,other,Map.of("appointmentTime","14:00"),403);call("DELETE",path,other,null,403);
  call("PUT",path,owner,Map.of("appointmentTime","14:00","specialInstructions","Updated instructions"),200);
  a=call("PUT",path,groomer,Map.of("status","CONFIRMED"),200);assertThat(a.path("groomerId").asLong()).isEqualTo(users.findByEmail("lisa@pawcare.com").orElseThrow().getId());
  callConfirmed("PUT",path,owner,Map.of("status","CANCELLED"),"Testing@123",200);
  long deleteId=call("POST","/api/grooming/appointments",owner,body,200).path("id").asLong();callConfirmed("DELETE","/api/grooming/appointments/"+deleteId,owner,null,"Testing@123",204);
 }
 @Test void medicalEditDeletePermissionsAndRelationships() throws Exception {
  String owner=owner().path("token").asText(),other=owner().path("token").asText(),vet=login("emily@pawcare.com"),admin=login("admin@pawcare.com");
  long pet=pet(owner).path("id").asLong(),appt=appointment(owner,pet).path("id").asLong();
  JsonNode record=call("POST","/api/medical-records",vet,Map.of("petId",pet,"appointmentId",appt,"diagnosis","Routine check","treatment","Routine care","prescription","Medication","recordDate",LocalDate.now().toString()),200);
  long id=record.path("id").asLong(),count=records.count();String path="/api/medical-records/"+id;
  call("GET","/api/medical-records/pet/"+pet,owner,null,200);call("GET","/api/medical-records/pet/"+pet,other,null,403);
  call("PUT",path,owner,Map.of("diagnosis","Not allowed"),403);call("PUT",path,login("michael@pawcare.com"),Map.of("diagnosis","Not allowed"),403);
  JsonNode updated=call("PUT",path,vet,Map.of("diagnosis","Revised diagnosis","prescription","","recordDate",LocalDate.now().minusDays(1).toString()),200);
  assertThat(updated.path("id").asLong()).isEqualTo(id);assertThat(updated.path("appointmentId").asLong()).isEqualTo(appt);assertThat(records.count()).isEqualTo(count);
  call("DELETE",path,vet,null,400);call("DELETE",path,owner,null,403);
  callConfirmed("DELETE",path,login("michael@pawcare.com"),null,"Admin@123",403);
  callConfirmed("DELETE",path,vet,null,"Admin@123",204);
  assertThat(records.existsById(id)).isFalse();assertThat(pets.existsById(pet)).isTrue();assertThat(appointments.existsById(appt)).isTrue();
 }
 @Test void categoryProductEditingAndAdminStoreDeletion() throws Exception {
  String store=login("karen@pawcare.com"),admin=login("admin@pawcare.com"),owner=owner().path("token").asText();String name="Category "+UUID.randomUUID();
  long cat=call("POST","/api/categories",store,Map.of("name",name),200).path("id").asLong();
  call("POST","/api/categories",store,Map.of("name",name.toUpperCase(Locale.ROOT)),400);call("POST","/api/categories",owner,Map.of("name","Forbidden"),403);
  long id=product(store,cat).path("id").asLong(),count=products.count();
  long second=call("POST","/api/categories",admin,Map.of("name","Other "+UUID.randomUUID()),200).path("id").asLong();
  JsonNode updated=call("PUT","/api/products/"+id,store,Map.of("name","Changed product","price",30,"stockQuantity",22,"reorderLevel",5,"categoryId",second,"clearSalePrice",true),200);
  assertThat(updated.path("id").asLong()).isEqualTo(id);assertThat(updated.path("reorderLevel").asInt()).isEqualTo(5);assertThat(updated.path("salePrice").isNull()).isTrue();assertThat(products.count()).isEqualTo(count);
  assertThat(call("GET","/api/products?category="+second,null,null,200).path("content").get(0).path("id").asLong()).isEqualTo(id);
  call("DELETE","/api/products/"+id,owner,null,403);callConfirmed("DELETE","/api/products/"+id,admin,null,"Admin@123",204);assertThat(categories.existsById(cat)).isTrue();assertThat(categories.existsById(second)).isTrue();
  long another=product(admin,cat).path("id").asLong();callConfirmed("DELETE","/api/products/"+another,store,null,"Admin@123",204);
 }
 @Test void cartBuyNowOrdersPaymentsAndReferencedProductDeletion() throws Exception {
  String owner=owner().path("token").asText(),other=owner().path("token").asText(),store=login("karen@pawcare.com"),admin=login("admin@pawcare.com");
  long cat=categories.findAll().getFirst().getId(),p1=product(store,cat).path("id").asLong(),p2=product(store,cat).path("id").asLong();
  JsonNode cart=call("POST","/api/cart/add",owner,Map.of("productId",p1,"quantity",2),200);long item=cart.path("items").get(0).path("id").asLong();
  assertThat(cart.path("subtotal").decimalValue()).isEqualByComparingTo("30");assertThat(cart.path("totalAmount").decimalValue()).isEqualByComparingTo("532.40");
  call("PUT","/api/cart/items/"+item,other,Map.of("quantity",1),403);call("DELETE","/api/cart/items/"+item,other,null,404);
  call("PUT","/api/cart/items/"+item,owner,Map.of("quantity",0),400);call("PUT","/api/cart/items/"+item,owner,Map.of("quantity",100),400);
  call("PUT","/api/cart/items/"+item,owner,Map.of("quantity",3),200);call("PUT","/api/cart/items/"+item,owner,Map.of("quantity",2),200);
  JsonNode quote=call("GET","/api/orders/quote?productId="+p2+"&quantity=1",owner,null,200);
  JsonNode direct=call("POST","/api/orders/checkout",owner,Map.of("shippingAddress","123 Test Road","productId",p2,"quantity",1),200);
  assertThat(direct.path("items").size()).isEqualTo(1);assertThat(direct.path("items").get(0).path("productId").asLong()).isEqualTo(p2);assertThat(direct.path("totalAmount").decimalValue()).isEqualByComparingTo(quote.path("totalAmount").decimalValue());
  assertThat(call("GET","/api/cart",owner,null,200).path("items").get(0).path("quantity").asInt()).isEqualTo(2);
  callConfirmed("DELETE","/api/products/"+p2,admin,null,"Admin@123",204);
  assertThat(products.findById(p2).orElseThrow().getIsDeleted()).isTrue();
  assertThat(products.findById(p2).orElseThrow().getIsActive()).isFalse();
  JsonNode order=call("POST","/api/orders/checkout",owner,Map.of("shippingAddress","123 Test Road"),200);
  assertThat(order.path("totalAmount").decimalValue()).isEqualByComparingTo("532.40");assertThat(call("GET","/api/cart",owner,null,200).path("items").size()).isZero();
  assertThat(products.findById(p1).orElseThrow().getStockQuantity()).isEqualTo(18);assertThat(products.findById(p2).orElseThrow().getStockQuantity()).isEqualTo(19);
  long id=order.path("id").asLong();call("POST","/api/payments/process",other,Map.of("orderId",id,"paymentMethod","SIMULATED"),403);
  call("POST","/api/payments/process",owner,Map.of("orderId",id,"paymentMethod","SIMULATED"),200);call("POST","/api/payments/process",owner,Map.of("orderId",id,"paymentMethod","SIMULATED"),400);
  call("PUT","/api/products/"+p2,store,Map.of("isActive",true),404);
  assertThat(orders.existsById(direct.path("id").asLong())).isTrue();assertThat(categories.existsById(cat)).isTrue();
  cart=call("POST","/api/cart/add",owner,Map.of("productId",p1,"quantity",1),200);item=cart.path("items").get(0).path("id").asLong();
  assertThat(call("DELETE","/api/cart/items/"+item,owner,null,200).path("items").size()).isZero();
  callConfirmed("DELETE","/api/products/"+p1,store,null,"Admin@123",204);
  assertThat(products.findById(p1).orElseThrow().getIsDeleted()).isTrue();
  var pdf=mvc.perform(get("/api/orders/"+id+"/receipt.pdf").header("Authorization","Bearer "+owner))
      .andExpect(status().isOk()).andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.containsString(".pdf")))
      .andReturn().getResponse().getContentAsByteArray();
  assertThat(pdf).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
 }
}
