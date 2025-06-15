package com.rhtsystem.randevuhastatakip.controller;

import com.rhtsystem.randevuhastatakip.dto.AppointmentDto;
import com.rhtsystem.randevuhastatakip.dto.DoctorRegistrationDto;
import com.rhtsystem.randevuhastatakip.dto.UserDto;
import com.rhtsystem.randevuhastatakip.model.Appointment;
import com.rhtsystem.randevuhastatakip.model.Role;
import com.rhtsystem.randevuhastatakip.model.User;
import com.rhtsystem.randevuhastatakip.repository.DoctorRepository;   // EKLENDİ
import com.rhtsystem.randevuhastatakip.repository.PatientRepository; // EKLENDİ
import com.rhtsystem.randevuhastatakip.service.AppointmentService;
import com.rhtsystem.randevuhastatakip.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('" + UserService.ROLE_ADMIN + "')")
public class AdminController {

    private final UserService userService;
    private final AppointmentService appointmentService; // EKLENDİ

    public static final List<String> AVAILABLE_SPECIALIZATIONS = Arrays.asList(
            "Acil Tıp", "Aile Hekimliği", "Anesteziyoloji ve Reanimasyon", "Beyin ve Sinir Cerrahisi",
            "Çocuk Cerrahisi", "Çocuk Sağlığı ve Hastalıkları", "Dahiliye (İç Hastalıkları)",
            "Dermatoloji (Cildiye)", "Enfeksiyon Hastalıkları", "Fiziksel Tıp ve Rehabilitasyon",
            "Genel Cerrahi", "Göğüs Cerrahisi", "Göğüs Hastalıkları", "Göz Hastalıkları",
            "Kadın Hastalıkları ve Doğum", "Kalp ve Damar Cerrahisi", "Kardiyoloji",
            "Kulak Burun Boğaz Hastalıkları (KBB)", "Nöroloji", "Ortopedi ve Travmatoloji",
            "Plastik, Rekonstrüktif ve Estetik Cerrahi", "Psikiyatri", "Radyoloji", "Üroloji"
    );

    @Autowired
    public AdminController(UserService userService, AppointmentService appointmentService) { // EKLENDİ
        this.userService = userService;
        this.appointmentService = appointmentService; // EKLENDİ
    }

    private UserDto convertUserToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEnabled(user.isEnabled());
        dto.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        // Varsa Patient veya Doctor özel bilgilerini de ekleyebiliriz.
        userService.findUserById(user.getId()).ifPresent(u -> {
            patientRepository.findByUserId(u.getId()).ifPresent(p -> dto.setUserType("Hasta"));
            doctorRepository.findByUserId(u.getId()).ifPresent(d -> {
                dto.setUserType("Doktor");
                dto.setSpecialization(d.getSpecialization());
            });
        });
         if(dto.getUserType() == null && user.getRoles().stream().anyMatch(r -> r.getName().equals(UserService.ROLE_ADMIN_PREFIXED))) {
            dto.setUserType("Admin");
        }
        return dto;
    }
     // AppointmentService'teki DTO'ya taşıyabiliriz veya burada da kullanabiliriz.
    private AppointmentDto convertAppointmentToDto(Appointment app) {
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
    public String adminDashboard(Model model) {
        model.addAttribute("message", "Admin Paneline Hoş Geldiniz!");
        // Admin paneli için istatistikler eklenebilir
        // model.addAttribute("totalUsers", userService.findAllUsers().size());
        // model.addAttribute("totalAppointments", appointmentService.getAllAppointments().size());
        return "admin/dashboard";
    }

    @GetMapping("/doctors/register")
    public String showDoctorRegistrationForm(Model model) {
        model.addAttribute("doctorDto", new DoctorRegistrationDto());
        model.addAttribute("specializations", AVAILABLE_SPECIALIZATIONS);
        return "admin/register-doctor";
    }

    @PostMapping("/doctors/register")
    public String registerDoctor(
            @ModelAttribute("doctorDto") DoctorRegistrationDto doctorDto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        // ... (önceki haliyle aynı)
        if (!doctorDto.getPassword().equals(doctorDto.getConfirmPassword())) {
            model.addAttribute("passwordError", "Şifreler uyuşmuyor!");
            model.addAttribute("specializations", AVAILABLE_SPECIALIZATIONS);
            model.addAttribute("doctorDto", doctorDto);
            return "admin/register-doctor";
        }
        if (doctorDto.getSpecialization() == null || doctorDto.getSpecialization().trim().isEmpty()) {
             model.addAttribute("specializationError", "Uzmanlık alanı seçimi zorunludur!");
             model.addAttribute("specializations", AVAILABLE_SPECIALIZATIONS);
             model.addAttribute("doctorDto", doctorDto);
             return "admin/register-doctor";
        }
        try {
            User newDoctorUser = new User();
            newDoctorUser.setUsername(doctorDto.getUsername());
            newDoctorUser.setPassword(doctorDto.getPassword());
            newDoctorUser.setFirstName(doctorDto.getFirstName());
            newDoctorUser.setLastName(doctorDto.getLastName());
            userService.registerNewUser(newDoctorUser, UserService.ROLE_DOKTOR_PREFIXED, Optional.of(doctorDto.getSpecialization()));
            redirectAttributes.addFlashAttribute("successMessage", "Doktor (" + doctorDto.getFirstName() + " " + doctorDto.getLastName() + ") başarıyla kaydedildi.");
            return "redirect:/admin/users"; // Doktor kaydedildikten sonra kullanıcı listesine
        } catch (Exception e) {
            model.addAttribute("registrationError", "Doktor kaydedilemedi: " + e.getMessage());
            model.addAttribute("specializations", AVAILABLE_SPECIALIZATIONS);
            model.addAttribute("doctorDto", doctorDto);
            return "admin/register-doctor";
        }
    }

    // Kullanıcıları Listeleme
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<UserDto> userDtos = userService.findAllUsers().stream()
                                    .map(this::convertUserToDtoSafe) // Güvenli DTO dönüşümü
                                    .collect(Collectors.toList());
        model.addAttribute("users", userDtos);
        return "admin/users"; // admin/users.html
    }

    // Kullanıcı Silme
    @PostMapping("/users/delete/{userId}")
    public String deleteUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        // Kendini silme kontrolü (opsiyonel ama iyi bir pratik)
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // if (auth.getName().equals(userService.findUserById(userId).map(User::getUsername).orElse(""))) {
        //    redirectAttributes.addFlashAttribute("errorMessage", "Kendinizi silemezsiniz!");
        //    return "redirect:/admin/users";
        // }
        try {
            String deletedUsername = userService.findUserById(userId).map(User::getUsername).orElse("Bilinmeyen Kullanıcı");
            userService.deleteUserById(userId);
            redirectAttributes.addFlashAttribute("successMessage", "Kullanıcı (" + deletedUsername + ") ve ilişkili kayıtları başarıyla silindi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Kullanıcı silinemedi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // Randevuları Listeleme
    @GetMapping("/appointments")
    public String listAllAppointments(Model model) {
        List<AppointmentDto> appointmentDtos = appointmentService.getAllAppointments().stream()
                                                .map(this::convertAppointmentToDto) // Yukarıdaki DTO dönüşüm metodu
                                                .collect(Collectors.toList());
        model.addAttribute("appointments", appointmentDtos);
        return "admin/appointments"; // admin/appointments.html
    }

    // Randevu Silme
    @PostMapping("/appointments/delete/{appointmentId}")
    public String deleteAppointment(@PathVariable Long appointmentId, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.deleteAppointmentById(appointmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Randevu (ID: " + appointmentId + ") başarıyla silindi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Randevu silinemedi: " + e.getMessage());
        }
        return "redirect:/admin/appointments";
    }
    
    // PatientRepository ve DoctorRepository'yi enjekte etmemiz gerekecek convertUserToDto için
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private DoctorRepository doctorRepository;

    private UserDto convertUserToDtoSafe(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEnabled(user.isEnabled());
        dto.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));

        // Null check ekleyerek PatientRepository ve DoctorRepository'nin null olmamasını sağla
        if (patientRepository != null) {
            patientRepository.findByUserId(user.getId()).ifPresent(p -> dto.setUserType("Hasta"));
        }
        if (doctorRepository != null) {
            doctorRepository.findByUserId(user.getId()).ifPresent(d -> {
                dto.setUserType("Doktor");
                dto.setSpecialization(d.getSpecialization());
            });
        }
        if(dto.getUserType() == null && user.getRoles().stream().anyMatch(r -> r.getName().equals(UserService.ROLE_ADMIN_PREFIXED))) {
            dto.setUserType("Admin");
        }
        return dto;
    }
}