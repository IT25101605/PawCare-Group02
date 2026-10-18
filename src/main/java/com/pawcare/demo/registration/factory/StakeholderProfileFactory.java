package com.pawcare.demo.registration.factory;

import com.pawcare.demo.dto.RegisterRequest;
import com.pawcare.demo.entity.StakeholderProfile;
import com.pawcare.demo.entity.User;
import com.pawcare.demo.exception.BadRequestException;
import java.util.Map;

/** Selects a concrete creator; unsupported roles are not publicly registerable. */
public final class StakeholderProfileFactory {
    private static final Map<String, StakeholderProfileCreator> CREATORS = Map.of(
        "VETERINARIAN", new VeterinarianProfileCreator(),
        "GROOMER", new GroomerProfileCreator(),
        "STORE_MANAGER", new StoreManagerProfileCreator(),
        "SERVICE_STAFF", new ServiceStaffProfileCreator()
    );
    private StakeholderProfileFactory() { }

    public static StakeholderProfile create(String role, RegisterRequest request, User user) {
        StakeholderProfileCreator creator = CREATORS.get(role);
        if (creator == null) throw new BadRequestException("This role cannot register publicly");
        return creator.create(request, user);
    }
}
