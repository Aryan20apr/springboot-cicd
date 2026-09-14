package com.example.cicddemo.user;

import com.example.cicddemo.user.dto.UserCreateRequest;
import com.example.cicddemo.user.dto.UserResponse;
import com.example.cicddemo.user.dto.UserUpdateRequest;
import com.example.cicddemo.user.exception.EmailAlreadyExistsException;
import com.example.cicddemo.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "Jane Doe", "jane@example.com", Instant.now());
    }

    @Test
    void getAllUsers_returnsAllMappedResponses() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserResponse> users = userService.getAllUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).email()).isEqualTo("jane@example.com");
    }

    @Test
    void getUserById_whenFound_returnsUserResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Jane Doe");
    }

    @Test
    void getUserById_whenNotFound_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createUser_whenEmailUnique_createsAndReturnsUserResponse() {
        UserCreateRequest request = new UserCreateRequest("John Doe", "john@example.com");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(new User(2L, "John Doe", "john@example.com", Instant.now()));

        UserResponse response = userService.createUser(request);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.email()).isEqualTo("john@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_whenEmailExists_throwsEmailAlreadyExistsException() {
        UserCreateRequest request = new UserCreateRequest("John Doe", "jane@example.com");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("jane@example.com");
    }

    @Test
    void updateUser_whenValid_updatesAndReturnsUserResponse() {
        UserUpdateRequest request = new UserUpdateRequest("Jane Updated", "jane.updated@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmailAndIdNot("jane.updated@example.com", 1L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(1L, request);

        assertThat(response.name()).isEqualTo("Jane Updated");
        assertThat(response.email()).isEqualTo("jane.updated@example.com");
    }

    @Test
    void updateUser_whenNotFound_throwsUserNotFoundException() {
        UserUpdateRequest request = new UserUpdateRequest("Jane Updated", "jane.updated@example.com");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateUser_whenEmailDuplicate_throwsEmailAlreadyExistsException() {
        UserUpdateRequest request = new UserUpdateRequest("Jane Updated", "other@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmailAndIdNot("other@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("other@example.com");
    }

    @Test
    void deleteUser_whenExists_deletesSuccessfully() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_whenNotFound_throwsUserNotFoundException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }
}
