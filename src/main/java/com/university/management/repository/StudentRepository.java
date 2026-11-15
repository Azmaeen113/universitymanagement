package com.university.management.repository;

import com.university.management.entity.Student;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRollNumber(String rollNumber);

    // approved = false  -> pending list for the admin
    List<Student> findByApproved(boolean approved);

    long countByApproved(boolean approved);
}
