package com.rhtsystem.randevuhastatakip.service;

import com.rhtsystem.randevuhastatakip.model.Patient;
import com.rhtsystem.randevuhastatakip.model.Role;
import com.rhtsystem.randevuhastatakip.model.User;
import com.rhtsystem.randevuhastatakip.model.Doctor;
import com.rhtsystem.randevuhastatakip.repository.DoctorRepository;
import com.rhtsystem.randevuhastatakip.repository.PatientRepository;
import com.rhtsystem.randevuhastatakip.repository.RoleRepository;
import com.rhtsystem.randevuhastatakip.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    // Rol sabitleri (Prefix'li olanlar DB ve GrantedAuthority için, prefix'siz olanlar hasRole() için)
    public static final String ROLE_HASTA_PREFIXED = "ROLE_HASTA";
    public static final String ROLE_DOKTOR_PREFIXED = "ROLE_DOKTOR";
    public static final String ROLE_ADMIN_PREFIXED = "ROLE_ADMIN";

    public static final String ROLE_HASTA = "HASTA";
    public static final String ROLE_DOKTOR = "DOKTOR";
    public static final String ROLE_ADMIN = "ADMIN";

    @Autowired
    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PatientRepository patientRepository,
                       DoctorRepository doctorRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerNewUser(User user, String roleNamePrefixed, Optional<String> specializationOpt) throws Exception {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new Exception("Hata: Kullanıcı adı (" + user.getUsername() + ") zaten mevcut!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Set<Role> roles = new HashSet<>();
        // RoleRepository'den prefix'li rol adıyla arama yapıyoruz
        Role userRole = roleRepository.findByName(roleNamePrefixed)
                .orElseThrow(() -> new RuntimeException("Hata: Rol bulunamadı. (" + roleNamePrefixed + ")"));
        roles.add(userRole);
        user.setRoles(roles);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        // Rolüne göre Patient veya Doctor oluştur (Admin hariç)
        if (ROLE_HASTA_PREFIXED.equals(roleNamePrefixed)) {
            Patient patient = new Patient();
            patient.setUser(savedUser);
            patientRepository.save(patient);
        } else if (ROLE_DOKTOR_PREFIXED.equals(roleNamePrefixed)) {
            String specialization = specializationOpt
                .orElseThrow(() -> new IllegalArgumentException("Doktorlar için uzmanlık alanı zorunludur."));
            Doctor doctor = new Doctor();
            doctor.setUser(savedUser);
            doctor.setSpecialization(specialization);
            doctorRepository.save(doctor);
        }
        // Admin rolü için ek bir Patient/Doctor kaydı oluşturmuyoruz.

        return savedUser;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // İleride kullanıcı listeleme, silme vb. metodlar eklenebilir.
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
}