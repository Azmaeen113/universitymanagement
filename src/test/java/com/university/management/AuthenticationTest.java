package com.university.management;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class AuthenticationTest extends BaseIntegrationTest {

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/login")
                .param("email", email)
                .param("password", password)
                .with(csrf()));
    }

    @Test
    void adminLoginGoesToAdminDashboard() throws Exception {
        login(ADMIN_EMAIL, PASSWORD)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void teacherLoginGoesToTeacherDashboard() throws Exception {
        login(TEACHER1_EMAIL, PASSWORD).andExpect(redirectedUrl("/teacher/dashboard"));
    }

    @Test
    void approvedStudentLoginGoesToStudentDashboard() throws Exception {
        login(STUDENT1_EMAIL, PASSWORD).andExpect(redirectedUrl("/student/dashboard"));
    }

    @Test
    void emailIsNotCaseSensitive() throws Exception {
        login("ADMIN@Test.com", PASSWORD).andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        login(ADMIN_EMAIL, "wrong-password").andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void unknownEmailIsRejected() throws Exception {
        login("nobody@test.com", PASSWORD).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void pendingStudentCannotLogin() throws Exception {
        login(PENDING_EMAIL, PASSWORD).andExpect(redirectedUrl("/login?pending"));

        mockMvc.perform(get("/login").param("pending", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your account is pending admin approval.")));
    }

    @Test
    void loginWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/login").param("email", ADMIN_EMAIL).param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserIsSentToLoginPage() throws Exception {
        mockMvc.perform(get("/admin/dashboard")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void publicPagesNeedNoLogin() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void anonymousApiCallReturns401Json() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void apiWorksWithBasicAuth() throws Exception {
        mockMvc.perform(get("/api/students").with(httpBasic(TEACHER1_EMAIL, PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void apiRejectsWrongBasicPassword() throws Exception {
        mockMvc.perform(get("/api/students").with(httpBasic(TEACHER1_EMAIL, "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiRejectsPendingStudentWithApprovalMessage() throws Exception {
        mockMvc.perform(get("/api/students").with(httpBasic(PENDING_EMAIL, PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Your account is pending admin approval."));
    }
}
