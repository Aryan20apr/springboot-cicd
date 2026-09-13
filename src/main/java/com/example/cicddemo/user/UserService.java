package com.example.cicddemo.user;

import com.example.cicddemo.user.dto.UserCreateRequest;
import com.example.cicddemo.user.dto.UserResponse;
import com.example.cicddemo.user.dto.UserUpdateRequest;
import com.example.cicddemo.user.exception.EmailAlreadyExistsException;
import com.example.cicddemo.user.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::fromEntity)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User(request.name(), request.email());
        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new EmailAlreadyExistsException(request.email());
        }

        user.setName(request.name());
        user.setEmail(request.email());

        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }
}
