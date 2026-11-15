package com.university.management.controller.web;

import com.university.management.dto.AdminRequest;
import com.university.management.entity.Admin;
import com.university.management.exception.BadRequestException;
import com.university.management.exception.DuplicateResourceException;
import com.university.management.service.AdminService;
import com.university.management.service.StudentService;
import com.university.management.service.TeacherService;
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

/** Admin dashboard and Admin CRUD pages (URL starts with /admin, ROLE_ADMIN only). */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWebController {

    private final AdminService adminService;
    private final TeacherService teacherService;
    private final StudentService studentService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("adminCount", adminService.count());
        model.addAttribute("teacherCount", teacherService.count());
        model.addAttribute("studentCount", studentService.count());
        model.addAttribute("pendingCount", studentService.countPending());
        return "admin/dashboard";
    }

    @GetMapping("/admins")
    public String list(Model model) {
        model.addAttribute("admins", adminService.findAll());
        return "admin/admins";
    }

    @GetMapping("/admins/new")
    public String newForm(Model model) {
        model.addAttribute("form", new AdminRequest());
        return formPage(model, "Add Admin", "/admin/admins", true);
    }

    @PostMapping("/admins")
    public String create(@Valid @ModelAttribute("form") AdminRequest form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                adminService.create(form);
                redirect.addFlashAttribute("success", "Admin created");
                return "redirect:/admin/admins";
            } catch (DuplicateResourceException e) {
                result.rejectValue(e.getField(), "duplicate", e.getMessage());
            } catch (BadRequestException e) {
                result.rejectValue("password", "required", e.getMessage());
            }
        }
        return formPage(model, "Add Admin", "/admin/admins", true);
    }

    @GetMapping("/admins/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Admin admin = adminService.findById(id);
        AdminRequest form = new AdminRequest();
        form.setFullName(admin.getFullName());
        form.setEmail(admin.getEmail());
        model.addAttribute("form", form);
        return formPage(model, "Edit Admin", "/admin/admins/" + id, false);
    }

    @PostMapping("/admins/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") AdminRequest form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                adminService.update(id, form);
                redirect.addFlashAttribute("success", "Admin updated");
                return "redirect:/admin/admins";
            } catch (DuplicateResourceException e) {
                result.rejectValue(e.getField(), "duplicate", e.getMessage());
            }
        }
        return formPage(model, "Edit Admin", "/admin/admins/" + id, false);
    }

    @PostMapping("/admins/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        try {
            adminService.delete(id, authentication);
            redirect.addFlashAttribute("success", "Admin deleted");
        } catch (BadRequestException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/admins";
    }

    private String formPage(Model model, String title, String action, boolean isNew) {
        model.addAttribute("title", title);
        model.addAttribute("formAction", action);
        model.addAttribute("backUrl", "/admin/admins");
        model.addAttribute("isNew", isNew);
        return "shared/admin-form";
    }
}
