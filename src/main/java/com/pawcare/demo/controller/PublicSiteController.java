package com.pawcare.demo.controller;

import com.pawcare.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
public class PublicSiteController {
    private final PetRepository pets;
    private final UserRepository users;
    private final ProductRepository products;
    private final GroomingServiceRepository grooming;
    private final ReviewRepository reviews;

    public record Stats(long pets, long veterinarians, long groomers, long products,
                        long groomingServices, long reviewCount, Double averageRating, Instant updatedAt) {}

    @GetMapping("/stats")
    @Transactional(readOnly=true)
    public ResponseEntity<Stats> stats() {
        long count=reviews.countPublicReviews();
        Stats stats=new Stats(pets.count(),users.countByRoleNameAndEnabledTrue("VETERINARIAN"),
                users.countByRoleNameAndEnabledTrue("GROOMER"),products.countByIsActiveTrueAndIsDeletedFalse(),
                grooming.countByIsActiveTrue(),count,count==0?null:reviews.averagePublicRating(),Instant.now());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(stats);
    }
}
