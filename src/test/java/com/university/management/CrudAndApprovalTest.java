package com.university.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.university.management.entity.Admin;
import com.university.management.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** CRUD through the REST API, the approval workflow and the admin web pages. */
class CrudAndApprovalTest extends BaseIntegrationTest {

    // ---------- Admin CRUD ----------

    @Test
    void adminCrud() throws Exception {
        // create -> 201, password never returned
        mockMvc.perform(post("/api/admins").with(asAdmin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminJson("Second Admin", "second@test.com", "secret123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("second@test.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        Admin saved = adminRepository.findByEmailIgnoreCase("second@test.com").orElseThrow();
        // the password in the database is a BCrypt hash, not the plain text
        assertThat(saved.getPassword()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", saved.getPassword())).isTrue();

        // read
        mockMvc.perform(get("/api/admins").with(asAdmin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/admins/" + saved.getId()).with(asAdmin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Second Admin"));

        // update (empty password = keep old password)
        mockMvc.perform(put("/api/admins/" + saved.getId()).with(asAdmin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminJson("Renamed Admin", "second@test.com", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Renamed Admin"));
        Admin updated = adminRepository.findById(saved.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("secret123", updated.getPassword())).isTrue();

        // delete -> 204, then 404
        mockMvc.perform(delete("/api/admins/" + saved.getId()).with(asAdmin()).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admins/" + saved.getId()).with(asAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCannotDeleteHimself() throws Exception {
        mockMvc.perform(delete("/api/admins/" + admin.getId()).with(asAdmin()).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ---------- Teacher CRUD ----------

    @Test
    void teacherCrudByAdmin() throws Exception {
        mockMvc.perform(post("/api/teachers").with(asAdmin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherJson("New Teacher", "new.teacher@test.com", "secret123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.department").value("CSE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/teachers").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mockMvc.perform(get("/api/teachers/" + teacher1.getId()).with(asTeacher(TEACHER2_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEACHER1_EMAIL));

        mockMvc.perform(delete("/api/teachers/" + teacher1.getId()).with(asAdmin()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ---------- Student CRUD ----------

    @Test
    void studentCreatedByAdminIsApproved() throws Exception {
        mockMvc.perform(post("/api/students").with(asAdmin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("Admin Made", "admin.made@test.com", "secret123", "ROLL-A")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void studentCreatedByTeacherIsPending() throws Exception {
        mockMvc.perform(post("/api/students").with(asTeacher(TEACHER1_EMAIL)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("Teacher Made", "teacher.made@test.com", "secret123", "ROLL-T")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approved").value(false));
    }

    @Test
    void unknownIdReturns404() throws Exception {
        mockMvc.perform(get("/api/students/99999").with(asAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------- validation ----------

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/students").with(asAdmin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","email":"not-an-email","password":"123","department":"CSE","semester":20,"rollNumber":"R1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.semester").exists());
    }

    @Test
    void missingPasswordOnCreateReturns400() throws Exception {
        mockMvc.perform(post("/api/admins").with(asAdmin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminJson("No Password", "nopass@test.com", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void brokenJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/admins").with(asAdmin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest());
    }

    // ---------- approval workflow ----------

    @Test
    void approveMakesPendingStudentApproved() throws Exception {
        mockMvc.perform(put("/api/students/" + pendingStudent.getId() + "/approve").with(asAdmin()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true));

        Student reloaded = studentRepository.findById(pendingStudent.getId()).orElseThrow();
        assertThat(reloaded.isApproved()).isTrue();

        // and now the student can log in
        mockMvc.perform(post("/login").param("email", PENDING_EMAIL).param("password", PASSWORD).with(csrf()))
                .andExpect(redirectedUrl("/student/dashboard"));
    }

    @Test
    void rejectDeletesPendingStudent() throws Exception {
        mockMvc.perform(post("/admin/students/" + pendingStudent.getId() + "/reject").with(asAdmin()).with(csrf()))
                .andExpect(redirectedUrl("/admin/students/pending"));

        assertThat(studentRepository.findById(pendingStudent.getId())).isEmpty();
    }

    @Test
    void approveFromPendingPageWorks() throws Exception {
        mockMvc.perform(post("/admin/students/" + pendingStudent.getId() + "/approve").with(asAdmin()).with(csrf()))
                .andExpect(redirectedUrl("/admin/students/pending"));

        assertThat(studentRepository.countByApproved(false)).isZero();
    }

    @Test
    void teacherRegistersStudentFromWebFormAndItIsPending() throws Exception {
        mockMvc.perform(post("/teacher/students").with(asTeacher(TEACHER1_EMAIL)).with(csrf())
                        .param("fullName", "Web Student").param("email", "web.student@test.com")
                        .param("password", "secret123").param("department", "CSE")
                        .param("semester", "2").param("rollNumber", "ROLL-W"))
                .andExpect(redirectedUrl("/teacher/students"));

        Student created = studentRepository.findByEmailIgnoreCase("web.student@test.com").orElseThrow();
        assertThat(created.isApproved()).isFalse();
    }

    @Test
    void registerPageShowsApprovalBanner() throws Exception {
        mockMvc.perform(get("/teacher/students/new").with(asTeacher(TEACHER1_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Student will require Admin approval before login")));
    }

    // ---------- admin web pages ----------

    @Test
    void dashboardShowsCounts() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("adminCount", 1L))
                .andExpect(model().attribute("teacherCount", 2L))
                .andExpect(model().attribute("studentCount", 3L))
                .andExpect(model().attribute("pendingCount", 1L));
    }

    @Test
    void pendingPageListsPendingStudentsOnly() throws Exception {
        mockMvc.perform(get("/admin/students/pending").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pending Student")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Student One"))));
    }

    @Test
    void adminCreatesTeacherFromWebForm() throws Exception {
        mockMvc.perform(post("/admin/teachers").with(asAdmin()).with(csrf())
                        .param("fullName", "Web Teacher").param("email", "web.teacher@test.com")
                        .param("password", "secret123").param("department", "EEE").param("designation", "Professor"))
                .andExpect(redirectedUrl("/admin/teachers"));

        assertThat(teacherRepository.findByEmailIgnoreCase("web.teacher@test.com")).isPresent();
    }

    @Test
    void webFormWithEmptyFieldsShowsErrors() throws Exception {
        mockMvc.perform(post("/admin/students").with(asAdmin()).with(csrf())
                        .param("fullName", "").param("email", "bad").param("password", "")
                        .param("department", "").param("semester", "").param("rollNumber", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "fullName", "email", "department",
                        "semester", "rollNumber"));
    }
}
