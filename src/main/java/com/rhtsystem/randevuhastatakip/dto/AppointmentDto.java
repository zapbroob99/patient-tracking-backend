package com.rhtsystem.randevuhastatakip.dto;

import com.rhtsystem.randevuhastatakip.model.AppointmentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AppointmentDto {
    private Long id;
    private LocalDateTime appointmentDateTime;
    private AppointmentStatus status;
    private String doctorNotes;

    // Hasta Bilgileri (Doktorun görmesi için)
    private Long patientId;
    private String patientFirstName;
    private String patientLastName;
    private String patientUsername; // TC veya ne kullanıyorsak

    // Doktor Bilgileri (Hastanın görmesi için veya genel listelemede)
    private Long doctorId;
    private String doctorFirstName;
    private String doctorLastName;
    private String doctorSpecialization;

    // Factory method veya MapStruct gibi bir mapper ile Appointment entity'sinden dönüşüm yapılabilir.
    // Şimdilik constructor veya manuel setleme ile ilerleyebiliriz.
}