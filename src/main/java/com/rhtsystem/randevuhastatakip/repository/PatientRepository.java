package com.rhtsystem.randevuhastatakip.repository;

import com.rhtsystem.randevuhastatakip.model.Patient;
import com.rhtsystem.randevuhastatakip.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    // Patient ID'si User ID'si ile aynı olduğu için direkt findById yeterli olabilir.
    // Ancak user objesi üzerinden patient bulmak istersek:
    Optional<Patient> findByUser(User user);
    Optional<Patient> findByUserId(Long userId); // User ID'sine göre Patient bulma
}