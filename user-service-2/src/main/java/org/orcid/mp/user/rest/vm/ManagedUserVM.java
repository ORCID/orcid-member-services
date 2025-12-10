package org.orcid.mp.user.rest.vm;

import jakarta.validation.constraints.Size;
import org.orcid.mp.user.dto.UserDTO;

public class ManagedUserVM extends UserDTO {

    public static final int PASSWORD_MIN_LENGTH = 4;

    public static final int PASSWORD_MAX_LENGTH = 100;

    public ManagedUserVM() {
        // Empty constructor needed for Jackson.
    }

    @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "ManagedUserVM{" + "} " + super.toString();
    }
}
