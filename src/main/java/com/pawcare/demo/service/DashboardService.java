package com.pawcare.demo.service;

import com.pawcare.demo.dto.DashboardDTO;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.repository.*;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@org.springframework.transaction.annotation.Transactional(readOnly=true)
public class DashboardService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final AppointmentRepository appointmentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final GroomingAppointmentRepository groomingRepo;
    private final PaymentRepository paymentRepository;

    public DashboardService(UserRepository userRepository, PetRepository petRepository,
                            AppointmentRepository appointmentRepository, OrderRepository orderRepository,
                            ProductRepository productRepository,
                            GroomingAppointmentRepository groomingRepo, PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.appointmentRepository = appointmentRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.groomingRepo = groomingRepo;
        this.paymentRepository = paymentRepository;
    }

    public DashboardDTO getAdminDashboard() {
        Map<String, Long> appointmentsByStatus = new LinkedHashMap<>();
        for (Appointment.AppointmentStatus status : Appointment.AppointmentStatus.values()) {
            appointmentsByStatus.put(status.name(), appointmentRepository.countByStatus(status));
        }

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (Order.OrderStatus status : Order.OrderStatus.values()) {
            ordersByStatus.put(status.name(), orderRepository.countByStatus(status));
        }

        return DashboardDTO.builder()
                .totalUsers(userRepository.count())
                .totalPets(petRepository.count())
                .totalAppointments(appointmentRepository.count())
                .totalOrders(orderRepository.count())
                .totalProducts(productRepository.count())
                .pendingAppointments(appointmentRepository.countByStatus(Appointment.AppointmentStatus.SCHEDULED))
                .completedAppointments(appointmentRepository.countByStatus(Appointment.AppointmentStatus.COMPLETED))
                .totalRevenue(orderRepository.calculateTotalRevenue())
                .appointmentsByStatus(appointmentsByStatus)
                .ordersByStatus(ordersByStatus)
                .build();
    }

    public Map<String, Object> getOwnerDashboard(Long userId) {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("totalPets", petRepository.countByOwnerIdAndIsActiveTrue(userId));
        dashboard.put("role", "PET_OWNER");
        return dashboard;
    }

    public Map<String, Object> getVetDashboard(Long vetId) {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("totalAppointments", appointmentRepository.countByVeterinarianId(vetId));
        dashboard.put("role", "VETERINARIAN");
        return dashboard;
    }

    public Map<String, Object> getGroomerDashboard(Long groomerId) {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("totalAppointments", groomingRepo.countByGroomerId(groomerId));
        dashboard.put("role", "GROOMER");
        return dashboard;
    }

    public Map<String, Object> getStoreDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("totalProducts", productRepository.count());
        dashboard.put("totalOrders", orderRepository.count());
        dashboard.put("totalRevenue", orderRepository.calculateTotalRevenue());
        dashboard.put("role", "STORE_MANAGER");
        return dashboard;
    }

    public Map<String, Object> getServiceDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("totalPayments", paymentRepository.count());
        dashboard.put("totalOrders", orderRepository.count());
        dashboard.put("totalRevenue", orderRepository.calculateTotalRevenue());
        dashboard.put("role", "SERVICE_STAFF");
        return dashboard;
    }

    public Map<String, Object> getManagementSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalUsers", userRepository.count());
        summary.put("totalPets", petRepository.count());
        summary.put("totalAppointments", appointmentRepository.count());
        summary.put("totalOrders", orderRepository.count());
        summary.put("totalProducts", productRepository.count());
        summary.put("totalPayments", paymentRepository.count());
        summary.put("totalRevenue", orderRepository.calculateTotalRevenue());
        summary.put("pendingAppointments", appointmentRepository.countByStatus(Appointment.AppointmentStatus.SCHEDULED));

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (Order.OrderStatus status : Order.OrderStatus.values()) {
            ordersByStatus.put(status.name(), orderRepository.countByStatus(status));
        }
        summary.put("ordersByStatus", ordersByStatus);

        Map<String, Long> appointmentsByStatus = new LinkedHashMap<>();
        for (Appointment.AppointmentStatus status : Appointment.AppointmentStatus.values()) {
            appointmentsByStatus.put(status.name(), appointmentRepository.countByStatus(status));
        }
        summary.put("appointmentsByStatus", appointmentsByStatus);

        return summary;
    }
}
