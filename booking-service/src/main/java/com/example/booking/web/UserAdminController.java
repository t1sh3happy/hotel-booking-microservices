package com.example.booking.web;

import com.example.booking.model.User;
import com.example.booking.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@PreAuthorize("hasAuthority('SCOPE_ADMIN')")
public class UserAdminController {
    private final UserRepository userRepository;

    public UserAdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody Map<String, Object> req) {
        String username = (String) req.get("username");
        String password = (String) req.get("password");
        boolean admin = req.getOrDefault("admin", false) instanceof Boolean b && b;

        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        u.setRole(admin ? "ADMIN" : "USER");

        User saved = userRepository.save(u);
        return ResponseEntity.ok(saved);
    }

    @PatchMapping
    public ResponseEntity<User> updateUser(@RequestBody Map<String, Object> req) {
        Long userId = ((Number) req.get("userId")).longValue();

        return userRepository.findById(userId)
                .map(user -> {
                    if (req.containsKey("username")) {
                        user.setUsername((String) req.get("username"));
                    }
                    if (req.containsKey("password")) {
                        user.setPasswordHash(BCrypt.hashpw((String) req.get("password"), BCrypt.gensalt()));
                    }
                    if (req.containsKey("admin")) {
                        boolean admin = req.get("admin") instanceof Boolean b && b;
                        user.setRole(admin ? "ADMIN" : "USER");
                    }
                    return ResponseEntity.ok(userRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteUser(@RequestParam Long userId) {
        userRepository.deleteById(userId);
        return ResponseEntity.ok().build();
    }

    // Старые методы можно оставить для дополнительного функционала
    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        return ResponseEntity.ok(Map.of("users", userRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable Long id) {
        return userRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User u) {
        return userRepository.findById(id)
                .map(ex -> { u.setId(id); return ResponseEntity.ok(userRepository.save(u)); })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}