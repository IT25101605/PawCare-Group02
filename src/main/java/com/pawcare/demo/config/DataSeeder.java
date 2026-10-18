package com.pawcare.demo.config;

import com.pawcare.demo.entity.Role;
import com.pawcare.demo.entity.User;
import com.pawcare.demo.repository.RoleRepository;
import com.pawcare.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedRoles();
        seedUsers();
    }

    private void seedRoles() {
        String[] roles = {"ADMIN", "PET_OWNER", "VETERINARIAN", "GROOMER", "BOARDING_STAFF", "STORE_MANAGER", "SERVICE_STAFF"};
        String[] descriptions = {"System Administrator", "Pet Owner", "Veterinary Doctor",
                "Pet Groomer", "Boarding Facility Staff", "Pet Store Manager", "Payment & Service Staff"};

        for (int i = 0; i < roles.length; i++) {
            if (!roleRepository.existsByName(roles[i])) {
                Role role = Role.builder().name(roles[i]).description(descriptions[i]).build();
                roleRepository.save(role);
            }
        }
    }

    private void seedUsers() {
        String defaultPassword = passwordEncoder.encode("Admin@123");

        // Admin
        createOrUpdateUser("Admin", "User", "admin@pawcare.com", defaultPassword,
                "0710000100", "ADMIN");

        // Pet Owner
        createOrUpdateUser("John", "Smith", "john@pawcare.com", defaultPassword,
                "0710000101", "PET_OWNER");

        // Second Pet Owner
        createOrUpdateUser("Sarah", "Johnson", "sarah@pawcare.com", defaultPassword,
                "0710000102", "PET_OWNER");

        // Veterinarian
        createOrUpdateUser("Emily", "Brown", "emily@pawcare.com", defaultPassword,
                "0710000103", "VETERINARIAN");

        // Second Veterinarian
        createOrUpdateUser("Michael", "Davis", "michael@pawcare.com", defaultPassword,
                "0710000104", "VETERINARIAN");

        // Groomer
        createOrUpdateUser("Lisa", "Wilson", "lisa@pawcare.com", defaultPassword,
                "0710000105", "GROOMER");

        // Boarding Staff
        createOrUpdateUser("Tom", "Anderson", "tom@pawcare.com", defaultPassword,
                "0710000106", "BOARDING_STAFF");

        // Store Manager
        createOrUpdateUser("Karen", "Martinez", "karen@pawcare.com", defaultPassword,
                "0710000107", "STORE_MANAGER");

        // Service Staff
        createOrUpdateUser("David", "Clark", "david@pawcare.com", defaultPassword,
                "0710000108", "SERVICE_STAFF");
    }

    private void createOrUpdateUser(String firstName, String lastName, String email,
                                     String encodedPassword, String phone, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseGet(() ->
                roleRepository.save(Role.builder().name(roleName).description(roleName).build())
        );

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) { return; }
        {
            user = User.builder()
                    .firstName(firstName).lastName(lastName)
                    .email(email)
                    .password(encodedPassword)
                    .phone(phone).enabled(true).emailVerified(true)
                    .role(role).build();
            userRepository.save(user);
        }
    }
}
