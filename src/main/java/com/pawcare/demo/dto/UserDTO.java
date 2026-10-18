package com.pawcare.demo.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDTO {
    private Long id;
    @Size(min=1,max=100) @Pattern(regexp=".*\\S.*")
    private String firstName;
    @Size(min=1,max=100) @Pattern(regexp=".*\\S.*")
    private String lastName;
    private String email;
    @Pattern(regexp="[0-9]{10}", message="Phone must contain exactly 10 digits")
    private String phone;
    @Size(min=5,max=500)
    private String address;
    @Size(min=1,max=100)
    private String city;
    @Size(max=100)
    private String state;
    @Size(max=20)
    private String zipCode;
    private String profileImage;
    private Boolean enabled;
    private Boolean emailVerified;
    private String role;
    private LocalDateTime createdAt;
}
