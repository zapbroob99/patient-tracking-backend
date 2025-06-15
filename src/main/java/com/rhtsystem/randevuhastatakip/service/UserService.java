package com.rhtsystem.randevuhastatakip.service;

import com.rhtsystem.randevuhastatakip.model.*; // Appointment'ı da import edelim
import com.rhtsystem.randevuhastatakip.repository.*; // AppointmentRepository'yi de import edelim
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List; // EKLENDİ
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppointmentRepository appointmentRepository; // EKLENDİ

    // Rol sabitleri
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
                       PasswordEncoder passwordEncoder,
                       AppointmentRepository appointmentRepository) { // EKLENDİ
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
        this.appointmentRepository = appointmentRepository; // EKLENDİ
    }

    @Transactional
    public User registerNewUser(User user, String roleNamePrefixed, Optional<String> specializationOpt) throws Exception {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new Exception("Hata: Kullanıcı adı (" + user.getUsername() + ") zaten mevcut!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(roleNamePrefixed)
                .orElseThrow(() -> new RuntimeException("Hata: Rol bulunamadı. (" + roleNamePrefixed + ")"));
        roles.add(userRole);
        user.setRoles(roles);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

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
        return savedUser;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> findAllUsers() { // Adminin tüm kullanıcıları listelemesi için
        return userRepository.findAll();
    }

    public Optional<User> findUserById(Long id) { // Adminin kullanıcı silmesi için
        return userRepository.findById(id);
    }


    @Transactional
    public void deleteUserById(Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Kullanıcı bulunamadı ID: " + userId));

        // 1. Kullanıcıya ait Patient veya Doctor profilini sil
        Optional<Patient> patientOpt = patientRepository.findByUserId(userId);
        if (patientOpt.isPresent()) {
            // Hastanın randevularını iptal et veya sil (şimdilik iptal edelim)
            List<Appointment> patientAppointments = appointmentRepository.findByPatientOrderByAppointmentDateTimeDesc(patientOpt.get());
            for (Appointment app : patientAppointments) {
                if (app.getStatus() == AppointmentStatus.PENDING || app.getStatus() == AppointmentStatus.CONFIRMED) {
                    app.setStatus(AppointmentStatus.CANCELLED);
                    app.setDoctorNotes((app.getDoctorNotes() == null ? "" : app.getDoctorNotes() + "\n") + "Hasta sistemden silindiği için iptal edildi.");
                    appointmentRepository.save(app);
                }
                // Veya direkt appointmentRepository.delete(app); (daha riskli)
            }
            patientRepository.delete(patientOpt.get());
        }

        Optional<Doctor> doctorOpt = doctorRepository.findByUserId(userId);
        if (doctorOpt.isPresent()) {
            // Doktorun randevularını iptal et veya sil (şimdilik iptal edelim)
            List<Appointment> doctorAppointments = appointmentRepository.findByDoctorOrderByAppointmentDateTimeAsc(doctorOpt.get());
            for (Appointment app : doctorAppointments) {
                 if (app.getStatus() == AppointmentStatus.PENDING || app.getStatus() == AppointmentStatus.CONFIRMED) {
                    app.setStatus(AppointmentStatus.CANCELLED);
                    app.setDoctorNotes((app.getDoctorNotes() == null ? "" : app.getDoctorNotes() + "\n") + "Doktor sistemden silindiği için iptal edildi.");
                    appointmentRepository.save(app);
                }
            }
            doctorRepository.delete(doctorOpt.get());
        }

        // 2. user_roles tablosundaki ilişkileri temizle (JPA bunu @ManyToMany ile otomatik yapabilir, ama emin olalım)
        // Genellikle User silindiğinde @JoinTable ilişkisi de silinir.
        // Eğer sorun olursa: user.getRoles().clear(); userRepository.save(user); ardından silme.

        // 3. User'ı sil
        userRepository.delete(user);
    }
}