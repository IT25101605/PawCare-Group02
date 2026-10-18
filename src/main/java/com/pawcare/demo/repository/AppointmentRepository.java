package com.pawcare.demo.repository;

import com.pawcare.demo.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.id = :id")
    java.util.Optional<Appointment> findForUpdate(@Param("id") Long id);

    Page<Appointment> findByOwnerId(Long ownerId, Pageable pageable);
    Page<Appointment> findByVeterinarianId(Long vetId, Pageable pageable);
    List<Appointment> findByPetId(Long petId);
    boolean existsByPetIdAndVeterinarianId(Long petId, Long veterinarianId);
    List<Appointment> findByAppointmentDate(LocalDate date);

    @Query("SELECT a FROM Appointment a WHERE a.status = :status")
    Page<Appointment> findByStatus(@Param("status") Appointment.AppointmentStatus status, Pageable pageable);

    long countByStatus(Appointment.AppointmentStatus status);
    long countByVeterinarianId(Long vetId);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate BETWEEN :start AND :end")
    List<Appointment> findByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
        SELECT CASE WHEN COUNT(a)>0 THEN true ELSE false END FROM Appointment a
        WHERE a.veterinarian.id=:vetId AND a.appointmentDate=:date AND a.appointmentTime=:time
          AND a.status NOT IN (com.pawcare.demo.entity.Appointment.AppointmentStatus.CANCELLED,
                               com.pawcare.demo.entity.Appointment.AppointmentStatus.NO_SHOW,
                               com.pawcare.demo.entity.Appointment.AppointmentStatus.COMPLETED)
          AND (:excludeId < 0 OR a.id<>:excludeId)
        """)
    boolean existsActiveSlot(@Param("vetId") Long vetId, @Param("date") LocalDate date,
                             @Param("time") LocalTime time, @Param("excludeId") Long excludeId);

    List<Appointment> findByAppointmentDateBetween(LocalDate start, LocalDate end);
    List<Appointment> findByAppointmentDateBeforeAndStatusIn(LocalDate date, java.util.Collection<Appointment.AppointmentStatus> statuses);
}
