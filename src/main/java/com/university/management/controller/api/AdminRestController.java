package com.university.management.controller.api;

import com.university.management.dto.AdminRequest;
import com.university.management.dto.AdminResponse;
import com.university.management.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST API for admins. Only ROLE_ADMIN can reach it (see SecurityConfig). */
@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
public class AdminRestController {

    private final AdminService adminService;

    @PostMapping
    public ResponseEntity<AdminResponse> create(@Valid @RequestBody AdminRequest request) {
        AdminResponse body = AdminResponse.from(adminService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body); // 201
    }

    @GetMapping
    public List<AdminResponse> getAll() {
        return adminService.findAll().stream().map(AdminResponse::from).toList(); // 200
    }

    @GetMapping("/{id}")
    public AdminResponse getOne(@PathVariable Long id) {
        return AdminResponse.from(adminService.findById(id));
    }

    @PutMapping("/{id}")
    public AdminResponse update(@PathVariable Long id, @Valid @RequestBody AdminRequest request) {
        return AdminResponse.from(adminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        adminService.delete(id, authentication);
        return ResponseEntity.noContent().build(); // 204
    }
}
