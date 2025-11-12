package org.filestorage.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.filestorage.app.config.UserDetailsServiceImpl;
import org.filestorage.app.dto.UserRequest;
import org.filestorage.app.dto.UserResponse;
import org.filestorage.app.exception.UserNotAuthorizedException;
import org.filestorage.app.model.User;
import org.filestorage.app.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserDetailsServiceImpl userDetailsServiceImpl;

    @Operation(summary = "Регистрация пользователя", description = "Возвращает username пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует")
    })
    @PostMapping("/auth/sign-up")
    public ResponseEntity<Map <String, String>> signUp(@Valid @RequestBody UserRequest userRequest, HttpServletRequest request, HttpServletResponse response) {
        UserResponse userResponse = userService.create(userRequest);
        authenticateSession(request, response, userRequest.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("username", userResponse.getUsername()));
    }

    @Operation(summary = "Авторизация пользователя", description = "Возвращает username пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный вход"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Неверные данные: пользователя нет или пароль неправильный")
    })
    @PostMapping("/auth/sign-in")
    public ResponseEntity<Map<String, String>> signIn(@Valid @RequestBody UserRequest userRequest, HttpServletRequest request, HttpServletResponse response) {
        UserResponse userResponse = userService.getUser(userRequest);
        authenticateSession(request, response, userRequest.getUsername());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of("username", userResponse.getUsername()));
    }

    @Operation(summary = "Выход пользователя", description = "Возвращает пустое тело")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Успешный выход"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    @PostMapping("/auth/sign-out")
    public ResponseEntity<Void> signOut(HttpServletRequest request, HttpServletResponse response) {

        if(request.getSession(false) == null) {
            throw new UserNotAuthorizedException("user not authorized");
        }

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        Cookie cookie = cookieInit("");
        response.addCookie(cookie);

        return ResponseEntity
                .noContent()
                .build();
    }

    private void authenticateSession(HttpServletRequest request, HttpServletResponse response, String username) {
        User userDetails = (User) userDetailsServiceImpl.loadUserByUsername(username);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        List.of()
                );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true)
                .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        Cookie cookie = cookieInit(request.getSession().getId());
        response.addCookie(cookie);
    }

    private Cookie cookieInit(String sessionId) {
        Cookie cookie = new Cookie("JSESSIONID", sessionId);
        cookie.setHttpOnly(true);
        cookie.setPath("/");

        if (sessionId.isEmpty()) {
            cookie.setMaxAge(0);
        }
        return cookie;
    }

}
