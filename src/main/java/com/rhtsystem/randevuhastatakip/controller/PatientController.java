package com.rhtsystem.randevuhastatakip.controller;

import com.rhtsystem.randevuhastatakip.dto.AppointmentRequestDto;
import com.rhtsystem.randevuhastatakip.dto.DoctorDto;
import com.rhtsystem.randevuhastatakip.model.Appointment;
import com.rhtsystem.randevuhastatakip.model.Doctor;
import com.rhtsystem.randevuhastatakip.model.Patient;
import com.rhtsystem.randevuhastatakip.service.AppointmentService;
import com.rhtsystem.randevuhastatakip.service.UserService; // Rol sabitleri için
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize; // Metod seviyesinde yetkilendirme
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
// import jakarta.validation.Valid;


@Controller
@RequestMapping("/patient") // Bu controller'daki tüm mapping'ler /patient ile başlayacak
@PreAuthorize("hasRole('" + UserService.ROLE_HASTA + "')") // Bu controller'a sadece ROLE_HASTA erişebilir
public class PatientController {

    private final AppointmentService appointmentService;

    @Autowired
    public PatientController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/dashboard")
    public String patientDashboard(Model model) {
        // Yaklaşan randevuları listele
        List<Appointment> upcomingAppointments = appointmentService.getMyUpcomingAppointmentsAsPatient();
        model.addAttribute("upcomingAppointments", upcomingAppointments);
        // İleride başka bilgiler de eklenebilir
        return "patient/dashboard"; // patient/dashboard.html
    }

    @GetMapping("/appointments/new")
    public String showCreateAppointmentForm(Model model, @RequestParam(required = false) String specialization) {
        List<Doctor> doctors;
        if (specialization != null && !specialization.trim().isEmpty()) {
            doctors = appointmentService.findDoctorsBySpecialization(specialization);
        } else {
            doctors = appointmentService.getAllDoctors();
        }

        List<DoctorDto> doctorDtos = doctors.stream()
                .map(doc -> new DoctorDto(doc.getUser().getId(), doc.getUser().getFirstName(), doc.getUser().getLastName(), doc.getSpecialization()))
                .collect(Collectors.toList());

        // Doktorların uzmanlık alanlarını (tekrarsız) alalım
        List<String> specializations = appointmentService.getAllDoctors().stream()
                                           .map(Doctor::getSpecialization)
                                           .distinct()
                                           .sorted()
                                           .collect(Collectors.toList());

        model.addAttribute("appointmentRequest", new AppointmentRequestDto());
        model.addAttribute("doctors", doctorDtos);
        model.addAttribute("specializations", specializations); // Filtreleme için
        model.addAttribute("selectedSpecialization", specialization);

        // Minumum tarih olarak bugünü set edelim (datetime-local için)
        model.addAttribute("minDateTime", LocalDate.now().toString() + "T00:00");


        return "patient/create-appointment"; // patient/create-appointment.html
    }

    @PostMapping("/appointments/create")
    public String createAppointment(
            // @Valid
            @ModelAttribute("appointmentRequest") AppointmentRequestDto appointmentRequest,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        // if (result.hasErrors()) {
        //     // Hata varsa formu tekrar yükle, doktor listesini ve uzmanlıkları tekrar gönder
        //     List<Doctor> doctors = appointmentService.getAllDoctors();
        //     List<DoctorDto> doctorDtos = doctors.stream()
        //             .map(doc -> new DoctorDto(doc.getUser().getId(), doc.getUser().getFirstName(), doc.getUser().getLastName(), doc.getSpecialization()))
        //             .collect(Collectors.toList());
        //     List<String> specializations = doctors.stream().map(Doctor::getSpecialization).distinct().sorted().collect(Collectors.toList());
        //
        //     model.addAttribute("doctors", doctorDtos);
        //     model.addAttribute("specializations", specializations);
        //     model.addAttribute("minDateTime", LocalDate.now().toString() + "T00:00");
        //     return "patient/create-appointment";
        // }

        try {
            appointmentService.createAppointment(appointmentRequest.getDoctorId(), appointmentRequest.getAppointmentDateTime());
            redirectAttributes.addFlashAttribute("successMessage", "Randevunuz başarıyla oluşturuldu.");
            return "redirect:/patient/appointments/my"; // Başarılı randevu sonrası randevularım sayfasına
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Randevu oluşturulamadı: " + e.getMessage());
            // Hata durumunda formu ve doktor listesini tekrar yüklemek için:
            // Bu kısmı yukarıdaki if bloğuna benzer şekilde doldurabiliriz veya direkt anasayfaya yönlendirebiliriz.
            // Şimdilik basitçe create formuna geri yönlendirelim, ama doktor listesi vb. eksik kalacak.
            // Daha iyi bir UX için, hata mesajıyla birlikte formu tekrar doğru şekilde göstermek gerekir.
            // return "redirect:/patient/appointments/new?error=" + e.getMessage(); // Bu şekilde de hata mesajı taşınabilir
             List<Doctor> doctors = appointmentService.getAllDoctors();
             List<DoctorDto> doctorDtos = doctors.stream()
                     .map(doc -> new DoctorDto(doc.getUser().getId(), doc.getUser().getFirstName(), doc.getUser().getLastName(), doc.getSpecialization()))
                     .collect(Collectors.toList());
             List<String> specializations = doctors.stream().map(Doctor::getSpecialization).distinct().sorted().collect(Collectors.toList());

             model.addAttribute("appointmentRequest", appointmentRequest); // Kullanıcının girdiği verileri koru
             model.addAttribute("doctors", doctorDtos);
             model.addAttribute("specializations", specializations);
             model.addAttribute("minDateTime", LocalDate.now().toString() + "T00:00");
             model.addAttribute("errorMessage", "Randevu oluşturulamadı: " + e.getMessage()); // Hata mesajını model'e ekle
             return "patient/create-appointment"; // Hata ile formu tekrar göster
        }
    }

    @GetMapping("/appointments/my")
    public String listMyAppointments(Model model) {
        List<Appointment> upcomingAppointments = appointmentService.getMyUpcomingAppointmentsAsPatient();
        List<Appointment> pastAppointments = appointmentService.getMyPastAppointmentsAsPatient();

        model.addAttribute("upcomingAppointments", upcomingAppointments);
        model.addAttribute("pastAppointments", pastAppointments);
        return "patient/my-appointments"; // patient/my-appointments.html
    }

    // Randevu iptali için POST mapping
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