package com.university.management;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.university.management.entity.Admin;
import com.university.management.entity.Student;
import com.university.management.entity.Teacher;
import com.university.management.repository.AdminRepository;
import com.university.management.repository.StudentRepository;
import com.university.management.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Starts the whole application with an in-memory H2 database and puts the same small
 * set of users into the database before every test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected static final String PASSWORD = "password123";

    protected static final String ADMIN_EMAIL = "admin@test.com";
    protected static final String TEACHER1_EMAIL = "teacher1@test.com";
    protected static final String TEACHER2_EMAIL = "teacher2@test.com";
    protected static final String STUDENT1_EMAIL = "student1@test.com";
    protected static final String STUDENT2_EMAIL = "student2@test.com";
    protected static final String PENDING_EMAIL = "pending@test.com";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected AdminRepository adminRepository;
    @Autowired protected TeacherRepository teacherRepository;
    @Autowired protected StudentRepository studentRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    protected Admin admin;
    protected Teacher teacher1;
    protected Teacher teacher2;
    protected Student student1;
    protected Student student2;
    protected Student pendingStudent;

    @BeforeEach
    void createTestData() {
        studentRepository.deleteAll();
        teacherRepository.deleteAll();
        adminRepository.deleteAll();

        admin = saveAdmin("Test Admin", ADMIN_EMAIL);
        teacher1 = saveTeacher("Teacher One", TEACHER1_EMAIL);
        teacher2 = saveTeacher("Teacher Two", TEACHER2_EMAIL);
        student1 = saveStudent("Student One", STUDENT1_EMAIL, "ROLL-1", true);
        student2 = saveStudent("Student Two", STUDENT2_EMAIL, "ROLL-2", true);
        pendingStudent = saveStudent("Pending Student", PENDING_EMAIL, "ROLL-3", false);
    }

    // ---- "logged in as" helpers (no real login needed for the permission tests) ----

    protected RequestPostProcessor asAdmin() {
        return user(ADMIN_EMAIL).roles("ADMIN");
    }

    protected RequestPostProcessor asTeacher(String email) {
        return user(email).roles("TEACHER");
    }

    protected RequestPostProcessor asStudent(String email) {
        return user(email).roles("STUDENT");
    }

    // ---- JSON bodies ----

    protected String adminJson(String name, String email, String password) {
        return """
                {"fullName":"%s","email":"%s","password":"%s"}
                """.formatted(name, email, password);
    }

    protected String teacherJson(String name, String email, String password) {
        return """
                {"fullName":"%s","email":"%s","password":"%s","department":"CSE","designation":"Lecturer"}
                """.formatted(name, email, password);
    }

    protected String studentJson(String name, String email, String password, String roll) {
        return """
                {"fullName":"%s","email":"%s","password":"%s","department":"CSE","semester":3,"rollNumber":"%s"}
                """.formatted(name, email, password, roll);
    }

    // ---- saving test users ----

    private Admin saveAdmin(String name, String email) {
        Admin a = new Admin();
        a.setFullName(name);
        a.setEmail(email);
        a.setPassword(passwordEncoder.encode(PASSWORD));
        return adminRepository.save(a);
    }

    private Teacher saveTeacher(String name, String email) {
        Teacher t = new Teacher();
        t.setFullName(name);
        t.setEmail(email);
        t.setPassword(passwordEncoder.encode(PASSWORD));
        t.setDepartment("CSE");
        t.setDesignation("Lecturer");
        return teacherRepository.save(t);
    }

    private Student saveStudent(String name, String email, String roll, boolean approved) {
        Student s = new Student();
        s.setFullName(name);
        s.setEmail(email);
        s.setPassword(passwordEncoder.encode(PASSWORD));
        s.setDepartment("CSE");
        s.setSemester(3);
        s.setRollNumber(roll);
        s.setApproved(approved);
        return studentRepository.save(s);
    }
}
