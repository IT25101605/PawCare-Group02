package com.pawcare.demo.service;

import com.pawcare.demo.entity.Order;
import com.pawcare.demo.entity.Product;
import com.pawcare.demo.exception.ResourceNotFoundException;
import com.pawcare.demo.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

@Service
public class InventoryReservationService {
    private final ProductRepository products;

    public InventoryReservationService(ProductRepository products){this.products=products;}

    /**
     * Called while the order row is locked and before moving it to a terminal status.
     * The terminal-state guards and the same order lock make both refund routes idempotent.
     * Shipped goods need a physical return; a refund alone must not put them back on sale.
     */
    @Transactional(propagation=Propagation.MANDATORY)
    public void releaseUnshipped(Order order){
        if(order.getStatus()!=Order.OrderStatus.PENDING && order.getStatus()!=Order.OrderStatus.CONFIRMED
                && order.getStatus()!=Order.OrderStatus.PROCESSING)return;
        Map<Long,Integer> quantities=new TreeMap<>();
        order.getItems().forEach(item->quantities.merge(item.getProduct().getId(),item.getQuantity(),Integer::sum));
        quantities.forEach((productId,quantity)->{
            Product product=products.findForUpdate(productId)
                    .orElseThrow(()->new ResourceNotFoundException("Product","id",productId));
            product.setStockQuantity(Math.addExact(product.getStockQuantity(),quantity));
            products.save(product);
        });
    }
}
