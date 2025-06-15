package com.rhtsystem.randevuhastatakip.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String specialization;

    // Opsiyonel: Doktorun tam adını döndüren bir yardımcı metod
    public String getFullName() {
        return firstName + " " + lastName;
    }
}