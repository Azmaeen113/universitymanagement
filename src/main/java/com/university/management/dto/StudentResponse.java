package com.university.management.dto;

import com.university.management.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentResponse {

    private Long id;
    private String fullName;
    private String email;
    private String department;
    private Integer semester;
    private String rollNumber;
    private boolean approved;

    public static StudentResponse from(Student student) {
        return new StudentResponse(student.getId(), student.getFullName(), student.getEmail(),
                student.getDepartment(), student.getSemester(), student.getRollNumber(),
                student.isApproved());
    }
}
