package com.pawcare.demo.pet.builder;

import com.pawcare.demo.dto.PetDTO;
import com.pawcare.demo.entity.Pet;
import com.pawcare.demo.entity.Species;
import com.pawcare.demo.entity.User;

/** Builder for registration-specific defaults and optional profile information. */
public final class PetRegistrationBuilder {
    private final Pet.PetBuilder builder = Pet.builder().isActive(true);

    public PetRegistrationBuilder from(PetDTO dto) {
        builder.name(dto.getName()).dateOfBirth(dto.getDateOfBirth())
            .color(dto.getColor()).weight(dto.getWeight())
            .microchipId(dto.getMicrochipId()).notes(dto.getNotes())
            .photo(dto.getPhoto());
        if (dto.getGender() != null) builder.gender(Pet.Gender.valueOf(dto.getGender()));
        return this;
    }
    public PetRegistrationBuilder species(Species species) { builder.species(species); return this; }
    public PetRegistrationBuilder owner(User owner) { builder.owner(owner); return this; }
    public Pet build() { return builder.build(); }
}
