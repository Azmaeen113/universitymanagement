package com.university.management.unit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.university.management.entity.Teacher;
import com.university.management.repository.TeacherRepository;
import com.university.management.service.EmailChecker;
import com.university.management.service.TeacherService;
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

/** Unit test: ownership rule of TeacherService with mocked repository. */
@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private EmailChecker emailChecker;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private TeacherService teacherService;

    @Test
    void teacherCannotDeleteAnotherTeacherButAdminCan() {
        Teacher victim = new Teacher();
        victim.setEmail("victim@test.com");
        when(teacherRepository.findById(7L)).thenReturn(Optional.of(victim));

        Authentication otherTeacher = new TestingAuthenticationToken("other@test.com", "pw", "ROLE_TEACHER");
        assertThrows(AccessDeniedException.class, () -> teacherService.delete(7L, otherTeacher));
        verify(teacherRepository, never()).delete(any());

        Authentication admin = new TestingAuthenticationToken("admin@test.com", "pw", "ROLE_ADMIN");
        teacherService.delete(7L, admin);
        verify(teacherRepository).delete(victim);
    }
}
