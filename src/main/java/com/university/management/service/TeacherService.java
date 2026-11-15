package com.university.management.service;

import com.university.management.dto.TeacherRequest;
import com.university.management.entity.Teacher;
import com.university.management.exception.BadRequestException;
import com.university.management.exception.ResourceNotFoundException;
import com.university.management.repository.TeacherRepository;
import com.university.management.security.AuthUtil;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final EmailChecker emailChecker;
    private final PasswordEncoder passwordEncoder;

    public List<Teacher> findAll() {
        return teacherRepository.findAll();
    }

    public Teacher findById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id " + id));
    }

    /** Used by the edit page: returns the teacher only if the current user may modify it. */
    public Teacher findForModify(Long id, Authentication currentUser) {
        Teacher teacher = findById(id);
        checkOwnership(teacher, currentUser);
        return teacher;
    }

    public Optional<Teacher> findByEmail(String email) {
        return teacherRepository.findByEmailIgnoreCase(email);
    }

    public long count() {
        return teacherRepository.count();
    }

    @Transactional
    public Teacher create(TeacherRequest request) {
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BadRequestException("Password is required");
        }
        String email = request.getEmail().trim().toLowerCase();
        emailChecker.ensureAvailable(email);

        Teacher teacher = new Teacher();
        teacher.setPassword(passwordEncoder.encode(request.getPassword()));
        copyFields(teacher, request, email);
        return teacherRepository.save(teacher);
    }

    @Transactional
    public Teacher update(Long id, TeacherRequest request, Authentication currentUser) {
        Teacher teacher = findById(id);
        checkOwnership(teacher, currentUser);

        String email = request.getEmail().trim().toLowerCase();
        if (!email.equalsIgnoreCase(teacher.getEmail())) {
            emailChecker.ensureAvailable(email);
        }

        copyFields(teacher, request, email);
        if (StringUtils.hasText(request.getPassword())) {
            teacher.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return teacherRepository.save(teacher);
    }

    @Transactional
    public void delete(Long id, Authentication currentUser) {
        Teacher teacher = findById(id);
        checkOwnership(teacher, currentUser);
        teacherRepository.delete(teacher);
    }

    /** Admin may change any teacher. A teacher may only change his/her own record. */
    private void checkOwnership(Teacher teacher, Authentication currentUser) {
        if (AuthUtil.isAdmin(currentUser)) {
            return;
        }
        if (!teacher.getEmail().equalsIgnoreCase(currentUser.getName())) {
            throw new AccessDeniedException("You can only modify your own profile");
        }
    }

    private void copyFields(Teacher teacher, TeacherRequest request, String email) {
        teacher.setFullName(request.getFullName().trim());
        teacher.setEmail(email);
        teacher.setDepartment(request.getDepartment().trim());
        teacher.setDesignation(request.getDesignation().trim());
    }
}
