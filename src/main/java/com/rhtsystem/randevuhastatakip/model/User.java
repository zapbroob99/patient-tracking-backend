package com.rhtsystem.randevuhastatakip.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// Spring Security için gerekli importlar ileride eklenecek
// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
// @Inheritance(strategy = InheritanceType.JOINED) // Eğer Hasta ve Doktor'u User'dan türeteceksek
public class User { // Şimdilik UserDetails implementasyonunu sonraya bırakalım, kafa karıştırmasın.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username; // TC Kimlik No veya e-posta olabilir

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private boolean enabled = true; // Kullanıcı aktif mi?

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST) // Rolleri kullanıcı ile birlikte kaydet
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // UserDetails metodlarını Spring Security'yi entegre ederken ekleyeceğiz.
    // Şimdilik temel kullanıcı bilgilerine odaklanalım.
}