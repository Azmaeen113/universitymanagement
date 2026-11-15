package com.university.management.controller.web;

import com.university.management.dto.TeacherRequest;
import com.university.management.entity.Teacher;
import com.university.management.exception.BadRequestException;
import com.university.management.exception.DuplicateResourceException;
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

/** Admin CRUD pages for teachers (/admin/teachers). */
@Controller
@RequestMapping("/admin/teachers")
@RequiredArgsConstructor
public class AdminTeacherWebController {

    private final TeacherService teacherService;

    @GetMapping
    public String list(Model model, Authentication authentication) {
        model.addAttribute("teachers", teacherService.findAll());
        model.addAttribute("basePath", "/admin/teachers");   // edit/delete links start with this
        model.addAttribute("canAdd", true);
        model.addAttribute("isAdmin", true);
        model.addAttribute("currentEmail", authentication.getName());
        return "shared/teachers";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new TeacherRequest());
        return formPage(model, "Add Teacher", "/admin/teachers", true);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") TeacherRequest form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                teacherService.create(form);
                redirect.addFlashAttribute("success", "Teacher created");
                return "redirect:/admin/teachers";
            } catch (DuplicateResourceException e) {
                result.rejectValue(e.getField(), "duplicate", e.getMessage());
            } catch (BadRequestException e) {
                result.rejectValue("password", "required", e.getMessage());
            }
        }
        return formPage(model, "Add Teacher", "/admin/teachers", true);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Teacher teacher = teacherService.findById(id);
        TeacherRequest form = new TeacherRequest();
        form.setFullName(teacher.getFullName());
        form.setEmail(teacher.getEmail());
        form.setDepartment(teacher.getDepartment());
        form.setDesignation(teacher.getDesignation());
        model.addAttribute("form", form);
        return formPage(model, "Edit Teacher", "/admin/teachers/" + id, false);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") TeacherRequest form,
                         BindingResult result, Model model, RedirectAttributes redirect,
                         Authentication authentication) {
        if (!result.hasErrors()) {
            try {
                teacherService.update(id, form, authentication);
                redirect.addFlashAttribute("success", "Teacher updated");
                return "redirect:/admin/teachers";
            } catch (DuplicateResourceException e) {
                result.rejectValue(e.getField(), "duplicate", e.getMessage());
            }
        }
        return formPage(model, "Edit Teacher", "/admin/teachers/" + id, false);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        teacherService.delete(id, authentication);
        redirect.addFlashAttribute("success", "Teacher deleted");
        return "redirect:/admin/teachers";
    }

    private String formPage(Model model, String title, String action, boolean isNew) {
        model.addAttribute("title", title);
        model.addAttribute("formAction", action);
        model.addAttribute("backUrl", "/admin/teachers");
        model.addAttribute("isNew", isNew);
        return "shared/teacher-form";
    }
}
