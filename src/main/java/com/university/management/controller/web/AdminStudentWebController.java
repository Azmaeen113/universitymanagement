package com.university.management.controller.web;

import com.university.management.dto.StudentRequest;
import com.university.management.entity.Student;
import com.university.management.exception.BadRequestException;
import com.university.management.exception.DuplicateResourceException;
import com.university.management.service.StudentService;
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

/** Admin CRUD pages for students, plus the pending approval list (/admin/students/pending). */
@Controller
@RequestMapping("/admin/students")
@RequiredArgsConstructor
public class AdminStudentWebController {

    private final StudentService studentService;

    @GetMapping
    public String list(Model model, Authentication authentication) {
        model.addAttribute("students", studentService.findAll());
        model.addAttribute("basePath", "/admin/students");
        model.addAttribute("canAdd", true);
        model.addAttribute("isAdmin", true);
        model.addAttribute("currentEmail", authentication.getName());
        return "shared/students";
    }

    // ---------- approval workflow ----------

    @GetMapping("/pending")
    public String pending(Model model) {
        model.addAttribute("students", studentService.findPending());
        return "admin/pending";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        studentService.approve(id);
        redirect.addFlashAttribute("success", "Student approved. The student can log in now.");
        return "redirect:/admin/students/pending";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirect) {
        studentService.reject(id);
        redirect.addFlashAttribute("success", "Student rejected and removed.");
        return "redirect:/admin/students/pending";
    }

    // ---------- CRUD ----------

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new StudentRequest());
        return formPage(model, "Add Student", "/admin/students", true);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") StudentRequest form, BindingResult result,
                         Model model, RedirectAttributes redirect, Authentication authentication) {
        if (!result.hasErrors()) {
            try {
                studentService.create(form, authentication);   // admin -> approved = true
                redirect.addFlashAttribute("success", "Student created");
                return "redirect:/admin/students";
            } catch (DuplicateResourceException e) {
                result.rejectValue(e.getField(), "duplicate", e.getMessage());
            } catch (BadRequestException e) {
                result.rejectValue("password", "required", e.getMessage());
            }
        }
        return formPage(model, "Add Student", "/admin/students", true);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Student student = studentService.findById(id);
        StudentRequest form = new StudentRequest();
        form.setFullName(student.getFullName());
        form.setEmail(student.getEmail());
        form.setDepartment(student.getDepartment());
        form.setSemester(student.getSemester());
        form.setRollNumber(student.getRollNumber());
        model.addAttribute("form", form);
        return formPage(model, "Edit Student", "/admin/students/" + id, false);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") StudentRequest form,
                         BindingResult result, Model model, RedirectAttributes redirect,
                         Authentication authentication) {
        if (!result.hasErrors()) {
            try {
                studentService.update(id, form, authentication);
                redirect.addFlashAttribute("success", "Student updated");
                return "redirect:/admin/students";
            } catch (DuplicateResourceException e) {
                result.rejectValue(e.getField(), "duplicate", e.getMessage());
            }
        }
        return formPage(model, "Edit Student", "/admin/students/" + id, false);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        studentService.delete(id, authentication);
        redirect.addFlashAttribute("success", "Student deleted");
        return "redirect:/admin/students";
    }

    private String formPage(Model model, String title, String action, boolean isNew) {
        model.addAttribute("title", title);
        model.addAttribute("formAction", action);
        model.addAttribute("backUrl", "/admin/students");
        model.addAttribute("isNew", isNew);
        return "shared/student-form";
    }
}
