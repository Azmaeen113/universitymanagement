package com.university.management.security;

import com.university.management.entity.Admin;
import com.university.management.entity.Student;
import com.university.management.entity.Teacher;
import com.university.management.repository.AdminRepository;
import com.university.management.repository.StudentRepository;
import com.university.management.repository.TeacherRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security calls this class at login. We have three tables, so we look for the
 * email in admins, then teachers, then students and give the user the matching role.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<Admin> admin = adminRepository.findByEmailIgnoreCase(email);
        if (admin.isPresent()) {
            return buildUser(admin.get().getEmail(), admin.get().getPassword(), "ADMIN", true);
        }

        Optional<Teacher> teacher = teacherRepository.findByEmailIgnoreCase(email);
        if (teacher.isPresent()) {
            return buildUser(teacher.get().getEmail(), teacher.get().getPassword(), "TEACHER", true);
        }

        Optional<Student> student = studentRepository.findByEmailIgnoreCase(email);
        if (student.isPresent()) {
            // enabled = false for an unapproved student. Spring Security then throws
            // DisabledException and the login is refused (see LoginFailureHandler).
            return buildUser(student.get().getEmail(), student.get().getPassword(), "STUDENT",
                    student.get().isApproved());
        }

        throw new UsernameNotFoundException("No user found with email " + email);
    }

    // roles("ADMIN") becomes the authority "ROLE_ADMIN"
    private UserDetails buildUser(String email, String password, String role, boolean enabled) {
        return User.builder()
                .username(email)
                .password(password)
                .roles(role)
                .disabled(!enabled)
                .build();
    }
}
