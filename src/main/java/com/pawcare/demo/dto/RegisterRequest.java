package com.pawcare.demo.dto;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequest {
    @NotBlank @Size(max=100) @Pattern(regexp="[\\p{L}\\p{M} .'-]+", message="Use letters, spaces, apostrophes or hyphens")
    private String firstName;
    @NotBlank @Size(max=100) @Pattern(regexp="[\\p{L}\\p{M} .'-]+", message="Use letters, spaces, apostrophes or hyphens")
    private String lastName;
    @NotBlank @Email @Size(max=150)
    private String email;
    @NotBlank @Size(min=8,max=72) @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])\\S+$", message="Password needs uppercase, lowercase, a number and a symbol, with no spaces")
    private String password;
    @NotBlank @Pattern(regexp="[0-9]{10}", message="Phone must contain exactly 10 digits")
    private String phone;
    @NotBlank @Size(min=5,max=500) private String address;
    @NotBlank @Size(max=100) private String city;
    @Size(max=100) private String state;
    @Pattern(regexp="(?:[A-Za-z0-9 -]{3,20})?",message="Enter a valid postal code") private String zipCode;
    @NotBlank @Pattern(regexp="PET_OWNER|VETERINARIAN|GROOMER|STORE_MANAGER|SERVICE_STAFF",message="Choose an available registration role")
    private String role;
    @Size(max=150) private String organization;
    @Size(max=100) private String licenseNumber;
    @Size(max=200) private String qualification;
    @Min(0) @Max(60) private Integer experienceYears;
    @Size(max=150) private String specialty;
    @Size(max=100) private String businessRegistration;
    @Size(max=100) private String employeeId;
    @Size(max=100) private String department;

    @AssertTrue(message="Veterinarians need a clinic, license number, qualification and years of experience")
    public boolean isVeterinarianDetailsValid() {
        return !"VETERINARIAN".equals(role) || (present(organization) && code(licenseNumber) && present(qualification) && experienceYears != null);
    }
    @AssertTrue(message="Groomers need a salon/business name, grooming specialty and years of experience")
    public boolean isGroomerDetailsValid() {
        return !"GROOMER".equals(role) || (present(organization) && present(specialty) && experienceYears != null);
    }
    @AssertTrue(message="Store managers need a store name and business registration number")
    public boolean isStoreDetailsValid() {
        return !"STORE_MANAGER".equals(role) || (present(organization) && code(businessRegistration));
    }
    @AssertTrue(message="Service staff need an organization, employee ID and department")
    public boolean isStaffDetailsValid() {
        return !"SERVICE_STAFF".equals(role) || (present(organization) && code(employeeId) && present(department));
    }
    private boolean present(String value) { return value != null && !value.isBlank(); }
    private boolean code(String value) { return present(value) && value.matches("[A-Za-z0-9][A-Za-z0-9 /-]{2,99}"); }
}
