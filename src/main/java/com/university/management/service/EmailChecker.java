package com.university.management.service;

import com.university.management.exception.DuplicateResourceException;
import com.university.management.repository.AdminRepository;
import com.university.management.repository.StudentRepository;
import com.university.management.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Business rule: one email can exist only once across admins, teachers AND students.
 * All three services call this class so the rule is written in one place.
 */
@Component
@RequiredArgsConstructor
public class EmailChecker {

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    public void ensureAvailable(String email) {
        boolean used = adminRepository.existsByEmailIgnoreCase(email)
                || teacherRepository.existsByEmailIgnoreCase(email)
                || studentRepository.existsByEmailIgnoreCase(email);
        if (used) {
            throw new DuplicateResourceException("email", "Email is already in use: " + email);
        }
    }
}
