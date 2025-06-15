package com.rhtsystem.randevuhastatakip.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
public class Doctor {

    @Id
    private Long id; // User ID'si ile aynı olacak

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column(nullable = false)
    private String specialization; // Uzmanlık alanı (örn: Kardiyoloji, Dahiliye)

    // Doktora özel diğer bilgiler buraya eklenebilir
    // private String roomNumber;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Appointment> appointments;

    public Doctor(User user, String specialization) {
        this.user = user;
        this.id = user.getId();
        this.specialization = specialization;
    }
}