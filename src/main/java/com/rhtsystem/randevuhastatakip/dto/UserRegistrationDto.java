package com.rhtsystem.randevuhastatakip.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Gerekirse validasyon anotasyonları eklenebilir (javax.validation veya jakarta.validation)
// import jakarta.validation.constraints.NotEmpty;
// import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class UserRegistrationDto {

    // @NotEmpty(message = "Kullanıcı adı boş olamaz")
    private String username; // TC Kimlik No veya e-posta olabilir

    // @NotEmpty(message = "Şifre boş olamaz")
    // @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    private String password;

    private String confirmPassword; // Şifre teyidi için

    // @NotEmpty(message = "Ad boş olamaz")
    private String firstName;

    // @NotEmpty(message = "Soyad boş olamaz")
    private String lastName;

    // @NotEmpty(message = "Rol seçimi zorunludur")
    private String role; // "HASTA" veya "DOKTOR" gibi değerler alacak

    private String specialization; // Sadece doktor rolü için
}