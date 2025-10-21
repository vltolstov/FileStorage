package org.filestorage.app.controller;

import org.filestorage.app.dto.UserResponse;
import org.filestorage.app.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @Test
    void shouldRegisterUserAndReturnUserName() throws Exception {

        String requestBody = """
                {
                    "username": "testUserName",
                    "password": "testPassword"
                }
                """;

        String expectedJson = """
                {
                    "username": "testUserName"
                }
        """;

        when(userService.create(any())).thenReturn(new UserResponse(1L, "testUserName"));

        mockMvc.perform(post("/auth/sign-up")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().json(expectedJson));
    }

    @Test
    void shouldLoginUserAndReturnUserName() throws Exception {

        String requestBody = """
                {
                    "username": "testUserName",
                    "password": "testPassword"
                }
                """;

        String expectedJson = """
                {
                    "username": "testUserName"
                }
        """;

        when(userService.getUser(any())).thenReturn(new UserResponse(1L, "testUserName"));

        mockMvc.perform(post("/auth/sign-in")
                    .contentType("application/json")
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @Test
    void shouldLogoutAndReturnEmptyBody() throws Exception {

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("username", "testUserName");

        mockMvc.perform(post("/auth/sign-out")
                        .session(session))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void shouldReturnUnAuthorizedWhenLogout() throws Exception {
        mockMvc.perform(post("/auth/sign-out"))
                .andExpect(status().isUnauthorized());
    }

}
