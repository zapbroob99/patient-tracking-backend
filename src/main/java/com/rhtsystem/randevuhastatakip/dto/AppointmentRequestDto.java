package com.rhtsystem.randevuhastatakip.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat; // Tarih formatlaması için

import java.time.LocalDateTime;
// import jakarta.validation.constraints.Future; // Gelecek bir tarih olmalı
// import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
public class AppointmentRequestDto {

    // @NotNull(message = "Doktor seçimi zorunludur.")
    private Long doctorId;

    // @NotNull(message = "Randevu tarihi ve saati zorunludur.")
    // @Future(message = "Randevu tarihi gelecek bir zaman olmalıdır.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) // HTML'den gelen datetime-local formatına uygun
    private LocalDateTime appointmentDateTime;

    // İleride hasta tarafından eklenecek bir not veya şikayet alanı da olabilir
    // private String patientNotes;
}