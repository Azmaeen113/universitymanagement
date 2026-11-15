package com.university.management.controller.web;

import com.university.management.dto.StudentRequest;
import com.university.management.entity.Student;
import com.university.management.exception.DuplicateResourceException;
import com.university.management.security.AuthUtil;
import com.university.management.service.StudentService;
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

/** Pages for the student role (URL starts with /student). */
@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentWebController {

    private final StudentService studentService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        Student student = studentService.findByEmail(authentication.getName()).orElse(null);
        if (student == null) {
            return "redirect:/admin/dashboard";   // an admin opened this URL, he has no student profile
        }
        model.addAttribute("student", student);
        return "student/dashboard";
    }

    // Student directory: everybody sees all students, edit/delete buttons only on the own row
    @GetMapping("/students")
    public String directory(Model model, Authentication authentication) {
        model.addAttribute("students", studentService.findAll());
        model.addAttribute("basePath", "/student/students");
        model.addAttribute("canAdd", false);
        model.addAttribute("isAdmin", AuthUtil.isAdmin(authentication));
        model.addAttribute("currentEmail", authentication.getName());
        return "shared/students";
    }

    @GetMapping("/students/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Authentication authentication) {
        Student student = studentService.findForModify(id, authentication);   // 403 if not owner
        StudentRequest form = new StudentRequest();
        form.setFullName(student.getFullName());
        form.setEmail(student.getEmail());
        form.setDepartment(student.getDepartment());
        form.setSemester(student.getSemester());
        form.setRollNumber(student.getRollNumber());
        model.addAttribute("form", form);
        return formPage(model, id);
    }

    @PostMapping("/students/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") StudentRequest form,
                         BindingResult result, Model model, RedirectAttributes redirect,
                         Authentication authentication, HttpServletRequest request) {
        if (!result.hasErrors()) {
            try {
                Student old = studentService.findForModify(id, authentication);
                boolean editingSelf = old.getEmail().equalsIgnoreCase(authentication.getName());
                Student saved = studentService.update(id, form, authentication);

                // the login name is the email, so after an email change the user must log in again
                if (editingSelf && !saved.getEmail().equalsIgnoreCase(authentication.getName())) {
                    request.getSession().invalidate();
                    return "redirect:/login?emailChanged";
                }
                redirect.addFlashAttribute("success", "Profile updated");
                return "redirect:/student/students";
            } catch (DuplicateResourceException e) {
                result.rejectValue(e.getField(), "duplicate", e.getMessage());
            }
        }
        return formPage(model, id);
    }

    @PostMapping("/students/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication,
                         RedirectAttributes redirect, HttpServletRequest request) {
        studentService.delete(id, authentication);   // 403 if not owner (admin may delete anyone)
        if (!AuthUtil.isAdmin(authentication)) {
            request.getSession().invalidate();       // a student deleted his own account
            return "redirect:/login?deleted";
        }
        redirect.addFlashAttribute("success", "Student deleted");
        return "redirect:/student/students";
    }

    private String formPage(Model model, Long id) {
        model.addAttribute("title", "Edit My Profile");
        model.addAttribute("formAction", "/student/students/" + id);
        model.addAttribute("backUrl", "/student/students");
        model.addAttribute("isNew", false);
        return "shared/student-form";
    }
}
