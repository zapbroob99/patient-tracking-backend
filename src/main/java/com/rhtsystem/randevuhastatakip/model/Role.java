package com.rhtsystem.randevuhastatakip.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // Örn: ROLE_HASTA, ROLE_DOKTOR

    // @ManyToMany(mappedBy = "roles")
    // private Set<User> users; // Eğer çift yönlü bir ilişki isterseniz, ama genellikle User üzerinden yönetilir.

    public Role(String name) {
        this.name = name;
    }
}