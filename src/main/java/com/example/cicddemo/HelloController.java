package com.example.cicddemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HelloController {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @GetMapping("/api/hello")
    public Map<String, Object> hello() {
        return Map.of(
                "message", "Hello from Spring Boot!",
                "profile", activeProfile,
                "timestamp", Instant.now().toString()
        );
    }
}