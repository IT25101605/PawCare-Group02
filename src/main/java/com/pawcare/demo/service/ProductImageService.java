package com.pawcare.demo.service;

import com.pawcare.demo.entity.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

@Service
public class ProductImageService {
    private static final String PREFIX = "/images/products/";
    private static final String PLACEHOLDER = PREFIX + "placeholder.svg";
    private static final Map<String, String> CATALOG_IMAGES = Map.ofEntries(
            Map.entry("PC-DOG-FOOD", "dog-food"), Map.entry("PC-CAT-FOOD", "cat-food"),
            Map.entry("PC-ROPE", "rope"), Map.entry("PC-BALL", "ball"),
            Map.entry("PC-COLLAR", "collar"), Map.entry("PC-BOWL", "bowl"),
            Map.entry("PC-SHAMPOO", "shampoo"), Map.entry("PC-BED", "bed"),
            Map.entry("BCK212", "soap"));

    private final ResourceLoader resources;
    private final Path uploads;

    public ProductImageService(ResourceLoader resources, @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.resources = resources;
        this.uploads = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public String resolve(Product product) {
        String stored = product.getImage();
        String file = CATALOG_IMAGES.get(product.getSku() == null ? "" : product.getSku().toUpperCase(Locale.ROOT));
        String bundled = file == null ? null : PREFIX + file + ".png";

        // Older databases may have no image, an old SVG, a machine-local path or a missing upload.
        // Keep a working custom photo; use the supplied project photo for known catalog products.
        if (file != null && (PREFIX + file + ".svg").equals(stored) && available(bundled)) return bundled;
        if (available(stored)) return stored;
        if (available(bundled)) return bundled;
        return PLACEHOLDER;
    }

    private boolean available(String url) {
        if (url == null) return false;
        if (url.matches("/images/products/[a-z0-9-]+\\.(?:png|svg)")) {
            return resources.getResource("classpath:/static" + url).exists();
        }
        if (url.matches("/uploads/[a-f0-9-]+\\.png")) {
            return Files.isRegularFile(uploads.resolve(url.substring("/uploads/".length())));
        }
        return false;
    }
}
