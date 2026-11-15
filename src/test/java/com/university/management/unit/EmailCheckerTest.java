package com.university.management.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.university.management.exception.DuplicateResourceException;
import com.university.management.repository.AdminRepository;
import com.university.management.repository.StudentRepository;
import com.university.management.repository.TeacherRepository;
import com.university.management.service.EmailChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit test: the cross-table email rule. The email is free in two tables but used in the third. */
@ExtendWith(MockitoExtension.class)
class EmailCheckerTest {

    @Mock
    private AdminRepository adminRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private StudentRepository studentRepository;
    @InjectMocks
    private EmailChecker emailChecker;

    @Test
    void emailUsedInStudentsTableIsRejected() {
        when(adminRepository.existsByEmailIgnoreCase("x@test.com")).thenReturn(false);
        when(teacherRepository.existsByEmailIgnoreCase("x@test.com")).thenReturn(false);
        when(studentRepository.existsByEmailIgnoreCase("x@test.com")).thenReturn(true);

        DuplicateResourceException error =
                assertThrows(DuplicateResourceException.class, () -> emailChecker.ensureAvailable("x@test.com"));

        assertThat(error.getField()).isEqualTo("email");
        assertThat(error.getMessage()).contains("x@test.com");
    }
}
