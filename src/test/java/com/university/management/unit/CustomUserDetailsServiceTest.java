package com.university.management.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.university.management.entity.Student;
import com.university.management.repository.AdminRepository;
import com.university.management.repository.StudentRepository;
import com.university.management.repository.TeacherRepository;
import com.university.management.security.CustomUserDetailsService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

/** Unit test: the login lookup goes through the three tables and disables unapproved students. */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private AdminRepository adminRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private StudentRepository studentRepository;
    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void unapprovedStudentIsFoundWithStudentRoleButDisabled() {
        Student pending = new Student();
        pending.setEmail("pending@test.com");
        pending.setPassword("hash");
        pending.setApproved(false);

        when(adminRepository.findByEmailIgnoreCase("pending@test.com")).thenReturn(Optional.empty());
        when(teacherRepository.findByEmailIgnoreCase("pending@test.com")).thenReturn(Optional.empty());
        when(studentRepository.findByEmailIgnoreCase("pending@test.com")).thenReturn(Optional.of(pending));

        UserDetails user = userDetailsService.loadUserByUsername("pending@test.com");

        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_STUDENT");
        assertThat(user.isEnabled()).isFalse();   // Spring Security turns this into DisabledException at login
    }
}
