package com.example.cicddemo.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(UserDataInitializer.class);

    @Bean
    CommandLineRunner initUsers(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                log.info("Seeding sample users into H2 database...");
                userRepository.save(new User("Alice Smith", "alice@example.com"));
                userRepository.save(new User("Bob Jones", "bob@example.com"));
                log.info("Sample users seeded successfully.");
            }
        };
    }
}
