package com.rhtsystem.randevuhastatakip.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
public class AppointmentNoteDto {
    // @NotBlank(message = "Not boş olamaz.")
    private String notes;
}