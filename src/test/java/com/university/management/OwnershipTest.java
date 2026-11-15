package com.university.management;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** A teacher / student may change only his or her own record. The admin may change all. */
class OwnershipTest extends BaseIntegrationTest {

    // ---------- teachers (API) ----------

    @Test
    void teacherCannotUpdateAnotherTeacher() throws Exception {
        mockMvc.perform(put("/api/teachers/" + teacher2.getId()).with(asTeacher(TEACHER1_EMAIL)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherJson("Hacked", TEACHER2_EMAIL, "")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotDeleteAnotherTeacher() throws Exception {
        mockMvc.perform(delete("/api/teachers/" + teacher2.getId()).with(asTeacher(TEACHER1_EMAIL)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanUpdateOwnProfile() throws Exception {
        mockMvc.perform(put("/api/teachers/" + teacher1.getId()).with(asTeacher(TEACHER1_EMAIL)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherJson("New Name", TEACHER1_EMAIL, "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Name"));
    }

    @Test
    void teacherCanDeleteOwnProfile() throws Exception {
        mockMvc.perform(delete("/api/teachers/" + teacher1.getId()).with(asTeacher(TEACHER1_EMAIL)).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanUpdateAnyTeacher() throws Exception {
        mockMvc.perform(put("/api/teachers/" + teacher2.getId()).with(asAdmin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherJson("Changed By Admin", TEACHER2_EMAIL, "")))
                .andExpect(status().isOk());
    }

    // ---------- students (API) ----------

    @Test
    void studentCannotUpdateAnotherStudent() throws Exception {
        mockMvc.perform(put("/api/students/" + student2.getId()).with(asStudent(STUDENT1_EMAIL)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("Hacked", STUDENT2_EMAIL, "", "ROLL-2")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotDeleteAnotherStudent() throws Exception {
        mockMvc.perform(delete("/api/students/" + student2.getId()).with(asStudent(STUDENT1_EMAIL)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCanUpdateAndDeleteOwnProfile() throws Exception {
        mockMvc.perform(put("/api/students/" + student1.getId()).with(asStudent(STUDENT1_EMAIL)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("My New Name", STUDENT1_EMAIL, "", "ROLL-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("My New Name"));

        mockMvc.perform(delete("/api/students/" + student1.getId()).with(asStudent(STUDENT1_EMAIL)).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void teacherCannotEditOrDeleteStudents() throws Exception {
        mockMvc.perform(put("/api/students/" + student1.getId()).with(asTeacher(TEACHER1_EMAIL)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("Hacked", STUDENT1_EMAIL, "", "ROLL-1")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/students/" + student1.getId()).with(asTeacher(TEACHER1_EMAIL)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteAnyStudent() throws Exception {
        mockMvc.perform(delete("/api/students/" + student2.getId()).with(asAdmin()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ---------- web pages ----------

    @Test
    void teacherCannotOpenEditPageOfAnotherTeacher() throws Exception {
        mockMvc.perform(get("/teacher/teachers/" + teacher2.getId() + "/edit").with(asTeacher(TEACHER1_EMAIL)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/teacher/teachers/" + teacher1.getId() + "/edit").with(asTeacher(TEACHER1_EMAIL)))
                .andExpect(status().isOk());
    }

    @Test
    void teacherCannotPostEditFormOfAnotherTeacher() throws Exception {
        mockMvc.perform(post("/teacher/teachers/" + teacher2.getId()).with(asTeacher(TEACHER1_EMAIL)).with(csrf())
                        .param("fullName", "Hacked").param("email", TEACHER2_EMAIL)
                        .param("department", "CSE").param("designation", "Lecturer"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/teacher/teachers/" + teacher2.getId() + "/delete")
                        .with(asTeacher(TEACHER1_EMAIL)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotOpenEditPageOfAnotherStudent() throws Exception {
        mockMvc.perform(get("/student/students/" + student2.getId() + "/edit").with(asStudent(STUDENT1_EMAIL)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/student/students/" + student1.getId() + "/edit").with(asStudent(STUDENT1_EMAIL)))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotPostEditFormOfAnotherStudent() throws Exception {
        mockMvc.perform(post("/student/students/" + student2.getId()).with(asStudent(STUDENT1_EMAIL)).with(csrf())
                        .param("fullName", "Hacked").param("email", STUDENT2_EMAIL)
                        .param("department", "CSE").param("semester", "3").param("rollNumber", "ROLL-2"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/student/students/" + student2.getId() + "/delete")
                        .with(asStudent(STUDENT1_EMAIL)).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
