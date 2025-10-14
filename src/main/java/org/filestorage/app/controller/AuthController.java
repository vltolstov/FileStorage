package org.filestorage.app.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.filestorage.app.dto.UserRequest;
import org.filestorage.app.dto.UserResponse;
import org.filestorage.app.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;


//    Тело запроса (application/json):
//    {
//        "username": "user_1",
//            "password": "password"
//    }
//
//    Ответ в случае успеха: 201 Created со следующим телом:
//    {
//        "username": "user_1"
//    }
    @PostMapping("/auth/sign-up")
    public ResponseEntity<Map <String, String>> signUp(@Valid @RequestBody UserRequest userRequest, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        UserResponse userResponse = userService.create(userRequest);

        HttpSession session = httpServletRequest.getSession();
        session.setAttribute("username", userRequest.getUsername());

        Cookie cookie = new Cookie("JSESSIONID", session.getId());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        httpServletResponse.addCookie(cookie);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("username", userResponse.getUsername()));
    }


//    Тело запроса (application/json):
//    {
//        "username": "user_1",
//            "password": "password"
//    }
//
//    Тело ответа в случае успеха (200 OK):
//    {
//        "username": "user_1"
//    }
    @PostMapping("/auth/sign-in")
    public void signIn(String name, String password) {
        System.out.println("Sign in");
//        Коды ошибок:
//
//        400 - ошибки валидации (пример - слишком короткий username)
//        401 - неверные данные (такого пользователя нет, или пароль неправильный)
//        500 - неизвестная ошибка
    }



//    Тела запроса нет.
//    Тело ответа в случае успеха (204 No Content) пустое.
//    Коды ошибок:
//            401 - запрос исполняется неавторизованным юзером
//    500 - неизвестная ошибка
    @PostMapping("/auth/sign-out")
    public void signOut(Long id) {

    }

}
