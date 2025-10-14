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

    private final UserService userService;

    @GetMapping("/user/me")
    public ResponseEntity<Map<String, String>> getUser(HttpServletRequest req, HttpServletResponse res) {

        if(req.getSession().getAttribute("username") == null) {
            throw new UserNotAuthorizedException("user not authorized");
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of("message", req.getSession().getAttribute("username").toString()));
    }
}
