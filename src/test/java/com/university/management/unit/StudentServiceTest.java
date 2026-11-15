package com.university.management.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.university.management.dto.StudentRequest;
import com.university.management.entity.Student;
import com.university.management.repository.StudentRepository;
import com.university.management.service.EmailChecker;
import com.university.management.service.StudentService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit test: StudentService alone, repositories and the password encoder are mocks (no Spring, no database). */
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private EmailChecker emailChecker;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private StudentService studentService;

    private StudentRequest request() {
        StudentRequest request = new StudentRequest();
        request.setFullName("  New Student ");
        request.setEmail(" New.Student@Test.com ");
        request.setPassword("secret123");
        request.setDepartment("CSE");
        request.setSemester(2);
        request.setRollNumber("ROLL-10");
        return request;
    }

    private void stubSave() {
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-password");
        when(studentRepository.existsByRollNumber("ROLL-10")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void studentCreatedByAdminIsApprovedAndPasswordIsHashed() {
        stubSave();
        Authentication admin = new TestingAuthenticationToken("admin@test.com", "pw", "ROLE_ADMIN");

        Student saved = studentService.create(request(), admin);

        assertThat(saved.isApproved()).isTrue();
        assertThat(saved.getPassword()).isEqualTo("hashed-password");
        assertThat(saved.getEmail()).isEqualTo("new.student@test.com");   // trimmed + lower case
        assertThat(saved.getFullName()).isEqualTo("New Student");
        verify(emailChecker).ensureAvailable("new.student@test.com");
    }

    @Test
    void studentCreatedByTeacherIsPendingApproval() {
        stubSave();
        Authentication teacher = new TestingAuthenticationToken("teacher@test.com", "pw", "ROLE_TEACHER");

        Student saved = studentService.create(request(), teacher);

        assertThat(saved.isApproved()).isFalse();
    }

    @Test
    void studentCannotUpdateAnotherStudentsProfile() {
        Student other = new Student();
        other.setEmail("other@test.com");
        when(studentRepository.findById(5L)).thenReturn(Optional.of(other));
        Authentication me = new TestingAuthenticationToken("me@test.com", "pw", "ROLE_STUDENT");

        assertThrows(AccessDeniedException.class, () -> studentService.update(5L, request(), me));

        verify(studentRepository, never()).save(any());
    }
}
