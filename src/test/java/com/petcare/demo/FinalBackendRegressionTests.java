package com.petcare.demo;

import com.pawcare.demo.PawCareApplication;
import com.pawcare.demo.config.GroomingSchemaCompatibilityInitializer;
import com.pawcare.demo.dto.PetDTO;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.BadRequestException;
import com.pawcare.demo.repository.*;
import com.pawcare.demo.security.CustomUserDetails;
import com.pawcare.demo.security.JwtTokenProvider;
import com.pawcare.demo.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.IntSupplier;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest(classes=PawCareApplication.class,properties={
        "spring.datasource.url=jdbc:h2:mem:finalbackend;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driverClassName=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false",
        "app.upload.dir=target/final-backend-test-uploads"})
@AutoConfigureMockMvc
class FinalBackendRegressionTests {
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired SpeciesRepository species;
    @Autowired ProductCategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired InvoiceRepository invoices;
    @Autowired GroomingServiceRepository groomingServices;
    @Autowired GroomingAppointmentRepository groomingAppointments;
    @Autowired PetService pets;
    @Autowired OrderService checkout;
    @Autowired PaymentService billing;
    @Autowired CartService carts;
    @Autowired GroomingSchemaCompatibilityInitializer migration;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired Environment environment;

    @AfterEach void clearAuthentication(){SecurityContextHolder.clearContext();}

    private User owner(){return users.save(User.builder().firstName("Final").lastName("Check")
            .email(UUID.randomUUID()+"@example.com").password("unused-test-only")
            .enabled(true).role(roles.findByName("PET_OWNER").orElseThrow()).city("Kandy").build());}
    private void signIn(User user){
        var principal=CustomUserDetails.build(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
    }
    private Product product(int stock){return products.save(Product.builder().name("Final check product")
            .sku("CHECK-"+UUID.randomUUID()).price(new BigDecimal("100.00")).stockQuantity(stock)
            .category(categories.findAll().getFirst()).build());}
    private PetDTO pet(User owner){signIn(owner);return pets.createPet(PetDTO.builder().name("Final Pet")
            .gender("UNKNOWN").speciesId(species.findAll().getFirst().getId()).build(),owner.getId());}
    private int parallel(IntSupplier first,IntSupplier second) throws Exception {
        var barrier=new CyclicBarrier(2);
        try(var executor=Executors.newFixedThreadPool(2)){
            var a=executor.submit(()->{barrier.await(5,TimeUnit.SECONDS);try{return first.getAsInt();}finally{SecurityContextHolder.clearContext();}});
            var b=executor.submit(()->{barrier.await(5,TimeUnit.SECONDS);try{return second.getAsInt();}finally{SecurityContextHolder.clearContext();}});
            return a.get(20,TimeUnit.SECONDS)+b.get(20,TimeUnit.SECONDS);
        }
    }

    @Test void petEditsPersistCustomSpeciesAndBreedAndReuseExistingNames(){
        User owner=owner();PetDTO original=pet(owner);
        String name="Species "+UUID.randomUUID(),breed="Breed "+UUID.randomUUID();
        var update=PetDTO.builder().customSpeciesName(name).customBreedName(breed).build();
        PetDTO changed=pets.updatePet(original.getId(),update);
        assertThat(changed.getSpeciesName()).isEqualTo(name);
        assertThat(changed.getBreedName()).isEqualTo(breed);
        PetDTO repeated=pets.updatePet(original.getId(),PetDTO.builder().customSpeciesName(name.toLowerCase())
                .customBreedName(breed.toLowerCase()).build());
        assertThat(repeated.getSpeciesId()).isEqualTo(changed.getSpeciesId());
        assertThat(repeated.getBreedId()).isEqualTo(changed.getBreedId());
    }

    @Test void twoBuyersCannotBuyTheSameLastProduct() throws Exception {
        User first=owner(),second=owner();Product product=product(1);
        IntSupplier a=()->{signIn(first);try{checkout.buyNow(first.getId(),"123 Test Road",product.getId(),1);return 1;}catch(BadRequestException ex){return 0;}};
        IntSupplier b=()->{signIn(second);try{checkout.buyNow(second.getId(),"123 Test Road",product.getId(),1);return 1;}catch(BadRequestException ex){return 0;}};
        assertThat(parallel(a,b)).isEqualTo(1);
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isZero();
    }

    @Test void doubleCartCheckoutCreatesOnlyOneOrder() throws Exception {
        User owner=owner();Product product=product(5);signIn(owner);carts.addItem(owner.getId(),product.getId(),2);
        IntSupplier attempt=()->{signIn(owner);try{checkout.createFromCart(owner.getId(),"123 Test Road");return 1;}catch(BadRequestException ex){return 0;}};
        assertThat(parallel(attempt,attempt)).isEqualTo(1);
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
    }

    @Test void doublePaymentCreatesOnlyOnePaymentAndInvoice() throws Exception {
        User owner=owner();Product product=product(2);signIn(owner);
        long id=checkout.buyNow(owner.getId(),"123 Test Road",product.getId(),1).getId();
        IntSupplier attempt=()->{signIn(owner);try{billing.processPayment(id,owner.getId(),"CASH",null,null,null,null);return 1;}catch(BadRequestException ex){return 0;}};
        assertThat(parallel(attempt,attempt)).isEqualTo(1);
        var pageable=org.springframework.data.domain.PageRequest.of(0,10);
        assertThat(payments.findByOrderId(id,pageable).getTotalElements()).isEqualTo(1);
        assertThat(invoices.findByOrderId(id,pageable).getTotalElements()).isEqualTo(1);
    }

    @Test void cancellingReleasesStockOnceAndCannotReopenTheOrder(){
        User owner=owner();Product product=product(5);signIn(owner);
        long id=checkout.buyNow(owner.getId(),"123 Test Road",product.getId(),2).getId();
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
        checkout.updateStatus(id,"CANCELLED");checkout.updateStatus(id,"CANCELLED");
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
        assertThatThrownBy(()->checkout.updateStatus(id,"PROCESSING")).isInstanceOf(BadRequestException.class);
    }

    @Test void legacyGroomingMigrationPreservesLaterEditsOnRepeatedStartup(){
        User owner=owner();PetDTO pet=pet(owner);var services=groomingServices.findAll().stream().limit(2).toList();
        var first=services.getFirst();var second=services.getLast();
        var booking=groomingAppointments.save(GroomingAppointment.builder()
                .pet(com.pawcare.demo.entity.Pet.builder().id(pet.getId()).build()).owner(owner).service(first)
                .appointmentDate(LocalDate.now().plusDays(50)).appointmentTime(LocalTime.NOON)
                .totalPrice(first.getPrice()).build());
        jdbc.execute("CREATE TABLE IF NOT EXISTS grooming_appointment_services (grooming_appointment_id BIGINT NOT NULL,grooming_service_id BIGINT NOT NULL,PRIMARY KEY(grooming_appointment_id,grooming_service_id))");
        for(var service:services)jdbc.update("INSERT INTO grooming_appointment_services VALUES (?,?)",booking.getId(),service.getId());
        migration.run(new DefaultApplicationArguments());
        String selected=jdbc.queryForObject("SELECT selected_service_ids FROM grooming_appointments WHERE id=?",String.class,booking.getId());
        assertThat(selected).contains(first.getId().toString(),second.getId().toString());
        jdbc.update("UPDATE grooming_appointments SET selected_service_ids=?,selected_service_names=?,total_duration_minutes=? WHERE id=?",
                second.getId().toString(),"Edited service snapshot",95,booking.getId());
        migration.run(new DefaultApplicationArguments());
        assertThat(jdbc.queryForObject("SELECT selected_service_ids FROM grooming_appointments WHERE id=?",String.class,booking.getId())).isEqualTo(second.getId().toString());
        assertThat(jdbc.queryForObject("SELECT selected_service_names FROM grooming_appointments WHERE id=?",String.class,booking.getId())).isEqualTo("Edited service snapshot");
        assertThat(jdbc.queryForObject("SELECT total_duration_minutes FROM grooming_appointments WHERE id=?",Integer.class,booking.getId())).isEqualTo(95);
    }

    @Test void missingInvalidAndExpiredTokensReturn401SoMobileCanRecover() throws Exception {
        String expired=new JwtTokenProvider(environment.getRequiredProperty("app.jwt.secret"),-10000)
                .generateTokenFromEmail("john@pawcare.com",1L,"PET_OWNER");
        for(String path:List.of("/api/auth/me","/api/pets/my")){
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            for(String token:List.of("not-a-token",expired))
                mvc.perform(get(path).header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/products").header("Authorization","Bearer not-a-token")).andExpect(status().isOk());
    }

    @Test void serviceStaffCanReadTheirOrderOverviewButCannotChangeDeliveryStatus() throws Exception {
        mvc.perform(get("/api/orders?page=0").with(user("billing").roles("SERVICE_STAFF"))).andExpect(status().isOk());
        mvc.perform(get("/api/orders?page=0").with(user("owner").roles("PET_OWNER"))).andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/orders/1/status")
                .with(user("billing").roles("SERVICE_STAFF")).contentType("application/json")
                .content("{\"status\":\"PROCESSING\"}")).andExpect(status().isForbidden());
    }

    @Test void freshDemoProfilesHavePhonesAcceptedByTheirEditForms(){
        for(String email:List.of("admin@pawcare.com","john@pawcare.com","sarah@pawcare.com","emily@pawcare.com",
                "michael@pawcare.com","lisa@pawcare.com","karen@pawcare.com","david@pawcare.com","tom@pawcare.com"))
            assertThat(users.findByEmail(email).orElseThrow().getPhone()).matches("[0-9]{10}");
    }

    @Test void verifyingCashUpdatesItsInvoiceAndRefundKeepsPaymentAndOrderInSync(){
        User owner=owner();Product product=product(2);signIn(owner);
        long id=checkout.buyNow(owner.getId(),"123 Test Road",product.getId(),1).getId();
        long payment=billing.processPayment(id,owner.getId(),"CASH",null,null,null,null).getId();
        billing.updatePaymentStatus(payment,"COMPLETED");
        var invoice=invoices.findByOrderId(id).getFirst();
        assertThat(invoice.getStatus()).isEqualTo(Invoice.InvoiceStatus.PAID);
        assertThat(invoice.getPaidDate()).isEqualTo(LocalDate.now());
        assertThatThrownBy(()->checkout.updateStatus(id,"CANCELLED")).isInstanceOf(BadRequestException.class);
        billing.updatePaymentStatus(payment,"REFUNDED");
        billing.updatePaymentStatus(payment,"REFUNDED");checkout.updateStatus(id,"REFUNDED");
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(2);
        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(Order.OrderStatus.REFUNDED);
        assertThat(invoices.findByOrderId(id).getFirst().getStatus()).isEqualTo(Invoice.InvoiceStatus.CANCELLED);
        assertThatThrownBy(()->billing.updatePaymentStatus(payment,"COMPLETED")).isInstanceOf(BadRequestException.class);
    }

    @Test void cancellingCashOrderCancelsItsDraftInvoiceAndBlocksLaterCollection(){
        User owner=owner();Product product=product(2);signIn(owner);
        long id=checkout.buyNow(owner.getId(),"123 Test Road",product.getId(),1).getId();
        long payment=billing.processPayment(id,owner.getId(),"CASH",null,null,null,null).getId();
        checkout.updateStatus(id,"CANCELLED");
        assertThat(payments.findById(payment).orElseThrow().getPaymentStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
        assertThat(invoices.findByOrderId(id).getFirst().getStatus()).isEqualTo(Invoice.InvoiceStatus.CANCELLED);
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(2);
        assertThatThrownBy(()->billing.updatePaymentStatus(payment,"COMPLETED")).isInstanceOf(BadRequestException.class);
    }

    @Test void orderRefundUpdatesItsCompletedPaymentAndInvoice(){
        User owner=owner();Product product=product(2);signIn(owner);
        long id=checkout.buyNow(owner.getId(),"123 Test Road",product.getId(),1).getId();
        long payment=billing.processPayment(id,owner.getId(),"CASH",null,null,null,null).getId();
        billing.updatePaymentStatus(payment,"COMPLETED");
        checkout.updateStatus(id,"REFUNDED");
        checkout.updateStatus(id,"REFUNDED");billing.updatePaymentStatus(payment,"REFUNDED");
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(2);
        assertThat(payments.findById(payment).orElseThrow().getPaymentStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        assertThat(invoices.findByOrderId(id).getFirst().getStatus()).isEqualTo(Invoice.InvoiceStatus.CANCELLED);
    }

    @Test void shippedOrDeliveredRefundsDoNotRestockGoodsBeforePhysicalReturn(){
        for(String delivery:List.of("SHIPPED","DELIVERED"))for(boolean refundThroughPayment:List.of(false,true)){
            User owner=owner();Product product=product(3);signIn(owner);
            long id=checkout.buyNow(owner.getId(),"123 Test Road",product.getId(),2).getId();
            long payment=billing.processPayment(id,owner.getId(),"CASH",null,null,null,null).getId();
            billing.updatePaymentStatus(payment,"COMPLETED");checkout.updateStatus(id,delivery);
            if(refundThroughPayment)billing.updatePaymentStatus(payment,"REFUNDED");else checkout.updateStatus(id,"REFUNDED");
            assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(1);
            assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(Order.OrderStatus.REFUNDED);
            assertThat(payments.findById(payment).orElseThrow().getPaymentStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        }
    }

    @Test void simultaneousRefundRoutesReleaseUnshippedStockOnlyOnce() throws Exception {
        User owner=owner();Product product=product(3);signIn(owner);
        long id=checkout.buyNow(owner.getId(),"123 Test Road",product.getId(),2).getId();
        long payment=billing.processPayment(id,owner.getId(),"CASH",null,null,null,null).getId();
        billing.updatePaymentStatus(payment,"COMPLETED");
        assertThat(parallel(()->{signIn(owner);checkout.updateStatus(id,"REFUNDED");return 1;},
                ()->{signIn(owner);billing.updatePaymentStatus(payment,"REFUNDED");return 1;})).isEqualTo(2);
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
    }
}
