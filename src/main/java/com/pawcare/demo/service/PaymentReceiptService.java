package com.pawcare.demo.service;

import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/** Generates a downloadable, authenticated PDF from the saved order and payment. */
@Service
@Transactional(readOnly = true)
public class PaymentReceiptService {
    private final OrderRepository orders;
    private final PaymentRepository payments;

    public PaymentReceiptService(OrderRepository orders, PaymentRepository payments) {
        this.orders = orders;
        this.payments = payments;
    }

    public byte[] generate(Long orderId) {
        Order order = orders.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        // A customer must not be able to download another customer's receipt.
        if (!SecurityService.hasRole("ADMIN") && !SecurityService.hasRole("STORE_MANAGER")
                && !SecurityService.hasRole("SERVICE_STAFF")) {
            SecurityService.requireOwner(order.getUser().getId());
        }
        Payment payment = payments.findByOrderId(orderId).stream()
                .max(Comparator.comparing(Payment::getId)).orElseThrow(() -> new BadRequestException("This order has no payment slip yet"));
        List<String> lines = new ArrayList<>();
        lines.add("PAWCARE - PAYMENT SLIP / INVOICE");
        lines.add("Generated: " + LocalDate.now());
        lines.add("");
        lines.add("Order: " + order.getOrderNumber());
        lines.add("Customer: " + order.getUser().getFullName());
        lines.add("Order date: " + order.getCreatedAt());
        lines.add("Payment method: " + payment.getPaymentMethod());
        lines.add("Payment status: " + payment.getPaymentStatus()
                + (payment.getPaymentMethod()==Payment.PaymentMethod.CASH ? " (pay at shop)" : ""));
        lines.add("Payment date: " + payment.getPaymentDate());
        if (payment.getTransactionId()!=null) lines.add("Reference: " + payment.getTransactionId());
        lines.add("");
        lines.add("ITEMS");
        for (OrderItem item : order.getItems()) {
            lines.add(safe(item.getProductName()) + " | Qty " + item.getQuantity()
                    + " | " + money(item.getTotalPrice()));
        }
        lines.add("");
        lines.add("Subtotal: " + money(order.getSubtotal()));
        lines.add("Tax: " + money(order.getTax()));
        lines.add("Shipping: " + money(order.getShippingCost()));
        lines.add("TOTAL: " + money(order.getTotalAmount()));
        lines.add("");
        lines.add(payment.getPaymentStatus()==Payment.PaymentStatus.COMPLETED
                ? "Payment confirmed - thank you for shopping with PawCare."
                : "Payment not yet confirmed. This is not proof of a completed payment.");
        return pdf(lines);
    }

    private static String money(BigDecimal amount){return "Rs. " + (amount==null?"0.00":amount.toPlainString());}
    private static String safe(String text){return text==null?"":text.replaceAll("[\\r\\n]+", " ");}
    private static String escape(String s){return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replaceAll("[^\\x20-\\x7E]", "?");}
    private static byte[] pdf(List<String> original) {
        List<String> wrapped = new ArrayList<>();
        for (String line : original) {
            String remaining = safe(line);
            while (remaining.length() > 88) {
                int at = remaining.lastIndexOf(' ', 88); if (at < 1) at = 88;
                wrapped.add(remaining.substring(0, at)); remaining=remaining.substring(at).stripLeading();
            }
            wrapped.add(remaining);
        }
        List<List<String>> pages = new ArrayList<>();
        for (int offset = 0; offset < wrapped.size(); offset += 48)
            pages.add(wrapped.subList(offset, Math.min(offset + 48, wrapped.size())));
        if (pages.isEmpty()) pages.add(List.of("PAWCARE RECEIPT"));
        int firstContent = 3 + pages.size(), fontRef = firstContent + pages.size();
        List<byte[]> objects = new ArrayList<>();
        objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder();
        for (int i=0;i<pages.size();i++)kids.append(3+i).append(" 0 R ");
        objects.add(bytes("<< /Type /Pages /Count " + pages.size() + " /Kids [" + kids + "] >>"));
        for (int i=0;i<pages.size();i++)objects.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 842] /Resources << /Font << /F1 " + fontRef + " 0 R >> >> /Contents " + (firstContent+i) + " 0 R >>"));
        for (List<String> page : pages) {
            StringBuilder s = new StringBuilder("BT\n/F1 10 Tf\n45 790 Td\n14 TL\n");
            for (String line : page)s.append('(').append(escape(line)).append(") Tj\nT*\n");
            s.append("ET\n");
            byte[] content=bytes(s.toString());
            ByteArrayOutputStream object=new ByteArrayOutputStream();
            try { object.write(bytes("<< /Length " + content.length + " >>\nstream\n")); object.write(content); object.write(bytes("endstream")); }
            catch(IOException e){throw new IllegalStateException(e);}
            objects.add(object.toByteArray());
        }
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try {
            out.write(bytes("%PDF-1.4\n"));
            List<Integer> offsets=new ArrayList<>();offsets.add(0);
            for(int i=0;i<objects.size();i++){offsets.add(out.size());out.write(bytes((i+1)+" 0 obj\n"));out.write(objects.get(i));out.write(bytes("\nendobj\n"));}
            int xref=out.size();out.write(bytes("xref\n0 "+(objects.size()+1)+"\n0000000000 65535 f \n"));
            for(int i=1;i<offsets.size();i++)out.write(bytes(String.format(Locale.ROOT,"%010d 00000 n \n",offsets.get(i))));
            out.write(bytes("trailer\n<< /Size "+(objects.size()+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF"));
        }catch(IOException e){throw new IllegalStateException(e);}
        return out.toByteArray();
    }
    private static byte[] bytes(String s){return s.getBytes(StandardCharsets.ISO_8859_1);}
}
