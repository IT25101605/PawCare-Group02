package com.pawcare.demo.repository;

import com.pawcare.demo.entity.GroomingAppointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.List;

public interface GroomingAppointmentRepository extends JpaRepository<GroomingAppointment, Long> {
    @Query("""
        SELECT CASE WHEN COUNT(g)>0 THEN true ELSE false END FROM GroomingAppointment g
        WHERE g.service.id=:serviceId
          OR CONCAT(CONCAT(',', COALESCE(g.selectedServiceIds, '')), ',') LIKE :selection
        """)
    boolean existsUsingService(@Param("serviceId") Long serviceId, @Param("selection") String selection);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GroomingAppointment g WHERE g.id = :id")
    java.util.Optional<GroomingAppointment> findForUpdate(@Param("id") Long id);

    Page<GroomingAppointment> findByOwnerId(Long ownerId, Pageable pageable);
    Page<GroomingAppointment> findByGroomerId(Long groomerId, Pageable pageable);
    List<GroomingAppointment> findByGroomerIdAndAppointmentDateOrderByAppointmentTimeAsc(Long groomerId, LocalDate date);
    Page<GroomingAppointment> findByGroomerIdOrGroomerIsNull(Long groomerId, Pageable pageable);
    long countByGroomerId(Long groomerId);
    long countByStatus(GroomingAppointment.GroomingStatus status);
    List<GroomingAppointment> findByPetId(Long petId);

    @Query("""
        SELECT CASE WHEN COUNT(g)>0 THEN true ELSE false END FROM GroomingAppointment g
        WHERE g.groomer.id=:groomerId AND g.appointmentDate=:date AND g.appointmentTime=:time
          AND g.status NOT IN (com.pawcare.demo.entity.GroomingAppointment.GroomingStatus.CANCELLED,
                               com.pawcare.demo.entity.GroomingAppointment.GroomingStatus.COMPLETED)
          AND (:excludeId < 0 OR g.id<>:excludeId)
        """)
    boolean existsActiveSlot(@Param("groomerId") Long groomerId, @Param("date") LocalDate date,
                             @Param("time") LocalTime time, @Param("excludeId") Long excludeId);

    @Query("""
        SELECT g FROM GroomingAppointment g
        LEFT JOIN FETCH g.service legacyService
        WHERE g.groomer.id=:groomerId AND g.appointmentDate=:date
          AND g.status NOT IN (com.pawcare.demo.entity.GroomingAppointment.GroomingStatus.CANCELLED,
                               com.pawcare.demo.entity.GroomingAppointment.GroomingStatus.COMPLETED)
        ORDER BY g.appointmentTime ASC
        """)
    List<GroomingAppointment> findActiveForGroomerDate(@Param("groomerId") Long groomerId,
                                                        @Param("date") LocalDate date);

    java.util.List<GroomingAppointment> findByAppointmentDateBetween(LocalDate start, LocalDate end);
}
