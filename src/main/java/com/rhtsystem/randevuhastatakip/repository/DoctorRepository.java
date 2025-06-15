package com.rhtsystem.randevuhastatakip.repository;

import com.rhtsystem.randevuhastatakip.model.Doctor;
import com.rhtsystem.randevuhastatakip.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUser(User user);
    Optional<Doctor> findByUserId(Long userId); // User ID'sine göre Doctor bulma
    List<Doctor> findBySpecialization(String specialization); // Uzmanlık alanına göre doktorları listeleme
}