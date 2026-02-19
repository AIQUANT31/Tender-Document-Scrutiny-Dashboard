package com.example.services;

import com.example.dto.LoginRequest;
import com.example.dto.SignupRequest;
import com.example.entity.User;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthServices {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Map<String, Object> signup(SignupRequest signupRequest) {
        Map<String, Object> response = new HashMap<>();

        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            response.put("success", false);
            response.put("message", "Username already exists");
            return response;
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            response.put("success", false);
            response.put("message", "Email already exists");
            return response;
        }

        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));

        userRepository.save(user);

        response.put("success", true);
        response.put("message", "User registered successfully!");
        response.put("user", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail()
        ));

        return response;
    }

    public Map<String, Object> login(LoginRequest loginRequest) {
        Map<String, Object> response = new HashMap<>();

        Optional<User> userOptional = userRepository.findByUsername(loginRequest.getUsername());

        if (userOptional.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found");
            return response;
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            response.put("success", false);
            response.put("message", "Invalid password");
            return response;
        }

        response.put("success", true);
        response.put("message", "Login successful");
        response.put("user", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail()
        ));

        return response;
    }

    public Map<String, Object> getProfile(Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found");
            return response;
        }
        
        User user = userOptional.get();
        response.put("success", true);
        response.put("user", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail()
        ));
        
        return response;
    }

    public Map<String, Object> updateProfile(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        Long userId = Long.valueOf(request.get("userId").toString());
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found");
            return response;
        }
        
        User user = userOptional.get();
        
        if (request.containsKey("username")) {
            String newUsername = request.get("username").toString();
            if (!newUsername.equals(user.getUsername()) && userRepository.existsByUsername(newUsername)) {
                response.put("success", false);
                response.put("message", "Username already exists");
                return response;
            }
            user.setUsername(newUsername);
        }
        
        if (request.containsKey("email")) {
            String newEmail = request.get("email").toString();
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                response.put("success", false);
                response.put("message", "Email already exists");
                return response;
            }
            user.setEmail(newEmail);
        }
        
        if (request.containsKey("password")) {
            user.setPassword(passwordEncoder.encode(request.get("password").toString()));
        }
        
        userRepository.save(user);
        
        response.put("success", true);
        response.put("message", "Profile updated successfully!");
        response.put("user", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail()
        ));
        
        return response;
    }
}
