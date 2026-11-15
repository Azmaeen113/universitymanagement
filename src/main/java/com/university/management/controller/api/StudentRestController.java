package com.university.management.controller.api;

import com.university.management.dto.StudentRequest;
import com.university.management.dto.StudentResponse;
import com.university.management.service.StudentService;
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

/**
 * REST API for students.
 *  - Admin: everything
 *  - Teacher: list/read, and register a new student (created as "pending")
 *  - Student: list/read, and update/delete own profile
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentRestController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request,
                                                  Authentication authentication) {
        StudentResponse body = StudentResponse.from(studentService.create(request, authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<StudentResponse> getAll() {
        return studentService.findAll().stream().map(StudentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public StudentResponse getOne(@PathVariable Long id) {
        return StudentResponse.from(studentService.findById(id));
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request,
                                  Authentication authentication) {
        return StudentResponse.from(studentService.update(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        studentService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }

    // Admin only (SecurityConfig). Sets approved = true.
    @PutMapping("/{id}/approve")
    public StudentResponse approve(@PathVariable Long id) {
        return StudentResponse.from(studentService.approve(id));
    }
}
