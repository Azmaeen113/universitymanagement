package com.university.management;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Which role may open which URL. */
class AuthorizationTest extends BaseIntegrationTest {

    // ---------- web pages ----------

    @Test
    void teacherCannotOpenAdminPages() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(asTeacher(TEACHER1_EMAIL))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/students/pending").with(asTeacher(TEACHER1_EMAIL))).andExpect(status().isForbidden());
    }

    @Test
    void studentCannotOpenAdminPages() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(asStudent(STUDENT1_EMAIL))).andExpect(status().isForbidden());
    }

    @Test
    void studentCannotOpenTeacherPages() throws Exception {
        mockMvc.perform(get("/teacher/dashboard").with(asStudent(STUDENT1_EMAIL))).andExpect(status().isForbidden());
        mockMvc.perform(get("/teacher/students/new").with(asStudent(STUDENT1_EMAIL))).andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotOpenStudentPages() throws Exception {
        mockMvc.perform(get("/student/dashboard").with(asTeacher(TEACHER1_EMAIL))).andExpect(status().isForbidden());
    }

    @Test
    void adminCanOpenEveryArea() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(asAdmin())).andExpect(status().isOk());
        mockMvc.perform(get("/teacher/teachers").with(asAdmin())).andExpect(status().isOk());
        mockMvc.perform(get("/student/students").with(asAdmin())).andExpect(status().isOk());
    }

    @Test
    void eachRoleCanOpenItsOwnDashboard() throws Exception {
        mockMvc.perform(get("/teacher/dashboard").with(asTeacher(TEACHER1_EMAIL))).andExpect(status().isOk());
        mockMvc.perform(get("/student/dashboard").with(asStudent(STUDENT1_EMAIL))).andExpect(status().isOk());
    }

    @Test
    void adminOpeningTeacherDashboardIsSentToAdminDashboard() throws Exception {
        mockMvc.perform(get("/teacher/dashboard").with(asAdmin())).andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void teacherCannotPostToAdminPagesEvenWithCsrf() throws Exception {
        mockMvc.perform(post("/admin/students/" + pendingStudent.getId() + "/approve")
                        .with(asTeacher(TEACHER1_EMAIL)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ---------- REST API ----------

    @Test
    void teacherCannotUseAdminApi() throws Exception {
        mockMvc.perform(get("/api/admins").with(asTeacher(TEACHER1_EMAIL)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void studentCannotUseTeacherApi() throws Exception {
        mockMvc.perform(get("/api/teachers").with(asStudent(STUDENT1_EMAIL))).andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotCreateTeacher() throws Exception {
        mockMvc.perform(post("/api/teachers").with(asTeacher(TEACHER1_EMAIL)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherJson("New Teacher", "new.teacher@test.com", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherAndStudentCanListStudents() throws Exception {
        mockMvc.perform(get("/api/students").with(asTeacher(TEACHER1_EMAIL))).andExpect(status().isOk());
        mockMvc.perform(get("/api/students").with(asStudent(STUDENT1_EMAIL))).andExpect(status().isOk());
    }

    @Test
    void onlyAdminCanApprove() throws Exception {
        String url = "/api/students/" + pendingStudent.getId() + "/approve";
        mockMvc.perform(put(url).with(asTeacher(TEACHER1_EMAIL)).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(put(url).with(asStudent(STUDENT1_EMAIL)).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(put(url).with(asAdmin()).with(csrf())).andExpect(status().isOk());
    }

    @Test
    void studentCannotRegisterStudents() throws Exception {
        mockMvc.perform(post("/api/students").with(asStudent(STUDENT1_EMAIL)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("New", "new@test.com", PASSWORD, "ROLL-NEW")))
                .andExpect(status().isForbidden());
    }

    @Test
    void postWithoutCsrfIsForbiddenForSessionUsers() throws Exception {
        mockMvc.perform(post("/api/students").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("New", "new@test.com", PASSWORD, "ROLL-NEW")))
                .andExpect(status().isForbidden());
    }
}
