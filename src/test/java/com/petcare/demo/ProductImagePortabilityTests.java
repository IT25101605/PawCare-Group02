package com.petcare.demo;

import com.pawcare.demo.PawCareApplication;
import com.pawcare.demo.entity.Product;
import com.pawcare.demo.repository.ProductRepository;
import com.pawcare.demo.repository.UserRepository;
import com.pawcare.demo.service.CartService;
import com.pawcare.demo.service.ProductImageService;
import com.pawcare.demo.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=PawCareApplication.class,properties={
        "spring.datasource.url=jdbc:h2:mem:productimages;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false",
        "app.upload.dir=target/portable-image-test-uploads"})
@AutoConfigureMockMvc
@Transactional
class ProductImagePortabilityTests {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired ProductService catalog;
    @Autowired CartService carts;
    @Autowired ProductImageService images;

    @Test void freshDatabaseCatalogPhotosAreBundledAndPublic() throws Exception {
        assertThat(products.count()).isGreaterThanOrEqualTo(8);
        for (Product product : products.findAll()) {
            String url = catalog.getById(product.getId()).getImage();
            assertThat(url).startsWith("/images/products/").endsWith(".png");
            mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        }
        mvc.perform(get("/images/products/soap.png")).andExpect(status().isOk());
    }

    @Test void oldDatabaseAndMissingUploadsUseMatchingPhotosWithoutOverwritingSavedData() throws Exception {
        Product product = products.findBySku("PC-DOG-FOOD").orElseThrow();
        product.setPrice(new BigDecimal("1234.00"));
        product.setStockQuantity(7);
        for (String oldPath : new String[]{null, "", "/uploads/00000000-0000-0000-0000-000000000000.png",
                "C:\\another-laptop\\dog-food.png", "/images/products/dog-food.svg"}) {
            product.setImage(oldPath);
            products.saveAndFlush(product);
            mvc.perform(get("/api/products/" + product.getId()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.image").value("/images/products/dog-food.png"));
            assertThat(product.getImage()).isEqualTo(oldPath);
            assertThat(product.getPrice()).isEqualByComparingTo("1234.00");
            assertThat(product.getStockQuantity()).isEqualTo(7);
        }
    }

    @Test void workingCustomUploadsArePreservedAndServed() throws Exception {
        Path upload = Path.of("target/portable-image-test-uploads", UUID.randomUUID() + ".png");
        Files.createDirectories(upload.getParent());
        byte[] photo = new ClassPathResource("static/images/products/cat-food.png").getContentAsByteArray();
        Files.write(upload, photo);
        try {
            Product product = products.findBySku("PC-DOG-FOOD").orElseThrow();
            product.setImage("/uploads/" + upload.getFileName());
            products.saveAndFlush(product);
            String url = catalog.getById(product.getId()).getImage();
            assertThat(url).isEqualTo(product.getImage());
            mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().bytes(photo));
        } finally {
            Files.deleteIfExists(upload);
        }
    }

    @Test void soapAndUnknownProductsHavePortableImagesAndUnsafePathsAreNotReturned() throws Exception {
        assertThat(images.resolve(Product.builder().sku("BCK212").build())).isEqualTo("/images/products/soap.png");
        for (String oldPath : new String[]{null, "/images/products/missing.png", "/uploads/../../private.png",
                "file:///C:/missing.png", "https://unavailable.example/photo.png"}) {
            String url = images.resolve(Product.builder().sku("CUSTOM-ITEM").image(oldPath).build());
            assertThat(url).isEqualTo("/images/products/placeholder.svg");
            mvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test void cartUsesTheSameRecoveredPhotoAsTheShop() {
        Product product = products.findBySku("PC-BALL").orElseThrow();
        product.setImage(null);
        products.saveAndFlush(product);
        long owner = users.findByEmail("john@pawcare.com").orElseThrow().getId();
        Map<String,Object> cart = carts.addItem(owner, product.getId(), 1);
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> items = (List<Map<String,Object>>) cart.get("items");
        assertThat(items).anySatisfy(item -> {
            assertThat(item.get("productId")).isEqualTo(product.getId());
            assertThat(item.get("image")).isEqualTo("/images/products/ball.png");
        });
    }
}
