package com.pawcare.demo.controller;
import com.pawcare.demo.dto.StoreAnalyticsDTO;
import com.pawcare.demo.service.StoreAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/analytics")
public class AnalyticsController {
 private final StoreAnalyticsService service; public AnalyticsController(StoreAnalyticsService service){this.service=service;}
 @GetMapping("/store") @PreAuthorize("hasAnyRole('STORE_MANAGER','ADMIN')") public ResponseEntity<StoreAnalyticsDTO> store(){return ResponseEntity.ok(service.get());}
}
