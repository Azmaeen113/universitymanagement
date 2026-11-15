package com.university.management.controller.web;

import com.university.management.dto.StudentRequest;
import com.university.management.dto.TeacherRequest;
import com.university.management.entity.Teacher;
import com.university.management.exception.BadRequestException;
import com.university.management.exception.DuplicateResourceException;
import com.university.management.security.AuthUtil;
import com.university.management.service.StudentService;
import com.university.management.service.TeacherService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Pages for the teacher role (URL starts with /teacher). */
@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherWebController {

    private final TeacherService teacherService;
    private final StudentService studentService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        Teacher teacher = teacherService.findByEmail(authentication.getName()).orElse(null);
        if (teacher == null) {
            return "redirect:/admin/dashboard";   // an admin opened this URL, he has no teacher profile
        }
        model.addAttribute("teacher", teacher);
        return "teacher/dashboard";
    }

    // ---------- teachers: everybody can see, only the owner can edit/delete ----------

    @GetMapping("/teachers")
    public String teachers(Model model, Authentication authentication) {
        model.addAttribute("teachers", teacherService.findAll());
        model.addAttribute("basePath", "/teacher/teachers");
        model.addAttribute("canAdd", false);
        model.addAttribute("isAdmin", AuthUtil.isAdmin(authentication));
        model.addAttribute("currentEmail", authentication.getName());
        return "shared/teachers";
    }

    @GetMapping("/teachers/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Authentication authentication) {
        Teacher teacher = teacherService.findForModify(id, authentication);   // 403 if not owner
        TeacherRequest form = new TeacherRequest();
        form.setFullName(teacher.getFullName());
        form.setEmail(teacher.getEmail());
        form.setDepartment(teacher.getDepartment());
        form.setDesignation(teacher.getDesignation());
        model.addAttribute("form", form);
        return teacherFormPage(model, id);
    }

    @PostMapping("/teachers/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") TeacherRequest form,
                         BindingResult result, Model model, RedirectAttributes redirect,
                         Authentication authentication, HttpServletRequest request) {
        if (!result.hasErrors()) {
            try {
                Teacher old = teacherService.findForModify(id, authentication);
                boolean editingSelf = old.getEmail().equalsIgnoreCase(authentication.getName());
                Teacher saved = teacherService.update(id, form, authentication);

                // the login name is the email, so after an email change the user must log in again
                if (editingSelf && !saved.getEmail().equalsIgnoreCase(authentication.getName())) {
                    request.getSession().invalidate();
                    return "redirect:/login?emailChanged";
                }
                redirect.addFlashAttribute("success", "Profile updated");
                return "redirect:/teacher/teachers";
            } catch (DuplicateResourceException e) {
                result.rejectValue(e.getField(), "duplicate", e.getMessage());
            }
        }
        return teacherFormPage(model, id);
    }

    @PostMapping("/teachers/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication,
                         RedirectAttributes redirect, HttpServletRequest request) {
        teacherService.delete(id, authentication);   // 403 if not owner (admin may delete anyone)
        if (!AuthUtil.isAdmin(authentication)) {
            request.getSession().invalidate();       // a teacher deleted his own account
            return "redirect:/login?deleted";
        }
        redirect.addFlashAttribute("success", "Teacher deleted");
        return "redirect:/teacher/teachers";
    }

    // ---------- students: view-only list + register (pending approval) ----------

    @GetMapping("/students")
    public String students(Model model, Authentication authentication) {
        model.addAttribute("students", studentService.findAll());
        model.addAttribute("basePath", null);        // teachers cannot edit/delete students
        model.addAttribute("canAdd", true);
        model.addAttribute("addUrl", "/teacher/students/new");
        model.addAttribute("isAdmin", AuthUtil.isAdmin(authentication));
        model.addAttribute("currentEmail", authentication.getName());
        return "shared/students";
    }

    @GetMapping("/students/new")
    public String registerForm(Model model) {
        model.addAttribute("form", new StudentRequest());
        return registerPage(model);
    }

    @PostMapping("/students")
    public String register(@Valid @ModelAttribute("form") StudentRequest form, BindingResult result,
                           Model model, RedirectAttributes redirect, Authentication authentication) {
        if (!result.hasErrors()) {
            try {
                studentService.create(form, authentication);   // teacher -> approved = false
                redirect.addFlashAttribute("success",
                        "Student registered. The student can log in after the Admin approves the account.");
                return "redirect:/teacher/students";
            } catch (DuplicateResourceException e) {
                result.rejectValue(e.getField(), "duplicate", e.getMessage());
            } catch (BadRequestException e) {
                result.rejectValue("password", "required", e.getMessage());
            }
        }
        return registerPage(model);
    }

    private String teacherFormPage(Model model, Long id) {
        model.addAttribute("title", "Edit My Profile");
        model.addAttribute("formAction", "/teacher/teachers/" + id);
        model.addAttribute("backUrl", "/teacher/teachers");
        model.addAttribute("isNew", false);
        return "shared/teacher-form";
    }

    private String registerPage(Model model) {
        model.addAttribute("title", "Register Student");
        model.addAttribute("formAction", "/teacher/students");
        model.addAttribute("backUrl", "/teacher/students");
        model.addAttribute("isNew", true);
        model.addAttribute("notice", "Student will require Admin approval before login");
        return "shared/student-form";
    }
}
