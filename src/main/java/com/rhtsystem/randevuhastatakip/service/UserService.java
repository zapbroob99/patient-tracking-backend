package com.rhtsystem.randevuhastatakip.service;

import com.rhtsystem.randevuhastatakip.model.Patient;
import com.rhtsystem.randevuhastatakip.model.Role;
import com.rhtsystem.randevuhastatakip.model.User;
import com.rhtsystem.randevuhastatakip.model.Doctor; // Doctor için
import com.rhtsystem.randevuhastatakip.repository.DoctorRepository;
import com.rhtsystem.randevuhastatakip.repository.PatientRepository;
import com.rhtsystem.randevuhastatakip.repository.RoleRepository;
import com.rhtsystem.randevuhastatakip.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // Henüz eklemedik, sonra ekleyeceğiz
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository; // Doctor için
    private final PasswordEncoder passwordEncoder; // Bu bean'i SecurityConfig'de oluşturacağız

    // ROLE İSİMLERİ (Constants olarak tanımlamak daha iyi)
    public static final String ROLE_HASTA = "ROLE_HASTA";
    public static final String ROLE_DOKTOR = "ROLE_DOKTOR";
    // public static final String ROLE_ADMIN = "ROLE_ADMIN"; // Gerekirse

    @Autowired
    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PatientRepository patientRepository,
                       DoctorRepository doctorRepository, // Doctor için
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository; // Doctor için
        this.passwordEncoder = passwordEncoder;
    }

  
    @Transactional
    public User registerNewUser(User user, String roleName, Optional<String> specializationOpt) throws Exception {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new Exception("Error: Username is already taken!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found. (" + roleName + ")"));
        roles.add(userRole);
        user.setRoles(roles);
        user.setEnabled(true);

        User savedUser = userRepository.save(user); // User'ı kaydet

        // Rolüne göre Patient veya Doctor oluştur ve User ile ilişkilendir
        if (ROLE_HASTA.equals(roleName)) {
            Patient patient = new Patient(); // Boş constructor ile oluştur
            patient.setUser(savedUser);    // User'ı set et
            // patient.setId(savedUser.getId()); // @MapsId bunu zaten yapmalı, ama explicit de deneyebiliriz
            patientRepository.save(patient);
        } else if (ROLE_DOKTOR.equals(roleName)) {
            String specialization = specializationOpt
                .orElseThrow(() -> new IllegalArgumentException("Specialization is required for doctors."));
            Doctor doctor = new Doctor(); // Boş constructor ile oluştur
            doctor.setUser(savedUser);   // User'ı set et
            doctor.setSpecialization(specialization);
            // doctor.setId(savedUser.getId()); // @MapsId bunu zaten yapmalı
            doctorRepository.save(doctor);
        }

        return savedUser; // savedUser'ı döndürmeye devam et
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Diğer kullanıcı yönetimi metodları eklenebilir (güncelleme, silme vb.)
}