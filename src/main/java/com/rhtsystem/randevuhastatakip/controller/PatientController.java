package com.rhtsystem.randevuhastatakip.controller;

import com.rhtsystem.randevuhastatakip.dto.AppointmentRequestDto;
import com.rhtsystem.randevuhastatakip.dto.DoctorDto;
import com.rhtsystem.randevuhastatakip.model.Appointment;
import com.rhtsystem.randevuhastatakip.model.Doctor;
import com.rhtsystem.randevuhastatakip.service.AppointmentService;
import com.rhtsystem.randevuhastatakip.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/patient")
@PreAuthorize("hasRole('" + UserService.ROLE_HASTA + "')")
public class PatientController {

    private final AppointmentService appointmentService;
    // AdminController'dan uzmanlık listesini alabiliriz veya burada da tanımlayabiliriz.
    // Şimdilik AppointmentService üzerinden tüm doktorların uzmanlıklarını alalım.
    // private static final List<String> AVAILABLE_SPECIALIZATIONS = AdminController.AVAILABLE_SPECIALIZATIONS;

    @Autowired
    public PatientController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/dashboard")
    public String patientDashboard(Model model) {
        List<Appointment> upcomingAppointments = appointmentService.getMyUpcomingAppointmentsAsPatient();
        model.addAttribute("upcomingAppointments", upcomingAppointments);
        return "patient/dashboard";
    }

    @GetMapping("/appointments/new")
    public String showCreateAppointmentSteps(Model model,
                                             @RequestParam(name = "step", defaultValue = "1") int step,
                                             @RequestParam(name = "specialization", required = false) String selectedSpecialization,
                                             @RequestParam(name = "doctorId", required = false) Long selectedDoctorId,
                                             @RequestParam(name = "appointmentDate", required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate) {

        // Adım 1: Uzmanlıkları listele
        List<String> allSpecializations = appointmentService.getAllDoctors().stream()
                                            .map(Doctor::getSpecialization)
                                            .distinct()
                                            .sorted()
                                            .collect(Collectors.toList());
        model.addAttribute("allSpecializations", allSpecializations);
        model.addAttribute("appointmentRequest", new AppointmentRequestDto()); // Form için
        model.addAttribute("currentStep", step);

        // Adım 2: Seçilen uzmanlığa göre doktorları listele
        if (selectedSpecialization != null && !selectedSpecialization.isEmpty()) {
            List<Doctor> doctorsInSpecialization = appointmentService.findDoctorsBySpecialization(selectedSpecialization);
            List<DoctorDto> doctorDtos = doctorsInSpecialization.stream()
                    .map(doc -> new DoctorDto(doc.getId(), doc.getUser().getFirstName(), doc.getUser().getLastName(), doc.getSpecialization()))
                    .collect(Collectors.toList());
            model.addAttribute("doctorsInSpecialization", doctorDtos);
            model.addAttribute("selectedSpecialization", selectedSpecialization);
        }

        // Adım 3: Tarih seçimi (selectedDoctorId varsa aktif olur)
        if (selectedDoctorId != null) {
            model.addAttribute("selectedDoctorId", selectedDoctorId);
            // Doktor adını da alıp model'e ekleyelim
            appointmentService.getAllDoctors().stream()
                .filter(doc -> doc.getId().equals(selectedDoctorId))
                .findFirst()
                .ifPresent(doc -> model.addAttribute("selectedDoctorName", doc.getUser().getFirstName() + " " + doc.getUser().getLastName()));
        }
        model.addAttribute("minDate", LocalDate.now().toString()); // Tarih seçimi için minimum gün

        // Adım 4: Saat slotlarını listele
        List<LocalTime> availableSlots = Collections.emptyList();
        if (selectedDoctorId != null && appointmentDate != null) {
            try {
                availableSlots = appointmentService.getAvailableTimeSlots(selectedDoctorId, appointmentDate);
                model.addAttribute("selectedDate", appointmentDate.toString());
            } catch (IllegalArgumentException e) {
                model.addAttribute("slotError", e.getMessage());
            }
        }
        model.addAttribute("availableTimeSlots", availableSlots);

        return "patient/create-appointment-steps"; // Yeni veya güncellenmiş HTML dosyası
    }


    @PostMapping("/appointments/create")
    public String createAppointment(
            // DTO'dan sadece doctorId'yi alacağız, tarih ve saat ayrı parametreler
            @RequestParam("doctorId") Long doctorId,
            @RequestParam("appointmentDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
            @RequestParam("selectedTimeSlot") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime selectedTimeSlot,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (doctorId == null || appointmentDate == null || selectedTimeSlot == null) {
            // Gerekli parametreler eksikse, formu uygun bir adımla tekrar göster.
            // Bu durum normalde formun doğru yönlendirmesiyle engellenmeli.
            redirectAttributes.addFlashAttribute("errorMessage", "Eksik bilgi: Doktor, tarih veya saat seçilmemiş.");
            return "redirect:/patient/appointments/new?step=1"; // Veya hatanın olduğu adıma
        }

        LocalDateTime fullAppointmentDateTime = LocalDateTime.of(appointmentDate, selectedTimeSlot);

        try {
            appointmentService.createAppointment(doctorId, fullAppointmentDateTime);
            redirectAttributes.addFlashAttribute("successMessage", "Randevunuz başarıyla oluşturuldu: " +
                    appointmentDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) + " saat " + selectedTimeSlot.toString());
            return "redirect:/patient/appointments/my";
        } catch (Exception e) {
            // Hata durumunda, kullanıcıyı formda kaldığı adıma yönlendirip hata mesajı göstermek daha iyi olur.
            // Şimdilik genel bir hata mesajı ile ilk adıma yönlendirelim.
            redirectAttributes.addFlashAttribute("errorMessage", "Randevu oluşturulamadı: " + e.getMessage());
            // Hata durumunda formu ve seçili değerleri koruyarak tekrar göstermek için:
            // (Bu kısım daha karmaşık hale gelebilir, şimdilik basit tutalım)
            return "redirect:/patient/appointments/new?step=3&doctorId=" + doctorId + "&appointmentDate=" + appointmentDate.toString();
        }
    }

    // Hata durumunda formu yeniden yüklemek için yardımcı metod (Opsiyonel, createAppointment içinde de yönetilebilir)
    // private String reloadFormWithStepsError(Model model, int step, String selectedSpecialization, Long selectedDoctorId, LocalDate appointmentDate, String errorMessage) {
    //     // Bu metod, showCreateAppointmentSteps metoduna benzer şekilde modeli doldurur
    //     // ve "patient/create-appointment-steps" template'ini döndürür.
    //     model.addAttribute("errorMessage", errorMessage);
    //     // ... diğer model attribute'ları ...
    //     return showCreateAppointmentSteps(model, step, selectedSpecialization, selectedDoctorId, appointmentDate);
    // }


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