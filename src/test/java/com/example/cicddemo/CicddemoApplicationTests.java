package com.example.cicddemo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
class CicddemoApplicationTests {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
	}

	@Autowired
	private com.example.cicddemo.user.UserRepository userRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void shouldConnectToTestcontainersAndSaveUser() {
		// Save a user to the actual PostgreSQL testcontainer database
		com.example.cicddemo.user.User user = new com.example.cicddemo.user.User("Integration Test", "integration@example.com");
		com.example.cicddemo.user.User savedUser = userRepository.save(user);
		
		org.junit.jupiter.api.Assertions.assertNotNull(savedUser.getId());
		
		// Retrieve it
		java.util.Optional<com.example.cicddemo.user.User> retrievedUser = userRepository.findById(savedUser.getId());
		org.junit.jupiter.api.Assertions.assertTrue(retrievedUser.isPresent());
		org.junit.jupiter.api.Assertions.assertEquals("integration@example.com", retrievedUser.get().getEmail());
	}

}
