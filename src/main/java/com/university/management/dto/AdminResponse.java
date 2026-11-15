package com.university.management.dto;

import com.university.management.entity.Admin;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Data returned to the client. No password field on purpose. */
@Getter
@AllArgsConstructor
public class AdminResponse {

    private Long id;
    private String fullName;
    private String email;

    public static AdminResponse from(Admin admin) {
        return new AdminResponse(admin.getId(), admin.getFullName(), admin.getEmail());
    }
}
