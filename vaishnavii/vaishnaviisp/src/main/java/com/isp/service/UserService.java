package com.isp.service;

import com.isp.model.User;
import com.isp.repo.UserRepository;
import com.isp.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

/**
 * Service for user authentication and management.
 */
public class UserService {
    private final UserRepository repository;
    private final EmailService emailService;

    public UserService(UserRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    /**
     * Register a new user (admin or customer).
     */
    public User registerUser(String username, String email, String password, User.Role role) {
        // Check if username exists
        if (repository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (repository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        String userId = IdGenerator.generate();
        // In production, use BCrypt or similar
        String passwordHash = hashPassword(password);
        User user = new User(userId, username, email, passwordHash, role);
        repository.save(user);

        // Send welcome email
        String subject = "Welcome to ISP Management System";
        String body = role == User.Role.ADMIN
                ? "Welcome Admin! Your account has been created. Username: " + username
                : "Welcome Customer! Your account has been created. Start managing your devices and plans.";
        emailService.sendEmail(email, subject, body);

        System.out.println("User registered: " + user);
        return user;
    }

    /**
     * Authenticate user login (supports email or username).
     */
    public Optional<User> authenticate(String usernameOrEmail, String password) {
        // Try to find by username first
        Optional<User> userOpt = repository.findByUsername(usernameOrEmail);
        
        // If not found, try email
        if (userOpt.isEmpty()) {
            userOpt = repository.findByEmail(usernameOrEmail);
        }
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (verifyPassword(password, user.getPasswordHash())) {
                user.setLastLogin(LocalDateTime.now());
                repository.save(user); // Update last login
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(String id) {
        return repository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public Collection<User> listAll() {
        return repository.findAll();
    }

    public void updateUser(User user) {
        repository.save(user);
    }

    public void deleteUser(String id) {
        repository.delete(id);
    }

    /**
     * Reset user password by email.
     */
    public boolean resetPassword(String email, String newPassword) {
        Optional<User> userOpt = repository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String newHash = hashPassword(newPassword);
            user.setPasswordHash(newHash);
            repository.save(user);
            
            // Send confirmation email
            String subject = "Password Reset Successful - ISP Management";
            String body = String.format(
                "Hello %s,\n\n" +
                "Your password has been successfully reset.\n" +
                "You can now login with your new password.\n\n" +
                "If you did not make this change, please contact support immediately.\n\n" +
                "Best regards,\nISP Management Team",
                user.getUsername()
            );
            emailService.sendEmail(email, subject, body);
            
            return true;
        }
        return false;
    }

    // Simple password hashing (in production use BCrypt)
    private String hashPassword(String password) {
        return Integer.toHexString(password.hashCode());
    }

    private boolean verifyPassword(String password, String hash) {
        return hashPassword(password).equals(hash);
    }
}
