package com.example.cicddemo.user;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser1 = userRepository.save(new User("Test User One", "user1@example.com"));
        testUser2 = userRepository.save(new User("Test User Two", "user2@example.com"));
        entityManager.flush();
    }

    @Test
    void findByEmail_whenEmailExists_returnsUser() {
        Optional<User> found = userRepository.findByEmail("user1@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User One");
        assertThat(found.get().getId()).isEqualTo(testUser1.getId());
    }

    @Test
    void findByEmail_whenEmailDoesNotExist_returnsEmpty() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_whenEmailExists_returnsTrue() {
        boolean exists = userRepository.existsByEmail("user1@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_whenEmailDoesNotExist_returnsFalse() {
        boolean exists = userRepository.existsByEmail("unknown@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmailAndIdNot_whenDifferentUserHasEmail_returnsTrue() {
        boolean exists = userRepository.existsByEmailAndIdNot("user2@example.com", testUser1.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmailAndIdNot_whenSameUserHasEmail_returnsFalse() {
        boolean exists = userRepository.existsByEmailAndIdNot("user1@example.com", testUser1.getId());

        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmailAndIdNot_whenEmailDoesNotExist_returnsFalse() {
        boolean exists = userRepository.existsByEmailAndIdNot("new@example.com", testUser1.getId());

        assertThat(exists).isFalse();
    }

    @Test
    void save_persistsUserWithGeneratedIdAndTimestamp() {
        User newUser = new User("New Person", "newperson@example.com");
        User savedUser = userRepository.save(newUser);
        entityManager.flush();

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("New Person");
        assertThat(savedUser.getEmail()).isEqualTo("newperson@example.com");
    }

    @Test
    void save_whenDuplicateEmail_throwsDataIntegrityViolationException() {
        User duplicateUser = new User("Duplicate Person", "user1@example.com");

        assertThatThrownBy(() -> {
            userRepository.save(duplicateUser);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteById_removesUserFromDatabase() {
        Long idToDelete = testUser1.getId();
        userRepository.deleteById(idToDelete);
        entityManager.flush();

        Optional<User> found = userRepository.findById(idToDelete);
        assertThat(found).isEmpty();
    }
}
