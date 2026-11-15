package com.university.management;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** An email can exist only once in admins + teachers + students. A roll number only once. */
class UniquenessTest extends BaseIntegrationTest {

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).with(asAdmin()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void adminEmailCannotBeUsedForNewTeacher() throws Exception {
        postJson("/api/teachers", teacherJson("Copy", ADMIN_EMAIL, PASSWORD)).andExpect(status().isConflict());
    }

    @Test
    void teacherEmailCannotBeUsedForNewStudent() throws Exception {
        postJson("/api/students", studentJson("Copy", TEACHER1_EMAIL, PASSWORD, "ROLL-X"))
                .andExpect(status().isConflict());
    }

    @Test
    void studentEmailCannotBeUsedForNewAdmin() throws Exception {
        postJson("/api/admins", adminJson("Copy", STUDENT1_EMAIL, PASSWORD)).andExpect(status().isConflict());
    }

    @Test
    void emailCheckIgnoresUpperCase() throws Exception {
        postJson("/api/admins", adminJson("Copy", "TEACHER1@TEST.COM", PASSWORD)).andExpect(status().isConflict());
    }

    @Test
    void sameTableDuplicateIsRejectedToo() throws Exception {
        postJson("/api/teachers", teacherJson("Copy", TEACHER2_EMAIL, PASSWORD)).andExpect(status().isConflict());
    }

    @Test
    void updateCannotTakeEmailOfAnotherTable() throws Exception {
        mockMvc.perform(put("/api/teachers/" + teacher1.getId()).with(asAdmin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherJson("Teacher One", STUDENT1_EMAIL, "")))
                .andExpect(status().isConflict());
    }

    @Test
    void updateKeepingOwnEmailIsAllowed() throws Exception {
        mockMvc.perform(put("/api/teachers/" + teacher1.getId()).with(asAdmin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherJson("Teacher One Renamed", TEACHER1_EMAIL, "")))
                .andExpect(status().isOk());
    }

    @Test
    void rollNumberMustBeUnique() throws Exception {
        postJson("/api/students", studentJson("Copy", "copy@test.com", PASSWORD, "ROLL-1"))
                .andExpect(status().isConflict());
    }

    @Test
    void duplicateEmailInWebFormShowsFieldError() throws Exception {
        mockMvc.perform(post("/admin/teachers").with(asAdmin()).with(csrf())
                        .param("fullName", "Copy").param("email", STUDENT1_EMAIL).param("password", PASSWORD)
                        .param("department", "CSE").param("designation", "Lecturer"))
                .andExpect(status().isOk())
                .andExpect(view().name("shared/teacher-form"))
                .andExpect(model().attributeHasFieldErrors("form", "email"));
    }

    @Test
    void duplicateRollNumberInWebFormShowsFieldError() throws Exception {
        mockMvc.perform(post("/teacher/students").with(asTeacher(TEACHER1_EMAIL)).with(csrf())
                        .param("fullName", "Copy").param("email", "copy@test.com").param("password", PASSWORD)
                        .param("department", "CSE").param("semester", "2").param("rollNumber", "ROLL-1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "rollNumber"));
    }
}
