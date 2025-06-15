package com.rhtsystem.randevuhastatakip.controller;

import com.rhtsystem.randevuhastatakip.dto.AppointmentDto;
import com.rhtsystem.randevuhastatakip.dto.AppointmentNoteDto;
import com.rhtsystem.randevuhastatakip.model.Appointment;
import com.rhtsystem.randevuhastatakip.model.AppointmentStatus;
import com.rhtsystem.randevuhastatakip.service.AppointmentService;
import com.rhtsystem.randevuhastatakip.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
// import jakarta.validation.Valid;

@Controller
@RequestMapping("/doctor")
@PreAuthorize("hasRole('" + UserService.ROLE_DOKTOR + "')") // UserService.ROLE_DOKTOR = "ROLE_DOKTOR" olmalı
public class DoctorController {

    private final AppointmentService appointmentService;

    @Autowired
    public DoctorController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    private AppointmentDto convertToDto(Appointment app) {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(app.getId());
        dto.setAppointmentDateTime(app.getAppointmentDateTime());
        dto.setStatus(app.getStatus());
        dto.setDoctorNotes(app.getDoctorNotes());

        if (app.getPatient() != null && app.getPatient().getUser() != null) {
            dto.setPatientId(app.getPatient().getId());
            dto.setPatientFirstName(app.getPatient().getUser().getFirstName());
            dto.setPatientLastName(app.getPatient().getUser().getLastName());
            dto.setPatientUsername(app.getPatient().getUser().getUsername());
        }

        if (app.getDoctor() != null && app.getDoctor().getUser() != null) {
            dto.setDoctorId(app.getDoctor().getId());
            dto.setDoctorFirstName(app.getDoctor().getUser().getFirstName());
            dto.setDoctorLastName(app.getDoctor().getUser().getLastName());
            dto.setDoctorSpecialization(app.getDoctor().getSpecialization());
        }
        return dto;
    }

    @GetMapping("/dashboard")
    public String doctorDashboard(Model model) {
        List<Appointment> pendingAppointments = appointmentService.getMyPendingAppointmentsAsDoctor();
        List<AppointmentDto> pendingAppointmentDtos = pendingAppointments.stream()
                                                            .map(this::convertToDto)
                                                            .collect(Collectors.toList());
        model.addAttribute("pendingAppointments", pendingAppointmentDtos);
        // İleride onaylanmış, bugünkü randevular vb. de eklenebilir.
        return "doctor/dashboard"; // doctor/dashboard.html
    }

    @GetMapping("/appointments")
    public String listDoctorAppointments(@RequestParam(name = "status", required = false) String statusFilter, Model model) {
        List<Appointment> appointments;
        if (statusFilter != null && !statusFilter.isEmpty()) {
            try {
                AppointmentStatus status = AppointmentStatus.valueOf(statusFilter.toUpperCase());
                appointments = appointmentService.getMyAppointmentsByStatusAsDoctor(status);
                model.addAttribute("selectedStatus", status.name());
            } catch (IllegalArgumentException e) {
                // Geçersiz status değeri gelirse tümünü göster
                appointments = appointmentService.getMyAppointmentsAsDoctor();
                model.addAttribute("filterError", "Geçersiz durum filtresi.");
            }
        } else {
            appointments = appointmentService.getMyAppointmentsAsDoctor();
        }

        List<AppointmentDto> appointmentDtos = appointments.stream()
                                                    .map(this::convertToDto)
                                                    .collect(Collectors.toList());

        model.addAttribute("appointments", appointmentDtos);
        model.addAttribute("allStatuses", AppointmentStatus.values()); // Filtre için tüm durumları gönder
        return "doctor/appointments"; // doctor/appointments.html
    }

    @PostMapping("/appointments/confirm/{appointmentId}")
    public String confirmAppointment(@PathVariable Long appointmentId, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.confirmAppointment(appointmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Randevu başarıyla onaylandı.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Randevu onaylanamadı: " + e.getMessage());
        }
        return "redirect:/doctor/appointments"; // Veya dashboard'a
    }

    @PostMapping("/appointments/reject/{appointmentId}")
    public String rejectAppointment(@PathVariable Long appointmentId, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.rejectAppointment(appointmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Randevu başarıyla reddedildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Randevu reddedilemedi: " + e.getMessage());
        }
        return "redirect:/doctor/appointments";
    }

    // Randevu detayı ve not ekleme/görüntüleme sayfası
    @GetMapping("/appointments/details/{appointmentId}")
    public String appointmentDetails(@PathVariable Long appointmentId, Model model) {
        Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            // Hata mesajı ile yönlendirme
            return "redirect:/doctor/appointments?error=notfound";
        }
        Appointment appointment = appointmentOpt.get();
        // Güvenlik kontrolü: Bu randevu gerçekten bu doktora mı ait?
        // AppointmentService içinde bu kontroller zaten var, ama burada da eklenebilir.

        model.addAttribute("appointment", convertToDto(appointment));
        model.addAttribute("noteDto", new AppointmentNoteDto()); // Not formu için boş DTO
        return "doctor/appointment-details"; // doctor/appointment-details.html
    }


    @PostMapping("/appointments/notes/add/{appointmentId}")
    public String addNoteToAppointment(
            @PathVariable Long appointmentId,
            // @Valid
            @ModelAttribute("noteDto") AppointmentNoteDto noteDto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) { // model'i tekrar formu göstermek için ekledik

        // if (result.hasErrors()) {
        //     // Hata varsa detay sayfasını notDto ve appointment ile tekrar yükle
        //     Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(appointmentId);
        //     if (appointmentOpt.isPresent()) {
        //         model.addAttribute("appointment", convertToDto(appointmentOpt.get()));
        //         model.addAttribute("noteDto", noteDto); // Hatalı DTO'yu geri gönder
        //         return "doctor/appointment-details";
        //     } else {
        //         return "redirect:/doctor/appointments?error=notfound";
        //     }
        // }

        try {
            appointmentService.addOrUpdateDoctorNote(appointmentId, noteDto.getNotes());
            redirectAttributes.addFlashAttribute("successMessage", "Not başarıyla eklendi/güncellendi.");
            return "redirect:/doctor/appointments/details/" + appointmentId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Not eklenemedi: " + e.getMessage());
             // Hata durumunda detay sayfasını tekrar yükleyelim
             Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(appointmentId);
             if (appointmentOpt.isPresent()) {
                 model.addAttribute("appointment", convertToDto(appointmentOpt.get()));
                 model.addAttribute("noteDto", noteDto); // Kullanıcının girdiği notu koru
                 model.addAttribute("errorMessage", "Not eklenemedi: " + e.getMessage());
                 return "doctor/appointment-details";
             } else {
                 return "redirect:/doctor/appointments?error=notfound";
             }
        }
    }
}