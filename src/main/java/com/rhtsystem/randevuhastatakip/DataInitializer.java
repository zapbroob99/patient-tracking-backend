package com.rhtsystem.randevuhastatakip;

import com.rhtsystem.randevuhastatakip.model.User;
import com.rhtsystem.randevuhastatakip.service.RoleService;
import com.rhtsystem.randevuhastatakip.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("Initializing roles...");
        roleService.createRoleIfNotExists(UserService.ROLE_HASTA_PREFIXED);
        roleService.createRoleIfNotExists(UserService.ROLE_DOKTOR_PREFIXED);
        roleService.createRoleIfNotExists(UserService.ROLE_ADMIN_PREFIXED); // ADMIN ROLÜ EKLENDİ

        // Örnek bir admin kullanıcısı ekleyelim (eğer yoksa)
        if (userService.findByUsername("admin").isEmpty()) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword("admin123");
            adminUser.setFirstName("Sistem");
            adminUser.setLastName("Yöneticisi");
            try {
                // Admin rolü için specializationOpt boş gönderiliyor.
                userService.registerNewUser(adminUser, UserService.ROLE_ADMIN_PREFIXED, Optional.empty());
                System.out.println("Admin user created: admin / admin123");
            } catch (Exception e) {
                System.err.println("Error creating admin user: " + e.getMessage());
            }
        }

        // Örnek bir doktor kullanıcısı ekleyelim (eğer yoksa)
        if (userService.findByUsername("doktor_test").isEmpty()) {
            User doctorUser = new User();
            doctorUser.setUsername("doktor_test");
            doctorUser.setPassword("password123");
            doctorUser.setFirstName("Ayşe");
            doctorUser.setLastName("Yılmaz");
            try {
                userService.registerNewUser(doctorUser, UserService.ROLE_DOKTOR_PREFIXED, Optional.of("Kardiyoloji"));
                System.out.println("Test doctor user created: doktor_test / password123");
            } catch (Exception e) {
                System.err.println("Error creating test doctor: " + e.getMessage());
            }
        }

        if (userService.findByUsername("hasta_test").isEmpty()) {
            User patientUser = new User();
            patientUser.setUsername("hasta_test");
            patientUser.setPassword("password123");
            patientUser.setFirstName("Ali");
            patientUser.setLastName("Veli");
            try {
                userService.registerNewUser(patientUser, UserService.ROLE_HASTA_PREFIXED, Optional.empty());
                System.out.println("Test patient user created: hasta_test / password123");
            } catch (Exception e) {
                System.err.println("Error creating test patient: " + e.getMessage());
            }
        }
        System.out.println("Data initialization complete.");
    }
}