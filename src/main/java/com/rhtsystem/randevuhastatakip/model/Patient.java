package com.rhtsystem.randevuhastatakip.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
public class Patient {

    @Id
    private Long id; // User ID'si ile aynı olacak

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Bu anotasyon, id alanının user ilişkisi üzerinden maplenmesini sağlar
    @JoinColumn(name = "id") // user_id yerine id olarak adlandırdık, çünkü Patient'ın primary key'i aynı zamanda FK.
    private User user;

    // Hastaya özel diğer bilgiler buraya eklenebilir (örn: adres, telefon vb. User'da yoksa)
    // private String address;
    // private String phoneNumber;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Appointment> appointments;

    public Patient(User user) {
        this.user = user;
        this.id = user.getId(); // User ID'sini Patient ID'si olarak ayarla
    }
}