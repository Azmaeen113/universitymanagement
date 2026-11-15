package com.university.management.repository;

import com.university.management.entity.Teacher;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
