package com.university.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.university.management.config.DataSeeder;
import org.junit.jupiter.api.Test;

/** The seeder is switched off in the "test" profile, so we run it by hand here. */
class DataSeederTest extends BaseIntegrationTest {

    @Test
    void seederCreatesWorkingAccountsAndIsSafeToRunTwice() throws Exception {
        studentRepository.deleteAll();
        teacherRepository.deleteAll();
        adminRepository.deleteAll();

        DataSeeder seeder = new DataSeeder(adminRepository, teacherRepository, studentRepository, passwordEncoder);
        seeder.run();
        seeder.run();   // second run must not create duplicates

        assertThat(adminRepository.count()).isEqualTo(2);
        assertThat(teacherRepository.count()).isEqualTo(3);
        assertThat(studentRepository.count()).isEqualTo(5);
        assertThat(studentRepository.countByApproved(false)).isEqualTo(2);

        login("admin@university.edu", "admin123", "/admin/dashboard");
        login("sarah.khan@university.edu", "teacher123", "/teacher/dashboard");
        login("ayesha.rahman@student.edu", "student123", "/student/dashboard");
        login("sakib.hasan@student.edu", "student123", "/login?pending");
    }

    private void login(String email, String password, String expectedRedirect) throws Exception {
        mockMvc.perform(post("/login").param("email", email).param("password", password).with(csrf()))
                .andExpect(redirectedUrl(expectedRedirect));
    }
}
