package com.pawcare.demo.service;

import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ShippingService shippingService;
    private final ProductImageService productImages;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       ProductRepository productRepository, UserRepository userRepository,
                       ShippingService shippingService, ProductImageService productImages) {
        this.cartRepository=cartRepository;this.cartItemRepository=cartItemRepository;
        this.productRepository=productRepository;this.userRepository=userRepository;this.shippingService=shippingService;
        this.productImages=productImages;
    }

    public Map<String,Object> getCart(Long userId){return cartToMap(getOrCreateCart(userId));}

    @Transactional
    public Map<String,Object> addItem(Long userId,Long productId,int quantity){
        if(quantity<1||quantity>1000000)throw new BadRequestException("Quantity must be between 1 and 1000000");
        Cart cart=getOrCreateCart(userId);
        Product product=productRepository.findById(productId).orElseThrow(()->new ResourceNotFoundException("Product","id",productId));
        if(!Boolean.TRUE.equals(product.getIsActive()))throw new BadRequestException("Product is unavailable");
        if(product.getStockQuantity()<quantity)throw new BadRequestException("Insufficient stock");
        var existing=cartItemRepository.findByCartIdAndProductId(cart.getId(),productId);
        if(existing.isPresent()){
            CartItem item=existing.get();if((long)item.getQuantity()+quantity>product.getStockQuantity())throw new BadRequestException("Total quantity exceeds stock");
            item.setQuantity(item.getQuantity()+quantity);cartItemRepository.save(item);
        }else{CartItem item=CartItem.builder().cart(cart).product(product).quantity(quantity).build();cartItemRepository.save(item);cart.getItems().add(item);}
        return getCart(userId);
    }

    @Transactional
    public Map<String,Object> updateItem(Long userId,Long itemId,int quantity){
        getOrCreateCart(userId);
        CartItem item=cartItemRepository.findById(itemId).orElseThrow(()->new ResourceNotFoundException("CartItem","id",itemId));
        if(!item.getCart().getUser().getId().equals(userId))throw new org.springframework.security.access.AccessDeniedException("This cart item belongs to another user");
        if(!Boolean.TRUE.equals(item.getProduct().getIsActive())) throw new BadRequestException("This product has been removed from the store");
        if(quantity<1||quantity>item.getProduct().getStockQuantity())throw new BadRequestException("Quantity must be positive and cannot exceed stock");
        item.setQuantity(quantity);cartItemRepository.save(item);return getCart(userId);
    }

    @Transactional
    public Map<String,Object> removeItem(Long userId,Long itemId){
        Cart cart=getOrCreateCart(userId);CartItem item=cart.getItems().stream().filter(i->i.getId().equals(itemId)).findFirst()
                .orElseThrow(()->new ResourceNotFoundException("CartItem","id",itemId));
        cart.getItems().remove(item);cartRepository.save(cart);return getCart(userId);
    }

    @Transactional public void clearCart(Long userId){Cart cart=getOrCreateCart(userId);cart.getItems().clear();cartRepository.save(cart);}

    private Cart getOrCreateCart(Long userId){
        // The same owner lock is used by checkout: cart changes and a double checkout
        // cannot race, including the first request before a cart row exists.
        User user=userRepository.findForUpdate(userId).orElseThrow(()->new ResourceNotFoundException("User","id",userId));
        return cartRepository.findByUserId(userId).orElseGet(()->{
            return cartRepository.save(Cart.builder().user(user).build());
        });
    }

    private Map<String,Object> cartToMap(Cart cart){
        Map<String,Object> result=new LinkedHashMap<>();result.put("id",cart.getId());result.put("userId",cart.getUser().getId());
        var items=cart.getItems().stream().map(ci->{
            Map<String,Object> item=new LinkedHashMap<>();item.put("id",ci.getId());item.put("productId",ci.getProduct().getId());
            item.put("productName",ci.getProduct().getName());item.put("price",ci.getProduct().getEffectivePrice());item.put("quantity",ci.getQuantity());
            item.put("image",productImages.resolve(ci.getProduct()));item.put("stockQuantity",ci.getProduct().getStockQuantity());item.put("isActive",ci.getProduct().getIsActive());
            item.put("subtotal",ci.getProduct().getEffectivePrice().multiply(java.math.BigDecimal.valueOf(ci.getQuantity())));return item;
        }).toList();
        result.put("items",items);result.put("itemCount",items.size());
        java.math.BigDecimal subtotal=items.stream().map(i->(java.math.BigDecimal)i.get("subtotal")).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);
        result.put("total",subtotal);
        java.math.BigDecimal shipping=subtotal.signum()==0?java.math.BigDecimal.ZERO:
                shippingService.calculateShipping(cart.getUser().getCity(),cart.getUser().getZipCode(),"CREDIT_CARD");
        result.putAll(OrderService.totals(subtotal,shipping));return result;
    }
}
