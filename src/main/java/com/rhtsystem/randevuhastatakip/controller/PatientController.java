package com.rhtsystem.randevuhastatakip.controller;

import com.rhtsystem.randevuhastatakip.dto.AppointmentRequestDto;
import com.rhtsystem.randevuhastatakip.dto.DoctorDto;
import com.rhtsystem.randevuhastatakip.model.Appointment;
import com.rhtsystem.randevuhastatakip.model.Doctor;
import com.rhtsystem.randevuhastatakip.service.AppointmentService;
import com.rhtsystem.randevuhastatakip.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat; // Tarih formatı için
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
// import jakarta.validation.Valid;


@Controller
@RequestMapping("/patient")
@PreAuthorize("hasRole('" + UserService.ROLE_HASTA + "')")
public class PatientController {

    private final AppointmentService appointmentService;

    @Autowired
    public PatientController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // ... (dashboard metodu aynı) ...
    @GetMapping("/dashboard")
    public String patientDashboard(Model model) {
        List<Appointment> upcomingAppointments = appointmentService.getMyUpcomingAppointmentsAsPatient();
        model.addAttribute("upcomingAppointments", upcomingAppointments);
        return "patient/dashboard";
    }


    @GetMapping("/appointments/new")
    public String showCreateAppointmentForm(Model model,
                                            @RequestParam(name = "doctorId", required = false) Long selectedDoctorId,
                                            @RequestParam(name = "appointmentDate", required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate) {

        List<Doctor> doctors = appointmentService.getAllDoctors();
        List<DoctorDto> doctorDtos = doctors.stream()
                .map(doc -> new DoctorDto(doc.getId(), doc.getUser().getFirstName(), doc.getUser().getLastName(), doc.getSpecialization()))
                .collect(Collectors.toList());

        model.addAttribute("doctors", doctorDtos);
        model.addAttribute("appointmentRequest", new AppointmentRequestDto()); // Form için boş DTO
        model.addAttribute("minDate", LocalDate.now().toString()); // Tarih seçimi için minimum gün

        List<LocalTime> availableSlots = Collections.emptyList();
        if (selectedDoctorId != null && appointmentDate != null) {
            try {
                availableSlots = appointmentService.getAvailableTimeSlots(selectedDoctorId, appointmentDate);
                model.addAttribute("selectedDoctorId", selectedDoctorId);
                model.addAttribute("selectedDate", appointmentDate.toString());
            } catch (IllegalArgumentException e) {
                model.addAttribute("slotError", e.getMessage());
            }
        }
        model.addAttribute("availableTimeSlots", availableSlots);

        return "patient/create-appointment-slots"; // Yeni HTML dosyası
    }

    @PostMapping("/appointments/create")
    public String createAppointment(
            // @Valid // DTO'da validasyonlar varsa
            @ModelAttribute("appointmentRequest") AppointmentRequestDto appointmentRequest,
            // BindingResult result, // Validasyon için
            @RequestParam("selectedTimeSlot") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime selectedTimeSlot,
            @RequestParam("appointmentDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
            Model model, // Hata durumunda modeli tekrar doldurmak için
            RedirectAttributes redirectAttributes) {

        // if (result.hasErrors()) {
        //     // Hata durumunda formu tekrar yükle (doktorları, tarihi, saatleri tekrar doldurarak)
        //     // Bu kısım daha detaylı doldurulabilir.
        //     return reloadFormWithError(model, appointmentRequest.getDoctorId(), appointmentDate, "Formda hatalar var.");
        // }

        if (appointmentRequest.getDoctorId() == null || selectedTimeSlot == null || appointmentDate == null) {
            return reloadFormWithError(model, appointmentRequest.getDoctorId(), appointmentDate, "Doktor, tarih ve saat seçimi zorunludur.");
        }

        LocalDateTime fullAppointmentDateTime = LocalDateTime.of(appointmentDate, selectedTimeSlot);

        try {
            appointmentService.createAppointment(appointmentRequest.getDoctorId(), fullAppointmentDateTime);
            redirectAttributes.addFlashAttribute("successMessage", "Randevunuz başarıyla oluşturuldu: " +
                    appointmentDate.toString() + " " + selectedTimeSlot.toString());
            return "redirect:/patient/appointments/my";
        } catch (Exception e) {
            // Hata durumunda formu ve seçili değerleri koruyarak tekrar göster
            return reloadFormWithError(model, appointmentRequest.getDoctorId(), appointmentDate, "Randevu oluşturulamadı: " + e.getMessage());
        }
    }
    
    // Hata durumunda formu yeniden yüklemek için yardımcı metod
    private String reloadFormWithError(Model model, Long doctorId, LocalDate appointmentDate, String errorMessage) {
        List<Doctor> doctors = appointmentService.getAllDoctors();
        List<DoctorDto> doctorDtos = doctors.stream()
                .map(doc -> new DoctorDto(doc.getId(), doc.getUser().getFirstName(), doc.getUser().getLastName(), doc.getSpecialization()))
                .collect(Collectors.toList());
        model.addAttribute("doctors", doctorDtos);
        model.addAttribute("appointmentRequest", new AppointmentRequestDto()); // Yeni boş DTO veya mevcut DTO
        model.addAttribute("minDate", LocalDate.now().toString());

        List<LocalTime> availableSlots = Collections.emptyList();
        if (doctorId != null && appointmentDate != null) {
            try {
                availableSlots = appointmentService.getAvailableTimeSlots(doctorId, appointmentDate);
                model.addAttribute("selectedDoctorId", doctorId);
                model.addAttribute("selectedDate", appointmentDate.toString());
            } catch (IllegalArgumentException e) {
                // Slot hatası olabilir, ama ana hata mesajını kullan
            }
        }
        model.addAttribute("availableTimeSlots", availableSlots);
        model.addAttribute("errorMessage", errorMessage); // Ana hata mesajı
        return "patient/create-appointment-slots";
    }


    // ... (listMyAppointments ve cancelAppointment metodları aynı) ...
     @GetMapping("/appointments/my")
    public String listMyAppointments(Model model) {
        List<Appointment> upcomingAppointments = appointmentService.getMyUpcomingAppointmentsAsPatient();
        List<Appointment> pastAppointments = appointmentService.getMyPastAppointmentsAsPatient();

        model.addAttribute("upcomingAppointments", upcomingAppointments);
        model.addAttribute("pastAppointments", pastAppointments);
        return "patient/my-appointments";
    }

    @PostMapping("/appointments/cancel/{appointmentId}")
    public String cancelAppointment(@PathVariable Long appointmentId, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.cancelAppointmentAsPatient(appointmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Randevu başarıyla iptal edildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Randevu iptal edilemedi: " + e.getMessage());
        }
        return "redirect:/patient/appointments/my";
    }
}