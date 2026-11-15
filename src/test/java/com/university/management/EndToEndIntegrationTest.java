package com.university.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.university.management.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Integration tests: real login (session cookie), real services and a real (H2) database.
 * Each test walks through a whole story, not a single URL.
 */
class EndToEndIntegrationTest extends BaseIntegrationTest {

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/login").param("email", email).param("password", password).with(csrf()));
    }

    private MockHttpSession sessionOf(ResultActions loginResult) {
        return (MockHttpSession) loginResult.andReturn().getRequest().getSession(false);
    }

    @Test
    void teacherRegistersStudentAdminApprovesThenStudentCanLogIn() throws Exception {
        // 1. teacher registers a student through the REST API
        mockMvc.perform(post("/api/students").with(httpBasic(TEACHER1_EMAIL, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("Flow Student", "flow@test.com", "secret123", "ROLL-FLOW")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approved").value(false));
        Student created = studentRepository.findByEmailIgnoreCase("flow@test.com").orElseThrow();

        // 2. the student is refused at login while pending
        login("flow@test.com", "secret123").andExpect(redirectedUrl("/login?pending"));

        // 3. the admin sees the student on the pending page and approves
        mockMvc.perform(get("/admin/students/pending").with(asAdmin()))
                .andExpect(content().string(containsString("Flow Student")));
        mockMvc.perform(put("/api/students/" + created.getId() + "/approve").with(httpBasic(ADMIN_EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true));

        // 4. now the same login works and goes to the student dashboard
        login("flow@test.com", "secret123").andExpect(redirectedUrl("/student/dashboard"));
    }

    @Test
    void sessionKeepsUserLoggedInUntilLogout() throws Exception {
        MockHttpSession session = sessionOf(login(TEACHER1_EMAIL, PASSWORD).andExpect(redirectedUrl("/teacher/dashboard")));
        assertThat(session).isNotNull();

        // the session cookie alone is enough to open the dashboard (no user() shortcut here)
        mockMvc.perform(get("/teacher/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Teacher One")));

        // but the same session is not allowed into the admin area
        mockMvc.perform(get("/admin/dashboard").session(session)).andExpect(status().isForbidden());

        // logout ends the session (a browser sends "Accept: text/html", so Spring Security redirects to /login?logout)
        mockMvc.perform(post("/logout").session(session).accept(MediaType.TEXT_HTML).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void studentWhoChangesOwnEmailIsLoggedOutAndMustUseNewEmail() throws Exception {
        MockHttpSession session = sessionOf(login(STUDENT1_EMAIL, PASSWORD));

        mockMvc.perform(post("/student/students/" + student1.getId()).session(session).with(csrf())
                        .param("fullName", "Student One").param("email", "renamed@test.com").param("password", "")
                        .param("department", "CSE").param("semester", "3").param("rollNumber", "ROLL-1"))
                .andExpect(redirectedUrl("/login?emailChanged"));
        assertThat(session.isInvalid()).isTrue();

        assertThat(studentRepository.findById(student1.getId()).orElseThrow().getEmail()).isEqualTo("renamed@test.com");
        login(STUDENT1_EMAIL, PASSWORD).andExpect(redirectedUrl("/login?error"));
        login("renamed@test.com", PASSWORD).andExpect(redirectedUrl("/student/dashboard"));
    }
}
