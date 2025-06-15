package com.rhtsystem.randevuhastatakip.controller;

import com.rhtsystem.randevuhastatakip.dto.DoctorRegistrationDto;
import com.rhtsystem.randevuhastatakip.model.User;
import com.rhtsystem.randevuhastatakip.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
// import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
// @PreAuthorize metodu için UserService.ROLE_ADMIN ("ADMIN") kullanılacak
@PreAuthorize("hasRole('" + UserService.ROLE_ADMIN + "')")
public class AdminController {

    private final UserService userService;

    // Sabit uzmanlık alanları
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
    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("message", "Admin Paneline Hoş Geldiniz!");
        // Örnek: Sistemdeki toplam kullanıcı sayısını gösterme
        // model.addAttribute("totalUsers", userService.findAllUsers().size());
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
            // @Valid
            @ModelAttribute("doctorDto") DoctorRegistrationDto doctorDto,
            BindingResult result, // Validasyon aktif edilirse kullanılacak
            Model model,
            RedirectAttributes redirectAttributes) {

        // if (result.hasErrors()) { // Validasyon aktif edilirse
        //     model.addAttribute("specializations", AVAILABLE_SPECIALIZATIONS);
        //     return "admin/register-doctor";
        // }

        if (!doctorDto.getPassword().equals(doctorDto.getConfirmPassword())) {
            model.addAttribute("passwordError", "Şifreler uyuşmuyor!");
            model.addAttribute("specializations", AVAILABLE_SPECIALIZATIONS);
            model.addAttribute("doctorDto", doctorDto); // Hatalı DTO'yu geri gönder
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

            // Doktor kaydı için ROLE_DOKTOR_PREFIXED ve uzmanlık alanı gönderiyoruz
            userService.registerNewUser(newDoctorUser, UserService.ROLE_DOKTOR_PREFIXED, Optional.of(doctorDto.getSpecialization()));

            redirectAttributes.addFlashAttribute("successMessage", "Doktor (" + doctorDto.getFirstName() + " " + doctorDto.getLastName() + ") başarıyla kaydedildi.");
            return "redirect:/admin/dashboard"; // Veya doktor listeleme sayfasına yönlendirilebilir
        } catch (Exception e) {
            model.addAttribute("registrationError", "Doktor kaydedilemedi: " + e.getMessage());
            model.addAttribute("specializations", AVAILABLE_SPECIALIZATIONS);
            model.addAttribute("doctorDto", doctorDto); // Hatalı DTO'yu geri gönder
            return "admin/register-doctor";
        }
    }
}