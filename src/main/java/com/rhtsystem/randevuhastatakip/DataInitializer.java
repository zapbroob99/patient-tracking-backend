package com.rhtsystem.randevuhastatakip;

import com.rhtsystem.randevuhastatakip.model.User;
import com.rhtsystem.randevuhastatakip.service.RoleService;
import com.rhtsystem.randevuhastatakip.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserService userService; // UserService'i direkt kullanmak yerine, RoleService içinde halletmek daha iyi olabilir.

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Initializing roles...");
        roleService.createRoleIfNotExists(UserService.ROLE_HASTA);
        roleService.createRoleIfNotExists(UserService.ROLE_DOKTOR);
        // roleService.createRoleIfNotExists(UserService.ROLE_ADMIN); // Gerekirse

        // Örnek bir doktor kullanıcısı ekleyelim (eğer yoksa)
        // Gerçek uygulamada bu kullanıcılar kayıt formu üzerinden eklenmeli
        if (userService.findByUsername("doktor_test").isEmpty()) {
            User doctorUser = new User();
            doctorUser.setUsername("doktor_test");
            doctorUser.setPassword("password123"); // Bu şifre UserService'de hash'lenecek
            doctorUser.setFirstName("Test");
            doctorUser.setLastName("Doktor");
            try {
                userService.registerNewUser(doctorUser, UserService.ROLE_DOKTOR, Optional.of("Genel Cerrahi"));
                System.out.println("Test doctor user created.");
            } catch (Exception e) {
                System.err.println("Error creating test doctor: " + e.getMessage());
            }
        }

        if (userService.findByUsername("hasta_test").isEmpty()) {
            User patientUser = new User();
            patientUser.setUsername("hasta_test");
            patientUser.setPassword("password123");
            patientUser.setFirstName("Test");
            patientUser.setLastName("Hasta");
            try {
                userService.registerNewUser(patientUser, UserService.ROLE_HASTA, Optional.empty());
                System.out.println("Test patient user created.");
            } catch (Exception e) {
                System.err.println("Error creating test patient: " + e.getMessage());
            }
        }
        System.out.println("Data initialization complete.");
    }
}