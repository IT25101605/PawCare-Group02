package com.pawcare.demo.repository;

import com.pawcare.demo.entity.ProviderUnavailableDate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface ProviderUnavailableDateRepository extends JpaRepository<ProviderUnavailableDate,Long> {
    boolean existsByProviderIdAndUnavailableDate(Long providerId,LocalDate date);
    List<ProviderUnavailableDate> findByProviderIdAndUnavailableDateGreaterThanEqualOrderByUnavailableDate(Long providerId,LocalDate date);
    Optional<ProviderUnavailableDate> findByIdAndProviderId(Long id,Long providerId);
}
