package com.pawcare.demo.service;

import com.pawcare.demo.dto.PaymentDTO;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional(readOnly=true)
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final ShippingService shippingService;
    private final NotificationService notifications;
    private final InventoryReservationService reservations;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository,
                          UserRepository userRepository, InvoiceRepository invoiceRepository,
                          ShippingService shippingService, NotificationService notifications, InventoryReservationService reservations) {
        this.paymentRepository=paymentRepository;this.orderRepository=orderRepository;this.userRepository=userRepository;
        this.invoiceRepository=invoiceRepository;this.shippingService=shippingService;this.notifications=notifications;
        this.reservations=reservations;
    }

    public Page<PaymentDTO> getAll(Pageable pageable){return paymentRepository.findAll(newest(pageable)).map(this::toDTO);}
    public Page<PaymentDTO> getByUser(Long userId,Pageable pageable){return paymentRepository.findByUserId(userId,newest(pageable)).map(this::toDTO);}
    private Pageable newest(Pageable p){return PageRequest.of(p.getPageNumber(),p.getPageSize(),Sort.by(Sort.Order.desc("paymentDate"),Sort.Order.desc("id")));}

    @Transactional
    public PaymentDTO processPayment(Long orderId,Long userId,String paymentMethod,String cardNumber,Integer expiryMonth,Integer expiryYear,String cvc){
        // Serialize payment submissions before checking status, including double taps on mobile.
        Order order=orderRepository.findForUpdate(orderId).orElseThrow(()->new ResourceNotFoundException("Order","id",orderId));
        User user=userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("User","id",userId));
        if(!SecurityService.hasRole("SERVICE_STAFF"))SecurityService.requireOwner(order.getUser().getId());
        if(order.getStatus()!=Order.OrderStatus.PENDING)throw new BadRequestException("This order has already been processed or cancelled");
        user=order.getUser();
        Payment.PaymentMethod method;try{method=Payment.PaymentMethod.valueOf(paymentMethod);}catch(Exception ex){throw new BadRequestException("Invalid payment method");}
        if(method==Payment.PaymentMethod.CREDIT_CARD||method==Payment.PaymentMethod.DEBIT_CARD)validateCard(cardNumber,expiryMonth,expiryYear,cvc);

        // Authoritative server-side shipping recalculation. Any client-supplied shipping is ignored.
        java.math.BigDecimal shipping=shippingService.calculateShipping(order.getDeliveryCity(),order.getPostalCode(),method.name());
        order.setCheckoutPaymentMethod(method.name());order.setShippingCost(shipping);
        order.setTotalAmount(order.getSubtotal().add(order.getTax()).add(shipping));

        boolean cash=method==Payment.PaymentMethod.CASH;
        String transactionId=(cash?"CASH-":"TXN-")+UUID.randomUUID().toString().substring(0,12).toUpperCase();
        Payment payment=Payment.builder().order(order).user(user).amount(order.getTotalAmount()).paymentMethod(method)
                .paymentStatus(cash?Payment.PaymentStatus.PENDING:Payment.PaymentStatus.COMPLETED)
                .transactionId(transactionId)
                .description(cash?"Pay at shop for order "+order.getOrderNumber():"Payment for order "+order.getOrderNumber()).build();
        payment=paymentRepository.save(payment);

        order.setStatus(Order.OrderStatus.CONFIRMED);orderRepository.save(order);
        invoiceRepository.save(Invoice.builder().invoiceNumber("INV-"+UUID.randomUUID().toString().substring(0,12).toUpperCase())
                .order(order).user(user).amount(order.getSubtotal()).tax(order.getTax()).total(order.getTotalAmount())
                .status(cash?Invoice.InvoiceStatus.DRAFT:Invoice.InvoiceStatus.PAID).paidDate(cash?null:java.time.LocalDate.now())
                .notes(cash?"In-Store Cash Payment / Pay at Shop":"Online payment invoice; shipping "+shipping).build());

        notifications.create(user.getId(),cash?"Order confirmed":"Payment successful",
                cash?"Your order "+order.getOrderNumber()+" is confirmed for pay-at-shop.":"Payment for "+order.getOrderNumber()+" was successful. Shipping: Rs. "+shipping.toPlainString()+".",
                cash?Notification.NotificationType.ORDER:Notification.NotificationType.PAYMENT,Notification.Priority.SUCCESS,
                "/pages/dashboard/owner.html#orders",order.getId(),"ORDER",
                "payment-"+order.getId()+"-"+method.name());

        return toDTO(payment);
    }

    private void validateCard(String number,Integer month,Integer year,String cvc){
        String digits=number==null?"":number.replaceAll("[\\s-]","");
        if(!digits.matches("[0-9]{16}"))throw new BadRequestException("Card number must contain exactly 16 digits");
        if(month==null||month<1||month>12)throw new BadRequestException("Expiry month must be between 01 and 12");
        if(year==null)throw new BadRequestException("Expiry year is required");
        java.time.YearMonth expiry;try{expiry=java.time.YearMonth.of(year,month);}catch(java.time.DateTimeException ex){throw new BadRequestException("Invalid card expiry date");}
        if(expiry.isBefore(java.time.YearMonth.now()))throw new BadRequestException("Card has expired");
        if(cvc==null||!cvc.matches("[0-9]{3}"))throw new BadRequestException("Invalid CVC. CVC must contain exactly 3 digits.");
    }

    @Transactional
    public PaymentDTO updatePaymentStatus(Long paymentId,String status){
        // Acquire the order first, matching checkout/cancellation lock order.
        Long orderId=paymentRepository.findOrderIdByPaymentId(paymentId).orElse(null);
        Order order=orderId==null?null:orderRepository.findForUpdate(orderId).orElseThrow();
        Payment payment=paymentRepository.findForUpdate(paymentId).orElseThrow(()->new ResourceNotFoundException("Payment","id",paymentId));
        Payment.PaymentStatus next;try{next=Payment.PaymentStatus.valueOf(status);}catch(Exception ex){throw new BadRequestException("Invalid payment status");}
        Payment.PaymentStatus previous=payment.getPaymentStatus();
        if(previous==next)return toDTO(payment);
        if(previous==Payment.PaymentStatus.FAILED || previous==Payment.PaymentStatus.REFUNDED ||
                (previous==Payment.PaymentStatus.COMPLETED && next!=Payment.PaymentStatus.REFUNDED) ||
                (previous==Payment.PaymentStatus.PENDING && next==Payment.PaymentStatus.REFUNDED))
            throw new BadRequestException("This payment cannot move to the selected status");
        if(order!=null && (order.getStatus()==Order.OrderStatus.CANCELLED || order.getStatus()==Order.OrderStatus.REFUNDED))
            throw new BadRequestException("A cancelled or refunded order cannot accept another payment");
        payment.setPaymentStatus(next);
        payment=paymentRepository.save(payment);
        if(order!=null){
            if(next==Payment.PaymentStatus.REFUNDED){
                reservations.releaseUnshipped(order);order.setStatus(Order.OrderStatus.REFUNDED);
            }
            if(next==Payment.PaymentStatus.FAILED && order.getStatus()==Order.OrderStatus.CONFIRMED)
                order.setStatus(Order.OrderStatus.PENDING);
            orderRepository.save(order);
            var orderInvoices=invoiceRepository.findByOrderId(order.getId());
            for(Invoice invoice:orderInvoices){
                if(next==Payment.PaymentStatus.COMPLETED && invoice.getStatus()!=Invoice.InvoiceStatus.CANCELLED){
                    invoice.setStatus(Invoice.InvoiceStatus.PAID);invoice.setPaidDate(java.time.LocalDate.now());
                }else if(next==Payment.PaymentStatus.FAILED || next==Payment.PaymentStatus.REFUNDED)
                    invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
            }
            invoiceRepository.saveAll(orderInvoices);
        }
        notifications.create(payment.getUser().getId(),"Payment updated","Payment status is now "+payment.getPaymentStatus()+".",
                Notification.NotificationType.PAYMENT,Notification.Priority.INFO,"/pages/dashboard/owner.html#orders",
                payment.getId(),"PAYMENT","payment-status-"+payment.getId()+"-"+payment.getPaymentStatus());
        return toDTO(payment);
    }

    private PaymentDTO toDTO(Payment p){
        return PaymentDTO.builder().id(p.getId()).orderId(p.getOrder()!=null?p.getOrder().getId():null)
                .orderNumber(p.getOrder()!=null?p.getOrder().getOrderNumber():null).userId(p.getUser().getId()).amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod().name()).paymentStatus(p.getPaymentStatus().name())
                .transactionId(p.getTransactionId()).description(p.getDescription()).build();
    }
}
