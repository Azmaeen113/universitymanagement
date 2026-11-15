package com.university.management.service;

import com.university.management.dto.StudentRequest;
import com.university.management.entity.Student;
import com.university.management.exception.BadRequestException;
import com.university.management.exception.DuplicateResourceException;
import com.university.management.exception.ResourceNotFoundException;
import com.university.management.repository.StudentRepository;
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
public class StudentService {

    private final StudentRepository studentRepository;
    private final EmailChecker emailChecker;
    private final PasswordEncoder passwordEncoder;

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public List<Student> findPending() {
        return studentRepository.findByApproved(false);
    }

    public Student findById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id " + id));
    }

    /** Used by the edit page: returns the student only if the current user may modify it. */
    public Student findForModify(Long id, Authentication currentUser) {
        Student student = findById(id);
        checkOwnership(student, currentUser);
        return student;
    }

    public Optional<Student> findByEmail(String email) {
        return studentRepository.findByEmailIgnoreCase(email);
    }

    public long count() {
        return studentRepository.count();
    }

    public long countPending() {
        return studentRepository.countByApproved(false);
    }

    /**
     * Approval rule: a student created by an Admin is approved at once,
     * a student created by a Teacher must wait for an Admin.
     */
    @Transactional
    public Student create(StudentRequest request, Authentication currentUser) {
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BadRequestException("Password is required");
        }
        String email = request.getEmail().trim().toLowerCase();
        emailChecker.ensureAvailable(email);
        checkRollNumberFree(request.getRollNumber().trim());

        Student student = new Student();
        student.setPassword(passwordEncoder.encode(request.getPassword()));
        student.setApproved(AuthUtil.isAdmin(currentUser));
        copyFields(student, request, email);
        return studentRepository.save(student);
    }

    @Transactional
    public Student update(Long id, StudentRequest request, Authentication currentUser) {
        Student student = findById(id);
        checkOwnership(student, currentUser);

        String email = request.getEmail().trim().toLowerCase();
        if (!email.equalsIgnoreCase(student.getEmail())) {
            emailChecker.ensureAvailable(email);
        }
        String rollNumber = request.getRollNumber().trim();
        if (!rollNumber.equals(student.getRollNumber())) {
            checkRollNumberFree(rollNumber);
        }

        copyFields(student, request, email);
        if (StringUtils.hasText(request.getPassword())) {
            student.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return studentRepository.save(student);
    }

    @Transactional
    public void delete(Long id, Authentication currentUser) {
        Student student = findById(id);
        checkOwnership(student, currentUser);
        studentRepository.delete(student);
    }

    /** Admin approves a pending student: approved = true, so the student can log in. */
    @Transactional
    public Student approve(Long id) {
        Student student = findById(id);
        student.setApproved(true);
        return studentRepository.save(student);
    }

    /** Admin rejects a pending student: the record is deleted. */
    @Transactional
    public void reject(Long id) {
        studentRepository.delete(findById(id));
    }

    /** Admin may change any student. A student may only change his/her own record. */
    private void checkOwnership(Student student, Authentication currentUser) {
        if (AuthUtil.isAdmin(currentUser)) {
            return;
        }
        if (!student.getEmail().equalsIgnoreCase(currentUser.getName())) {
            throw new AccessDeniedException("You can only modify your own profile");
        }
    }

    private void checkRollNumberFree(String rollNumber) {
        if (studentRepository.existsByRollNumber(rollNumber)) {
            throw new DuplicateResourceException("rollNumber", "Roll number is already in use: " + rollNumber);
        }
    }

    private void copyFields(Student student, StudentRequest request, String email) {
        student.setFullName(request.getFullName().trim());
        student.setEmail(email);
        student.setDepartment(request.getDepartment().trim());
        student.setSemester(request.getSemester());
        student.setRollNumber(request.getRollNumber().trim());
    }
}
