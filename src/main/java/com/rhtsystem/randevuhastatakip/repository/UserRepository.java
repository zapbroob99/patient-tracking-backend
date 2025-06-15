package com.rhtsystem.randevuhastatakip.repository;

import com.rhtsystem.randevuhastatakip.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username); // Kullanıcı adına göre bulma
    Boolean existsByUsername(String username);    // Verilen kullanıcı adı var mı?
    // İleride e-posta ile de arama gerekirse eklenebilir:
    // Optional<User> findByEmail(String email);
    // Boolean existsByEmail(String email);
}