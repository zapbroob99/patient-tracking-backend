package com.rhtsystem.randevuhastatakip.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private Set<String> roles;
    private String userType; // Hasta, Doktor, Admin
    private String specialization; // Doktorlar için
}