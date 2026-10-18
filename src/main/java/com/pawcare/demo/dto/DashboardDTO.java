package com.pawcare.demo.dto;

import lombok.*;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardDTO {
    private long totalUsers;
    private long totalPets;
    private long totalAppointments;
    private long totalOrders;
    private long totalProducts;
    private long pendingAppointments;
    private long completedAppointments;
    private double totalRevenue;
    private Map<String, Long> appointmentsByStatus;
    private Map<String, Long> ordersByStatus;
    private Map<String, Double> revenueByMonth;
}
