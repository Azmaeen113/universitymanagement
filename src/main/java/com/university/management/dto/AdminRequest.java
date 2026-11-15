package com.university.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Data sent by the client (JSON or HTML form) to create / update an Admin. */
@Getter
@Setter
@NoArgsConstructor
public class AdminRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;

    // Empty is allowed here (means "keep old password" on update).
    // The service rejects an empty password on create.
    @Pattern(regexp = "|.{6,}", message = "Password must be at least 6 characters")
    private String password;
}
