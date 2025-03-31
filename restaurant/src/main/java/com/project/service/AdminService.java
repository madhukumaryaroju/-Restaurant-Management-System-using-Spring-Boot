package com.project.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.entity.Admin;
import com.project.repository.AdminRepository;

@Service
@Transactional
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;
    
    public Admin save(Admin admin) {
        return adminRepository.save(admin);
    }
    
    public List<Admin> fetchAll() {
        return adminRepository.findAll();
    }
    
    public Optional<Admin> findByEmail(String email) {
        return adminRepository.findByEmail(email);
    }
    
    public Optional<Admin> findById(int id) {
        return adminRepository.findById(id);
    }
    
    public void deleteById(int id) {
        adminRepository.deleteById(id);
    }
    
    public boolean existsByEmail(String email) {
        return adminRepository.existsByEmail(email);
    }
    
    public Admin updateAdmin(Admin admin) {
        if (!adminRepository.existsById(admin.getId())) {
            throw new RuntimeException("Admin not found with id: " + admin.getId());
        }
        return adminRepository.save(admin);
    }
    
    public boolean validateCredentials(String email, String password) {
        Optional<Admin> admin = adminRepository.findByEmail(email);
        return admin.isPresent() && admin.get().getPassword().equals(password);
    }
}