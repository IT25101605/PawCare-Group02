package com.pawcare.demo.dto;
import lombok.*;
import java.math.BigDecimal;
import java.util.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StoreAnalyticsDTO {
 private long totalOrders,completedOrders,pendingOrders,cancelledOrders;
 private BigDecimal totalRevenue;
 private List<Metric> monthlyRevenue,topProducts;
 private List<StockItem> lowStock,outOfStock;
 private Map<String,Long> orderStatusDistribution;
 @Data @NoArgsConstructor @AllArgsConstructor public static class Metric { private String label; private BigDecimal value; }
 @Data @NoArgsConstructor @AllArgsConstructor public static class StockItem { private Long productId; private String name; private Integer stock; private Integer reorderLevel; }
}
