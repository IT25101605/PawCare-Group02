package com.pawcare.demo.registration.factory;

import com.pawcare.demo.dto.RegisterRequest;
import com.pawcare.demo.entity.StakeholderProfile;
import com.pawcare.demo.entity.User;

public final class VeterinarianProfileCreator extends StakeholderProfileCreator {
    @Override public StakeholderProfile create(RegisterRequest request, User user) {
        StakeholderProfile profile = newBaseProfile(request, user);
        profile.setLicenseNumber(request.getLicenseNumber());
        profile.setQualification(request.getQualification());
        profile.setExperienceYears(request.getExperienceYears());
        return profile;
    }
}
