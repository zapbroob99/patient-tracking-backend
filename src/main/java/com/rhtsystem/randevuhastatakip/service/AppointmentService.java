package com.rhtsystem.randevuhastatakip.service;

import com.rhtsystem.randevuhastatakip.model.*;
import com.rhtsystem.randevuhastatakip.repository.AppointmentRepository;
import com.rhtsystem.randevuhastatakip.repository.DoctorRepository;
import com.rhtsystem.randevuhastatakip.repository.PatientRepository;
import com.rhtsystem.randevuhastatakip.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ÖNEMLİ

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
    }

    // --- Yardımcı Metodlar ---
    // Bu metodlar genellikle @Transactional gerektirmez çünkü DB'ye yazmazlar
    // ve çağıran metod zaten bir transaction içinde olabilir.
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("No authenticated user found.");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found in database: " + username));
    }

    private Patient getCurrentAuthenticatedPatient() {
        User currentUser = getCurrentAuthenticatedUser();
        return patientRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user is not a patient or patient record not found."));
    }

    private Doctor getCurrentAuthenticatedDoctor() {
        User currentUser = getCurrentAuthenticatedUser();
        return doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user is not a doctor or doctor record not found."));
    }

    // --- Hasta İşlemleri ---

    @Transactional // Yazma işlemi
    public Appointment createAppointment(Long doctorId, LocalDateTime appointmentDateTime) throws Exception {
        Patient currentPatient = getCurrentAuthenticatedPatient();
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with ID: " + doctorId));

        List<AppointmentStatus> activeStatuses = List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
        boolean appointmentExists = appointmentRepository.existsByDoctorAndAppointmentDateTimeAndStatusIn(doctor, appointmentDateTime, activeStatuses);

        if (appointmentExists) {
            throw new Exception("Selected time slot is not available for this doctor.");
        }

        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Appointment date and time cannot be in the past.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(currentPatient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDateTime(appointmentDateTime);
        // appointment.setStatus(AppointmentStatus.PENDING); // @PrePersist bunu yapıyor

        return appointmentRepository.save(appointment);
    }

    @Transactional(readOnly = true) // Okuma işlemi
    public List<Appointment> getMyAppointmentsAsPatient() {
        Patient currentPatient = getCurrentAuthenticatedPatient();
        return appointmentRepository.findByPatientOrderByAppointmentDateTimeDesc(currentPatient);
    }

    @Transactional(readOnly = true) // Okuma işlemi
    public List<Appointment> getMyUpcomingAppointmentsAsPatient() {
        Patient currentPatient = getCurrentAuthenticatedPatient();
        return appointmentRepository.findByPatientAndAppointmentDateTimeAfterOrderByAppointmentDateTimeAsc(currentPatient, LocalDateTime.now());
    }

    @Transactional(readOnly = true) // Okuma işlemi (Hata burada çıkıyordu)
    public List<Appointment> getMyPastAppointmentsAsPatient() {
        Patient currentPatient = getCurrentAuthenticatedPatient();
        return appointmentRepository.findByPatientAndAppointmentDateTimeBeforeOrderByAppointmentDateTimeDesc(currentPatient, LocalDateTime.now());
    }

    @Transactional // Yazma işlemi
    public Appointment cancelAppointmentAsPatient(Long appointmentId) throws Exception {
        Patient currentPatient = getCurrentAuthenticatedPatient();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + appointmentId));

        if (!appointment.getPatient().getId().equals(currentPatient.getId())) {
            throw new IllegalAccessException("You are not authorized to cancel this appointment.");
        }

        if (appointment.getStatus() == AppointmentStatus.PENDING || appointment.getStatus() == AppointmentStatus.CONFIRMED) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            return appointmentRepository.save(appointment);
        } else {
            throw new IllegalStateException("This appointment cannot be cancelled. Status: " + appointment.getStatus());
        }
    }


    // --- Doktor İşlemleri ---

    @Transactional(readOnly = true) // Okuma işlemi
    public List<Appointment> getMyAppointmentsAsDoctor() {
        Doctor currentDoctor = getCurrentAuthenticatedDoctor();
        return appointmentRepository.findByDoctorOrderByAppointmentDateTimeAsc(currentDoctor);
    }

    @Transactional(readOnly = true) // Okuma işlemi
    public List<Appointment> getMyAppointmentsByStatusAsDoctor(AppointmentStatus status) {
        Doctor currentDoctor = getCurrentAuthenticatedDoctor();
        return appointmentRepository.findByDoctorAndStatusOrderByAppointmentDateTimeAsc(currentDoctor, status);
    }

    @Transactional(readOnly = true) // Okuma işlemi
    public List<Appointment> getMyPendingAppointmentsAsDoctor() {
        return getMyAppointmentsByStatusAsDoctor(AppointmentStatus.PENDING);
    }

    @Transactional // Yazma işlemi
    public Appointment confirmAppointment(Long appointmentId) throws Exception {
        Doctor currentDoctor = getCurrentAuthenticatedDoctor();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + appointmentId));

        if (!appointment.getDoctor().getId().equals(currentDoctor.getId())) {
            throw new IllegalAccessException("You are not authorized to manage this appointment.");
        }

        if (appointment.getStatus() == AppointmentStatus.PENDING) {
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            return appointmentRepository.save(appointment);
        } else {
            throw new IllegalStateException("Only PENDING appointments can be confirmed. Current status: " + appointment.getStatus());
        }
    }

    @Transactional // Yazma işlemi
    public Appointment rejectAppointment(Long appointmentId) throws Exception {
        Doctor currentDoctor = getCurrentAuthenticatedDoctor();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + appointmentId));

        if (!appointment.getDoctor().getId().equals(currentDoctor.getId())) {
            throw new IllegalAccessException("You are not authorized to manage this appointment.");
        }

        if (appointment.getStatus() == AppointmentStatus.PENDING) {
            appointment.setStatus(AppointmentStatus.REJECTED);
            return appointmentRepository.save(appointment);
        } else {
            throw new IllegalStateException("Only PENDING appointments can be rejected. Current status: " + appointment.getStatus());
        }
    }

    @Transactional // Yazma işlemi
    public Appointment addOrUpdateDoctorNote(Long appointmentId, String notes) throws Exception {
        Doctor currentDoctor = getCurrentAuthenticatedDoctor();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + appointmentId));

        if (!appointment.getDoctor().getId().equals(currentDoctor.getId())) {
            throw new IllegalAccessException("You are not authorized to add notes to this appointment.");
        }

        if (appointment.getStatus() == AppointmentStatus.CONFIRMED /* || appointment.getStatus() == AppointmentStatus.COMPLETED */) {
            appointment.setDoctorNotes(notes);
            return appointmentRepository.save(appointment);
        } else {
            throw new IllegalStateException("Notes can only be added to CONFIRMED or COMPLETED appointments. Current status: " + appointment.getStatus());
        }
    }

    // --- Genel Sorgular ---
    @Transactional(readOnly = true) // Okuma işlemi
    public Optional<Appointment> getAppointmentById(Long appointmentId) {
        // Bu metodun çağıranı, doktorun bu randevuya erişim yetkisi olup olmadığını kontrol etmeli.
        // Veya burada ek bir yetki kontrolü yapılabilir.
        return appointmentRepository.findById(appointmentId);
    }

    @Transactional(readOnly = true) // Okuma işlemi (genellikle LOB içermez ama alışkanlık)
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    @Transactional(readOnly = true) // Okuma işlemi
    public List<Doctor> findDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecialization(specialization);
    }
}