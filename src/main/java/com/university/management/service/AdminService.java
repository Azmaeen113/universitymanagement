package com.university.management.service;

import com.university.management.dto.AdminRequest;
import com.university.management.entity.Admin;
import com.university.management.exception.BadRequestException;
import com.university.management.exception.ResourceNotFoundException;
import com.university.management.repository.AdminRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final EmailChecker emailChecker;
    private final PasswordEncoder passwordEncoder;

    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    public Admin findById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id " + id));
    }

    public long count() {
        return adminRepository.count();
    }

    @Transactional
    public Admin create(AdminRequest request) {
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BadRequestException("Password is required");
        }
        String email = request.getEmail().trim().toLowerCase();
        emailChecker.ensureAvailable(email);

        Admin admin = new Admin();
        admin.setFullName(request.getFullName().trim());
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        return adminRepository.save(admin);
    }

    @Transactional
    public Admin update(Long id, AdminRequest request) {
        Admin admin = findById(id);

        String email = request.getEmail().trim().toLowerCase();
        // only check the other tables when the email really changed
        if (!email.equalsIgnoreCase(admin.getEmail())) {
            emailChecker.ensureAvailable(email);
        }

        admin.setFullName(request.getFullName().trim());
        admin.setEmail(email);
        if (StringUtils.hasText(request.getPassword())) {
            admin.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return adminRepository.save(admin);
    }

    @Transactional
    public void delete(Long id, Authentication currentUser) {
        Admin admin = findById(id);
        if (admin.getEmail().equalsIgnoreCase(currentUser.getName())) {
            throw new BadRequestException("You cannot delete your own account");
        }
        adminRepository.delete(admin);
    }
}
