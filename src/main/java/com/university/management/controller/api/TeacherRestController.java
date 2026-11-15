package com.university.management.controller.api;

import com.university.management.dto.TeacherRequest;
import com.university.management.dto.TeacherResponse;
import com.university.management.service.TeacherService;
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
 * REST API for teachers. Admin and Teacher can reach it.
 * A teacher can only update/delete his/her own record (checked in TeacherService).
 */
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherRestController {

    private final TeacherService teacherService;

    @PostMapping
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody TeacherRequest request) {
        TeacherResponse body = TeacherResponse.from(teacherService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<TeacherResponse> getAll() {
        return teacherService.findAll().stream().map(TeacherResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TeacherResponse getOne(@PathVariable Long id) {
        return TeacherResponse.from(teacherService.findById(id));
    }

    @PutMapping("/{id}")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request,
                                  Authentication authentication) {
        return TeacherResponse.from(teacherService.update(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        teacherService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
