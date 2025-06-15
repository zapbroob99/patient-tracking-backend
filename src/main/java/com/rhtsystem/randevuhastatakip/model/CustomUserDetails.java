package com.rhtsystem.randevuhastatakip.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User; // Spring'in User sınıfını kullanacağız

import java.util.Collection;

public class CustomUserDetails extends User {
    private final Long id;
    private final String firstName;
    private final String lastName;

    public CustomUserDetails(Long id, String username, String password, String firstName, String lastName,
                             boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked,
                             Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public CustomUserDetails(com.rhtsystem.randevuhastatakip.model.User user, // Kendi User entity'miz
                             Collection<? extends GrantedAuthority> authorities) {
        super(user.getUsername(), user.getPassword(), user.isEnabled(), true, true, true, authorities);
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
    }


    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}