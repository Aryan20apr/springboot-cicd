package com.example.cicddemo.user;

import com.example.cicddemo.user.dto.UserCreateRequest;
import com.example.cicddemo.user.dto.UserResponse;
import com.example.cicddemo.user.dto.UserUpdateRequest;
import com.example.cicddemo.user.exception.EmailAlreadyExistsException;
import com.example.cicddemo.user.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getAllUsers_shouldReturnUserListAndStatus200() throws Exception {
        UserResponse response = new UserResponse(1L, "Alice", "alice@example.com", Instant.now());
        when(userService.getAllUsers()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].email").value("alice@example.com"));

        verify(userService).getAllUsers();
    }

    @Test
    void getUserById_whenUserExists_shouldReturnUserAndStatus200() throws Exception {
        UserResponse response = new UserResponse(1L, "Alice", "alice@example.com", Instant.now());
        when(userService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        verify(userService).getUserById(1L);
    }

    @Test
    void getUserById_whenUserDoesNotExist_shouldReturnStatus404() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));

        verify(userService).getUserById(99L);
    }

    @Test
    void createUser_whenPayloadValid_shouldReturnStatus201AndCreatedUser() throws Exception {
        UserResponse createdResponse = new UserResponse(1L, "Bob", "bob@example.com", Instant.now());
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(createdResponse);

        String json = """
                {
                    "name": "Bob",
                    "email": "bob@example.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Bob"))
                .andExpect(jsonPath("$.email").value("bob@example.com"));

        verify(userService).createUser(any(UserCreateRequest.class));
    }

    @Test
    void createUser_whenNameIsBlank_shouldReturnStatus400AndFieldError() throws Exception {
        String json = """
                {
                    "name": "",
                    "email": "valid@example.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void createUser_whenEmailIsInvalid_shouldReturnStatus400AndFieldError() throws Exception {
        String json = """
                {
                    "name": "Valid Name",
                    "email": "invalid-email-format"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void createUser_whenEmailAlreadyExists_shouldReturnStatus409() throws Exception {
        when(userService.createUser(any(UserCreateRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("existing@example.com"));

        String json = """
                {
                    "name": "Existing User",
                    "email": "existing@example.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email is already registered: existing@example.com"));
    }

    @Test
    void updateUser_whenPayloadValid_shouldReturnStatus200AndUpdatedUser() throws Exception {
        UserResponse updatedResponse = new UserResponse(1L, "Bob Updated", "bob.updated@example.com", Instant.now());
        when(userService.updateUser(eq(1L), any(UserUpdateRequest.class))).thenReturn(updatedResponse);

        String json = """
                {
                    "name": "Bob Updated",
                    "email": "bob.updated@example.com"
                }
                """;

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Bob Updated"))
                .andExpect(jsonPath("$.email").value("bob.updated@example.com"));

        verify(userService).updateUser(eq(1L), any(UserUpdateRequest.class));
    }

    @Test
    void updateUser_whenUserNotFound_shouldReturnStatus404() throws Exception {
        when(userService.updateUser(eq(99L), any(UserUpdateRequest.class)))
                .thenThrow(new UserNotFoundException(99L));

        String json = """
                {
                    "name": "Bob",
                    "email": "bob@example.com"
                }
                """;

        mockMvc.perform(put("/api/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    @Test
    void updateUser_whenEmailDuplicate_shouldReturnStatus409() throws Exception {
        when(userService.updateUser(eq(1L), any(UserUpdateRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("other@example.com"));

        String json = """
                {
                    "name": "Bob",
                    "email": "other@example.com"
                }
                """;

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deleteUser_whenSuccessful_shouldReturnStatus204() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    void deleteUser_whenNotFound_shouldReturnStatus404() throws Exception {
        doThrow(new UserNotFoundException(99L)).when(userService).deleteUser(99L);

        mockMvc.perform(delete("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));

        verify(userService).deleteUser(99L);
    }
}
