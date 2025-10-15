package org.filestorage.app.service;

import lombok.RequiredArgsConstructor;
import org.filestorage.app.dto.UserRequest;
import org.filestorage.app.dto.UserResponse;
import org.filestorage.app.exception.UserAlreadyExistException;
import org.filestorage.app.exception.InvalidCredentialException;
import org.filestorage.app.mapper.UserMapper;
import org.filestorage.app.model.User;
import org.filestorage.app.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse create(UserRequest userRequest) {
        if(userRepository.existsByName(userRequest.getUsername())) {
            throw new UserAlreadyExistException();
        }

        User user = userMapper.toEntity(userRequest);
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        user = userRepository.save(user);

        return userMapper.toResponse(user);
    }

    public UserResponse getUser(UserRequest userRequest) {
        if(!userRepository.existsByName(userRequest.getUsername())) {
            throw new InvalidCredentialException("Username or password incorrect");
        }

        User user = userRepository.getByName(userRequest.getUsername());

        if (!passwordEncoder.matches(userRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialException("Username or password incorrect");
        }

        return userMapper.toResponse(user);
    }

    public void update(Long id, String name) {

    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

}
