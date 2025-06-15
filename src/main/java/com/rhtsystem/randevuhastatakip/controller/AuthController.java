package com.rhtsystem.randevuhastatakip.controller;

import com.rhtsystem.randevuhastatakip.dto.UserRegistrationDto;
import com.rhtsystem.randevuhastatakip.model.User;
import com.rhtsystem.randevuhastatakip.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; // Validasyon için
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
// import jakarta.validation.Valid; // Validasyon için

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Login sayfasını göstermek için (Spring Security varsayılanını kullanıyorsak bu gerekmeyebilir,
    // ama özel bir login sayfamız olursa diye)
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // login.html (henüz oluşturmadık, varsayılanı kullanıyoruz)
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        // Form için boş bir DTO nesnesi ekleyelim
        model.addAttribute("userDto", new UserRegistrationDto());
        return "register"; // register.html
    }

    @PostMapping("/register")
    public String processRegistration(
            // @Valid // DTO'daki validasyonları aktif etmek için
            @ModelAttribute("userDto") UserRegistrationDto userDto,
            BindingResult result, // Validasyon sonuçları için
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validasyon hataları varsa formu tekrar göster
        // if (result.hasErrors()) {
        //     model.addAttribute("userDto", userDto); // Hatalı verilerle formu doldur
        //     return "register";
        // }

        // Şifre ve şifre teyidini kontrol et
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            // result.rejectValue("confirmPassword", "userDto.confirmPassword", "Şifreler uyuşmuyor!");
            model.addAttribute("userDto", userDto);
            model.addAttribute("passwordError", "Şifreler uyuşmuyor!");
            return "register";
        }

        // Doktor rolü seçilmişse uzmanlık alanı zorunlu mu kontrol et
        if (UserService.ROLE_DOKTOR.equals("ROLE_" + userDto.getRole().toUpperCase())) {
            if (userDto.getSpecialization() == null || userDto.getSpecialization().trim().isEmpty()) {
                model.addAttribute("userDto", userDto);
                model.addAttribute("specializationError", "Doktor için uzmanlık alanı zorunludur!");
                return "register";
            }
        }


        try {
            User newUser = new User();
            newUser.setUsername(userDto.getUsername());
            newUser.setPassword(userDto.getPassword()); // Şifre serviste hash'lenecek
            newUser.setFirstName(userDto.getFirstName());
            newUser.setLastName(userDto.getLastName());

            String roleName = "ROLE_" + userDto.getRole().toUpperCase(); // ROLE_HASTA veya ROLE_DOKTOR
            Optional<String> specialization = UserService.ROLE_DOKTOR.equals(roleName) ?
                                              Optional.ofNullable(userDto.getSpecialization()) :
                                              Optional.empty();

            userService.registerNewUser(newUser, roleName, specialization);

            redirectAttributes.addFlashAttribute("successMessage", "Kayıt başarılı! Lütfen giriş yapınız.");
            return "redirect:/login?registered"; // Başarılı kayıt sonrası login sayfasına yönlendir

        } catch (IllegalArgumentException e) { // Örneğin specialization zorunlu hatası
             model.addAttribute("userDto", userDto);
             model.addAttribute("registrationError", e.getMessage());
             return "register";
        } catch (Exception e) {
            // Kullanıcı adı zaten alınmış gibi hatalar
            model.addAttribute("userDto", userDto);
            model.addAttribute("registrationError", e.getMessage());
            // result.rejectValue("username", "userDto.username", e.getMessage());
            return "register";
        }
    }
}