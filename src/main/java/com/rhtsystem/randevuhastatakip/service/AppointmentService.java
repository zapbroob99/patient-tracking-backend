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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository; // Giriş yapmış kullanıcıyı almak için

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

    @Transactional
    public Appointment createAppointment(Long doctorId, LocalDateTime appointmentDateTime) throws Exception {
        Patient currentPatient = getCurrentAuthenticatedPatient();
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with ID: " + doctorId));

        // Çakışma kontrolü (Aynı doktor, aynı saat)
        // Appointment entity'sindeki @UniqueConstraint bunu DB seviyesinde zaten engeller.
        // Ama yine de servis seviyesinde bir kontrol eklemek iyi bir pratiktir.
        List<AppointmentStatus> activeStatuses = List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
        boolean appointmentExists = appointmentRepository.existsByDoctorAndAppointmentDateTimeAndStatusIn(doctor, appointmentDateTime, activeStatuses);

        if (appointmentExists) {
            throw new Exception("Selected time slot is not available for this doctor.");
        }

        // TODO: Daha detaylı çakışma kontrolü eklenebilir. Örneğin, doktorun çalışma saatleri, randevu aralıkları vs.
        // TODO: Randevu saatinin geçmiş bir tarih olmaması kontrolü eklenebilir.
        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Appointment date and time cannot be in the past.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(currentPatient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setStatus(AppointmentStatus.PENDING); // Varsayılan durum

        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getMyAppointmentsAsPatient() {
        Patient currentPatient = getCurrentAuthenticatedPatient();
        return appointmentRepository.findByPatientOrderByAppointmentDateTimeDesc(currentPatient);
    }

    public List<Appointment> getMyUpcomingAppointmentsAsPatient() {
        Patient currentPatient = getCurrentAuthenticatedPatient();
        return appointmentRepository.findByPatientAndAppointmentDateTimeAfterOrderByAppointmentDateTimeAsc(currentPatient, LocalDateTime.now());
    }

    public List<Appointment> getMyPastAppointmentsAsPatient() {
        Patient currentPatient = getCurrentAuthenticatedPatient();
        return appointmentRepository.findByPatientAndAppointmentDateTimeBeforeOrderByAppointmentDateTimeDesc(currentPatient, LocalDateTime.now());
    }

    @Transactional
    public Appointment cancelAppointmentAsPatient(Long appointmentId) throws Exception {
        Patient currentPatient = getCurrentAuthenticatedPatient();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + appointmentId));

        if (!appointment.getPatient().getId().equals(currentPatient.getId())) {
            throw new IllegalAccessException("You are not authorized to cancel this appointment.");
        }

        // Sadece belirli durumlardaki randevular iptal edilebilir (örn: PENDING, CONFIRMED)
        // İptal süresi kısıtı da eklenebilir (örn: randevuya 24 saatten az kala iptal edilemez)
        if (appointment.getStatus() == AppointmentStatus.PENDING || appointment.getStatus() == AppointmentStatus.CONFIRMED) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            return appointmentRepository.save(appointment);
        } else {
            throw new IllegalStateException("This appointment cannot be cancelled. Status: " + appointment.getStatus());
        }
    }


    // --- Doktor İşlemleri ---

    public List<Appointment> getMyAppointmentsAsDoctor() {
        Doctor currentDoctor = getCurrentAuthenticatedDoctor();
        return appointmentRepository.findByDoctorOrderByAppointmentDateTimeAsc(currentDoctor);
    }

    public List<Appointment> getMyAppointmentsByStatusAsDoctor(AppointmentStatus status) {
        Doctor currentDoctor = getCurrentAuthenticatedDoctor();
        return appointmentRepository.findByDoctorAndStatusOrderByAppointmentDateTimeAsc(currentDoctor, status);
    }

    public List<Appointment> getMyPendingAppointmentsAsDoctor() {
        return getMyAppointmentsByStatusAsDoctor(AppointmentStatus.PENDING);
    }

    @Transactional
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

    @Transactional
    public Appointment rejectAppointment(Long appointmentId) throws Exception {
        Doctor currentDoctor = getCurrentAuthenticatedDoctor();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + appointmentId));

        if (!appointment.getDoctor().getId().equals(currentDoctor.getId())) {
            throw new IllegalAccessException("You are not authorized to manage this appointment.");
        }

        if (appointment.getStatus() == AppointmentStatus.PENDING) {
            appointment.setStatus(AppointmentStatus.REJECTED);
            // İsteğe bağlı olarak reddetme notu da eklenebilir.
            // appointment.setDoctorNotes("Doktor uygun değil.");
            return appointmentRepository.save(appointment);
        } else {
            throw new IllegalStateException("Only PENDING appointments can be rejected. Current status: " + appointment.getStatus());
        }
    }

    @Transactional
    public Appointment addOrUpdateDoctorNote(Long appointmentId, String notes) throws Exception {
        Doctor currentDoctor = getCurrentAuthenticatedDoctor();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + appointmentId));

        if (!appointment.getDoctor().getId().equals(currentDoctor.getId())) {
            throw new IllegalAccessException("You are not authorized to add notes to this appointment.");
        }

        // Notlar genellikle onaylanmış veya tamamlanmış randevulara eklenir.
        if (appointment.getStatus() == AppointmentStatus.CONFIRMED /* || appointment.getStatus() == AppointmentStatus.COMPLETED */) {
            appointment.setDoctorNotes(notes);
            return appointmentRepository.save(appointment);
        } else {
            throw new IllegalStateException("Notes can only be added to CONFIRMED or COMPLETED appointments. Current status: " + appointment.getStatus());
        }
    }

    // --- Genel Sorgular (Belki admin için veya listeleme için) ---
    public Optional<Appointment> getAppointmentById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId);
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public List<Doctor> findDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecialization(specialization);
    }
}