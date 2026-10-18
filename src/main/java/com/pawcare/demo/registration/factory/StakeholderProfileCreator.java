package com.pawcare.demo.registration.factory;

import com.pawcare.demo.dto.RegisterRequest;
import com.pawcare.demo.entity.StakeholderProfile;
import com.pawcare.demo.entity.User;

/** Factory Method: each concrete creator defines its profile construction. */
public abstract class StakeholderProfileCreator {
    public abstract StakeholderProfile create(RegisterRequest request, User user);

    protected final StakeholderProfile newBaseProfile(RegisterRequest request, User user) {
        StakeholderProfile profile = new StakeholderProfile();
        profile.setUser(user);
        profile.setOrganization(request.getOrganization());
        return profile;
    }
}
