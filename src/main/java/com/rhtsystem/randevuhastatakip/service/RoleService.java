package com.rhtsystem.randevuhastatakip.service;

import com.rhtsystem.randevuhastatakip.model.Role;
import com.rhtsystem.randevuhastatakip.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role createRoleIfNotExists(String roleName) {
        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isPresent()) {
            return roleOpt.get();
        } else {
            Role newRole = new Role(roleName);
            return roleRepository.save(newRole);
        }
    }

    public Optional<Role> findByName(String roleName) {
        return roleRepository.findByName(roleName);
    }
}