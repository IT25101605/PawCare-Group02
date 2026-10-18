package com.pawcare.demo.service;

import com.pawcare.demo.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("securityService")
public class SecurityService {
    public static boolean hasRole(String role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
    public static void requireOwner(Long ownerId) {
        if (!hasRole("ADMIN") && !new SecurityService().isCurrentUser(ownerId))
            throw new org.springframework.security.access.AccessDeniedException("This record belongs to another owner");
    }

    public boolean isCurrentUser(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) return false;
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        return user.getId().equals(userId);
    }
}
