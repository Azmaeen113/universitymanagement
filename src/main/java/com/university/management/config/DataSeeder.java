package com.university.management.config;

import com.university.management.entity.Admin;
import com.university.management.entity.Student;
import com.university.management.entity.Teacher;
import com.university.management.repository.AdminRepository;
import com.university.management.repository.StudentRepository;
import com.university.management.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once when the application starts and inserts test accounts.
 * It does nothing if the database already has admins, so restarting is safe.
 * (Not used in the "test" profile, the tests create their own data.)
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (adminRepository.count() > 0) {
            return;
        }

        // ---- Admins (password: admin123) ----
        addAdmin("System Admin", "admin@university.edu", "admin123");
        addAdmin("Rahim Uddin", "rahim.admin@university.edu", "admin123");

        // ---- Teachers (password: teacher123) ----
        addTeacher("Dr. Sarah Khan", "sarah.khan@university.edu", "Computer Science", "Professor");
        addTeacher("Mr. Imran Hossain", "imran.hossain@university.edu", "Computer Science", "Lecturer");
        addTeacher("Dr. Nusrat Jahan", "nusrat.jahan@university.edu", "Electrical Engineering", "Associate Professor");

        // ---- Students (password: student123) ----
        addStudent("Ayesha Rahman", "ayesha.rahman@student.edu", "Computer Science", 3, "CSE-2023-001", true);
        addStudent("Tanvir Ahmed", "tanvir.ahmed@student.edu", "Computer Science", 5, "CSE-2022-014", true);
        addStudent("Maliha Chowdhury", "maliha.chowdhury@student.edu", "Electrical Engineering", 2, "EEE-2024-007", true);
        // pending: registered by a teacher, cannot log in until the admin approves
        addStudent("Sakib Hasan", "sakib.hasan@student.edu", "Computer Science", 1, "CSE-2025-032", false);
        addStudent("Nadia Islam", "nadia.islam@student.edu", "Electrical Engineering", 1, "EEE-2025-011", false);
    }

    private void addAdmin(String fullName, String email, String password) {
        Admin admin = new Admin();
        admin.setFullName(fullName);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        adminRepository.save(admin);
    }

    private void addTeacher(String fullName, String email, String department, String designation) {
        Teacher teacher = new Teacher();
        teacher.setFullName(fullName);
        teacher.setEmail(email);
        teacher.setPassword(passwordEncoder.encode("teacher123"));
        teacher.setDepartment(department);
        teacher.setDesignation(designation);
        teacherRepository.save(teacher);
    }

    private void addStudent(String fullName, String email, String department, int semester,
                            String rollNumber, boolean approved) {
        Student student = new Student();
        student.setFullName(fullName);
        student.setEmail(email);
        student.setPassword(passwordEncoder.encode("student123"));
        student.setDepartment(department);
        student.setSemester(semester);
        student.setRollNumber(rollNumber);
        student.setApproved(approved);
        studentRepository.save(student);
    }
}
