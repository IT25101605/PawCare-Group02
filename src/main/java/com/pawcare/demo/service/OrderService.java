package com.pawcare.demo.service;

import com.pawcare.demo.dto.*;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.*;
import com.pawcare.demo.notification.strategy.NotificationDispatchService;
import com.pawcare.demo.notification.strategy.NotificationEvent;
import com.pawcare.demo.notification.strategy.NotificationScenario;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ShippingService shippingService;
    private final NotificationService notifications;
    private final NotificationDispatchService strategyNotifications;
    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final InventoryReservationService reservations;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository,
                        ProductRepository productRepository, ReviewRepository reviewRepository, UserRepository userRepository,
                        ShippingService shippingService, NotificationService notifications,
                        NotificationDispatchService strategyNotifications, PaymentRepository payments, InvoiceRepository invoices,
                        InventoryReservationService reservations) {
        this.orderRepository=orderRepository; this.cartRepository=cartRepository;
        this.productRepository=productRepository; this.reviewRepository=reviewRepository; this.userRepository=userRepository;
        this.shippingService=shippingService; this.notifications=notifications; this.strategyNotifications=strategyNotifications;
        this.payments=payments;this.invoices=invoices;
        this.reservations=reservations;
    }

    public Page<OrderDTO> getAll(Pageable pageable){return orderRepository.findAll(newest(pageable)).map(this::toDTO);}
    public Page<OrderDTO> getByUser(Long userId, Pageable pageable){return orderRepository.findByUserId(userId,newest(pageable)).map(this::toDTO);}
    private Pageable newest(Pageable p){return PageRequest.of(p.getPageNumber(),p.getPageSize(),Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")));}
    public OrderDTO getById(Long id){
        Order o=orderRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Order","id",id));
        if(SecurityService.hasRole("PET_OWNER")) SecurityService.requireOwner(o.getUser().getId());
        return toDTO(o);
    }

    @Transactional
    public OrderDTO createFromCart(Long userId,String shippingAddress){return createFromCart(userId,shippingAddress,null,null,null);}
    @Transactional
    public OrderDTO createFromCart(Long userId,String shippingAddress,String city,String postalCode,String paymentMethod){
        validateAddress(shippingAddress);
        User user=userRepository.findForUpdate(userId).orElseThrow(()->new ResourceNotFoundException("User","id",userId));
        Cart cart=cartRepository.findByUserId(userId).orElseThrow(()->new BadRequestException("Cart is empty"));
        if(cart.getItems().isEmpty())throw new BadRequestException("Cart is empty");
        OrderDTO result=createOrder(user,shippingAddress,city,postalCode,paymentMethod,cart.getItems());
        cart.getItems().clear();cartRepository.save(cart);return result;
    }

    @Transactional
    public OrderDTO buyNow(Long userId,String address,Long productId,Integer quantity){return buyNow(userId,address,null,null,null,productId,quantity);}
    @Transactional
    public OrderDTO buyNow(Long userId,String address,String city,String postalCode,String paymentMethod,Long productId,Integer quantity){
        validateAddress(address);
        if(quantity==null||quantity<1||quantity>1000000)throw new BadRequestException("Choose a valid quantity");
        User user=userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("User","id",userId));
        Product product=productRepository.findForUpdate(productId).orElseThrow(()->new ResourceNotFoundException("Product","id",productId));
        return createOrder(user,address,city,postalCode,paymentMethod,List.of(CartItem.builder().product(product).quantity(quantity).build()));
    }

    public Map<String,BigDecimal> quote(Long productId,int quantity,Long userId,String city,String postalCode,String paymentMethod){
        if(quantity<1||quantity>1000000)throw new BadRequestException("Choose a valid quantity");
        Product product=productRepository.findById(productId).orElseThrow(()->new ResourceNotFoundException("Product","id",productId));
        if(!Boolean.TRUE.equals(product.getIsActive())||product.getStockQuantity()<quantity)throw new BadRequestException("Product unavailable or insufficient stock");
        User user=userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("User","id",userId));
        String c=blank(city)?user.getCity():city;String z=blank(postalCode)?user.getZipCode():postalCode;
        return totals(product.getEffectivePrice().multiply(BigDecimal.valueOf(quantity)),shippingService.calculateShipping(c,z,paymentMethod));
    }
    public Map<String,BigDecimal> quote(Long productId,int quantity){
        Product product=productRepository.findById(productId).orElseThrow(()->new ResourceNotFoundException("Product","id",productId));
        return totals(product.getEffectivePrice().multiply(BigDecimal.valueOf(quantity)),BigDecimal.ZERO);
    }

    static Map<String,BigDecimal> totals(BigDecimal subtotal){return totals(subtotal,BigDecimal.ZERO);}
    static Map<String,BigDecimal> totals(BigDecimal subtotal,BigDecimal shipping){
        BigDecimal tax=subtotal.multiply(BigDecimal.valueOf(0.08)).setScale(2,RoundingMode.HALF_UP);
        BigDecimal safeShipping=shipping==null?BigDecimal.ZERO:shipping;
        return Map.of("subtotal",subtotal,"tax",tax,"shippingCost",safeShipping,"totalAmount",subtotal.add(tax).add(safeShipping));
    }

    @Transactional
    public OrderDTO recalculateForPayment(Long orderId,String paymentMethod){
        Order o=orderRepository.findById(orderId).orElseThrow(()->new ResourceNotFoundException("Order","id",orderId));
        BigDecimal shipping=shippingService.calculateShipping(o.getDeliveryCity(),o.getPostalCode(),paymentMethod);
        o.setCheckoutPaymentMethod(paymentMethod);o.setShippingCost(shipping);o.setTotalAmount(o.getSubtotal().add(o.getTax()).add(shipping));
        return toDTO(orderRepository.save(o));
    }

    private OrderDTO createOrder(User user,String shippingAddress,String city,String postalCode,String paymentMethod,List<CartItem> items){
        // Lock stock in a stable order before reading quantities. Concurrent buyers cannot
        // both reserve the final item, and multi-product carts do not acquire opposing locks.
        Map<Long,Product> lockedProducts=new HashMap<>();
        items.stream().map(ci->ci.getProduct().getId()).distinct().sorted().forEach(id->
                lockedProducts.put(id,productRepository.findForUpdate(id)
                        .orElseThrow(()->new ResourceNotFoundException("Product","id",id))));
        String deliveryCity=blank(city)?user.getCity():city.trim();
        String deliveryPostal=blank(postalCode)?user.getZipCode():postalCode.trim();
        String method=blank(paymentMethod)?"CREDIT_CARD":paymentMethod.trim().toUpperCase(Locale.ROOT);
        Order order=Order.builder().orderNumber("ORD-"+UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .user(user).shippingAddress(shippingAddress.trim()).deliveryCity(deliveryCity)
                .postalCode(deliveryPostal).checkoutPaymentMethod(method).status(Order.OrderStatus.PENDING).build();
        BigDecimal subtotal=BigDecimal.ZERO;
        for(CartItem ci:items){
            Product product=lockedProducts.get(ci.getProduct().getId());
            if(!Boolean.TRUE.equals(product.getIsActive())||ci.getQuantity()<1)throw new BadRequestException("Cart contains an unavailable product or invalid quantity");
            if(product.getStockQuantity()<ci.getQuantity())throw new BadRequestException("Insufficient stock for: "+product.getName());
            BigDecimal unitPrice=product.getEffectivePrice(),itemTotal=unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()));
            order.getItems().add(OrderItem.builder().order(order).product(product).productName(product.getName()).quantity(ci.getQuantity()).unitPrice(unitPrice).totalPrice(itemTotal).build());
            subtotal=subtotal.add(itemTotal);
            int newStock=product.getStockQuantity()-ci.getQuantity();product.setStockQuantity(newStock);productRepository.save(product);
            int threshold=product.getReorderLevel()==null?5:Math.max(product.getReorderLevel(),5);
            if(newStock<threshold){
                java.util.LinkedHashSet<User> recipients=new java.util.LinkedHashSet<>();
                recipients.addAll(userRepository.findByRoleName("STORE_MANAGER"));
                recipients.addAll(userRepository.findByRoleName("ADMIN"));
                for(User manager:recipients){
                    if(Boolean.TRUE.equals(manager.getEnabled()))notifications.create(manager.getId(),"Low stock warning",
                            "Stock for "+product.getName()+" is below "+threshold+" units ("+newStock+" remaining).",
                            Notification.NotificationType.INVENTORY,Notification.Priority.WARNING,
                            "STORE_MANAGER".equals(manager.getRole().getName())?"/pages/dashboard/store.html":"/pages/dashboard/admin.html",
                            product.getId(),"PRODUCT","low-stock-"+product.getId()+"-"+newStock+"-"+manager.getId());
                }
            }
        }
        BigDecimal tax=subtotal.multiply(BigDecimal.valueOf(0.08)).setScale(2,RoundingMode.HALF_UP);
        BigDecimal shipping=shippingService.calculateShipping(deliveryCity,deliveryPostal,method);
        order.setSubtotal(subtotal);order.setTax(tax);order.setShippingCost(shipping);order.setTotalAmount(subtotal.add(tax).add(shipping));
        order=orderRepository.save(order);
        notifications.create(user.getId(),"Order created","Your order "+order.getOrderNumber()+" has been created.",
                Notification.NotificationType.ORDER,Notification.Priority.INFO,"/pages/dashboard/owner.html#orders",
                order.getId(),"ORDER","order-created-"+order.getId());
        return toDTO(order);
    }

    @Transactional
    public OrderDTO updateStatus(Long id,String status){
        Order order=orderRepository.findForUpdate(id).orElseThrow(()->new ResourceNotFoundException("Order","id",id));
        Order.OrderStatus next;try{next=Order.OrderStatus.valueOf(status);}catch(Exception ex){throw new BadRequestException("Invalid order status");}
        Order.OrderStatus previous=order.getStatus();
        if(previous==next)return toDTO(order);
        if(previous==Order.OrderStatus.CANCELLED || previous==Order.OrderStatus.REFUNDED)
            throw new BadRequestException("Cancelled or refunded orders cannot be reopened");
        var orderPayments=payments.findByOrderId(id);
        boolean paid=orderPayments.stream().anyMatch(payment->payment.getPaymentStatus()==Payment.PaymentStatus.COMPLETED);
        if(next==Order.OrderStatus.CANCELLED && paid)
            throw new BadRequestException("A paid order must be refunded instead of cancelled");
        if(next==Order.OrderStatus.REFUNDED && !paid)
            throw new BadRequestException("There is no completed payment to refund; cancel an unpaid order instead");
        if(next==Order.OrderStatus.CANCELLED){
            if(previous==Order.OrderStatus.SHIPPED || previous==Order.OrderStatus.OUT_FOR_DELIVERY || previous==Order.OrderStatus.DELIVERED)
                throw new BadRequestException("An order that has shipped cannot be cancelled; use the refund process");
        } else if(next!=Order.OrderStatus.REFUNDED && next.ordinal()<previous.ordinal()) {
            throw new BadRequestException("An order cannot move back to an earlier delivery stage");
        }
        if(next==Order.OrderStatus.CANCELLED || next==Order.OrderStatus.REFUNDED){
            reservations.releaseUnshipped(order);
            for(Payment payment:orderPayments){
                if(next==Order.OrderStatus.REFUNDED && payment.getPaymentStatus()==Payment.PaymentStatus.COMPLETED)
                    payment.setPaymentStatus(Payment.PaymentStatus.REFUNDED);
                else if(payment.getPaymentStatus()==Payment.PaymentStatus.PENDING)
                    payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
            }
            payments.saveAll(orderPayments);
            var orderInvoices=invoices.findByOrderId(id);
            orderInvoices.forEach(invoice->invoice.setStatus(Invoice.InvoiceStatus.CANCELLED));
            invoices.saveAll(orderInvoices);
        }
        order.setStatus(next);order=orderRepository.save(order);
        strategyNotifications.dispatch(NotificationScenario.ORDER_STATUS, order.getUser().getId(),
                new NotificationEvent(order.getUser().getFullName(), order.getOrderNumber(), next.name(), ""),
                "/pages/dashboard/owner.html#orders", order.getId(), "ORDER",
                "order-status-" + order.getId() + "-" + next);
        return toDTO(order);
    }

    private void validateAddress(String address){if(address==null||address.strip().length()<5||address.length()>500)throw new BadRequestException("Shipping address must be 5–500 characters");}
    private static boolean blank(String s){return s==null||s.isBlank();}

    private OrderDTO toDTO(Order o){
        List<OrderItemDTO> items=o.getItems().stream().map(i->toItemDTO(o,i)).collect(Collectors.toList());
        return OrderDTO.builder().id(o.getId()).createdAt(o.getCreatedAt()).orderNumber(o.getOrderNumber()).userId(o.getUser().getId())
                .userName(o.getUser().getFullName()).subtotal(o.getSubtotal()).tax(o.getTax()).shippingCost(o.getShippingCost())
                .totalAmount(o.getTotalAmount()).status(o.getStatus().name()).shippingAddress(o.getShippingAddress())
                .deliveryCity(o.getDeliveryCity()).postalCode(o.getPostalCode()).checkoutPaymentMethod(o.getCheckoutPaymentMethod())
                .notes(o.getNotes()).items(items).build();
    }
    private OrderItemDTO toItemDTO(Order order, OrderItem item) {
        Review review = reviewRepository.findByUserIdAndProductIdAndOrderIdAndReviewType(
                order.getUser().getId(), item.getProduct().getId(), order.getId(), Review.ReviewType.PRODUCT).orElse(null);
        String reviewStatus = null;
        if (review != null) {
            Review.ModerationStatus status = review.getModerationStatus();
            if (status == null) status = Boolean.TRUE.equals(review.getIsApproved()) ? Review.ModerationStatus.APPROVED : Review.ModerationStatus.PENDING;
            reviewStatus = status.name();
        }
        return OrderItemDTO.builder().id(item.getId()).productId(item.getProduct().getId())
                .productName(item.getProductName()).quantity(item.getQuantity()).unitPrice(item.getUnitPrice()).totalPrice(item.getTotalPrice())
                .reviewId(review == null ? null : review.getId()).reviewRating(review == null ? null : review.getRating()).reviewStatus(reviewStatus).build();
    }

}
