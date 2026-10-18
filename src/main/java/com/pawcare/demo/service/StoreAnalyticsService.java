package com.pawcare.demo.service;
import com.pawcare.demo.dto.StoreAnalyticsDTO;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
@Service @Transactional(readOnly=true)
public class StoreAnalyticsService {
 private final OrderRepository orders; private final ProductRepository products;
 public StoreAnalyticsService(OrderRepository orders,ProductRepository products){this.orders=orders;this.products=products;}
 public StoreAnalyticsDTO get(){List<Order> all=orders.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("createdAt"),org.springframework.data.domain.Sort.Order.desc("id")));Map<String,Long> status=new LinkedHashMap<>();Map<String,BigDecimal> monthly=new TreeMap<>();Map<String,BigDecimal> sold=new HashMap<>();BigDecimal revenue=BigDecimal.ZERO;long completed=0,pending=0,cancelled=0;for(Order o:all){status.merge(o.getStatus().name(),1L,Long::sum);if(o.getStatus()==Order.OrderStatus.DELIVERED)completed++;if(o.getStatus()==Order.OrderStatus.PENDING)pending++;if(o.getStatus()==Order.OrderStatus.CANCELLED||o.getStatus()==Order.OrderStatus.REFUNDED)cancelled++;if(o.getStatus()!=Order.OrderStatus.CANCELLED&&o.getStatus()!=Order.OrderStatus.REFUNDED){revenue=revenue.add(o.getTotalAmount());if(o.getCreatedAt()!=null)monthly.merge(o.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),o.getTotalAmount(),BigDecimal::add);for(OrderItem i:o.getItems())sold.merge(i.getProductName(),BigDecimal.valueOf(i.getQuantity()),BigDecimal::add);}}
 List<StoreAnalyticsDTO.Metric> months=monthly.entrySet().stream().map(e->new StoreAnalyticsDTO.Metric(e.getKey(),e.getValue())).toList();List<StoreAnalyticsDTO.Metric> top=sold.entrySet().stream().sorted(Map.Entry.<String,BigDecimal>comparingByValue().reversed()).limit(10).map(e->new StoreAnalyticsDTO.Metric(e.getKey(),e.getValue())).toList();List<Product> ps=products.findAll();List<StoreAnalyticsDTO.StockItem> low=ps.stream().filter(p->p.getStockQuantity()!=null&&p.getReorderLevel()!=null&&p.getStockQuantity()<=p.getReorderLevel()&&p.getStockQuantity()>0).sorted(Comparator.comparing(Product::getStockQuantity)).map(p->new StoreAnalyticsDTO.StockItem(p.getId(),p.getName(),p.getStockQuantity(),p.getReorderLevel())).toList();List<StoreAnalyticsDTO.StockItem> out=ps.stream().filter(p->p.getStockQuantity()!=null&&p.getStockQuantity()==0).map(p->new StoreAnalyticsDTO.StockItem(p.getId(),p.getName(),p.getStockQuantity(),p.getReorderLevel())).toList();return StoreAnalyticsDTO.builder().totalOrders(all.size()).completedOrders(completed).pendingOrders(pending).cancelledOrders(cancelled).totalRevenue(revenue).monthlyRevenue(months).topProducts(top).lowStock(low).outOfStock(out).orderStatusDistribution(status).build();}
}
