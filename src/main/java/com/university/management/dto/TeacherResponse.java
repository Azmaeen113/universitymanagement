package com.university.management.dto;

import com.university.management.entity.Teacher;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeacherResponse {

    private Long id;
    private String fullName;
    private String email;
    private String department;
    private String designation;

    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(teacher.getId(), teacher.getFullName(), teacher.getEmail(),
                teacher.getDepartment(), teacher.getDesignation());
    }
}
