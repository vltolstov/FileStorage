package org.filestorage.app.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.filestorage.app.exception.UserNotAuthorizedException;
import org.filestorage.app.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/user/me")
    public ResponseEntity<Map<String, String>> getUser(HttpServletRequest request, HttpServletResponse response) {

        if(request.getSession().getAttribute("username") == null) {
            throw new UserNotAuthorizedException("user not authorized");
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of("message", request.getSession().getAttribute("username").toString()));
    }
}
