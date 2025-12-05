package org.filestorage.app;

import jakarta.servlet.http.Cookie;
import org.filestorage.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AuthTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    static final String REQUEST_BODY = """
                {
                    "username": "testUserName",
                    "password": "testPassword"
                }
                """;

    static final String EXPECTED_JSON = """
                {
                    "username": "testUserName"
                }
        """;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnInvalidRegistrationData() throws Exception {
        String invalidRequestBody = """
                {
                    "username": " ",
                    "password": " "
                }
                """;

        mockMvc.perform(post("/api/auth/sign-up")
                .contentType("application/json")
                .content(invalidRequestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSaveUserInDbAndReturnUserName() throws Exception {
        mockMvc.perform(post("/api/auth/sign-up")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().json(EXPECTED_JSON));
    }

    @Test
    void shouldAuthenticateUserInSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/sign-up")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        String sessionCookie = response.getCookie("SESSION").getValue();
        assertNotNull(sessionCookie);

        MvcResult second = mockMvc.perform(get("/api/user/me")
                .cookie(new Cookie("SESSION", sessionCookie)))
                .andReturn();

        String responseBody = second.getResponse().getContentAsString();
        assertTrue(responseBody.contains("testUserName"), "Username should be present in response");
    }

    @Test
    void shouldSetCookieAtRegistration() throws Exception {
        mockMvc.perform(post("/api/auth/sign-up")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("SESSION"));
    }

    @Test
    void shouldReturnConflictWhenUserAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/auth/sign-up")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/sign-up")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldLoginAndReturnUserName() throws Exception {
        mockMvc.perform(post("/api/auth/sign-up").contentType("application/json").content(REQUEST_BODY)).andReturn();

        mockMvc.perform(post("/api/auth/sign-in")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(content().json(EXPECTED_JSON));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType("application/json")
                        .content(REQUEST_BODY))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void shouldReturnBadRequestForInvalidCredentials() throws Exception {

        String invalidCredentials = """
            {
                "username": " ",
                "password": " "
            }
        """;

        mockMvc.perform(post("/api/auth/sign-in")
                .contentType("application/json")
                .content(invalidCredentials))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSetSessionWhenUserSignIn() throws Exception {
        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType("application/json")
                        .content(REQUEST_BODY))
                .andExpect(status().isCreated());

        MvcResult signInResult = mockMvc.perform(post("/api/auth/sign-in")
                        .contentType("application/json")
                        .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse signInResponse = signInResult.getResponse();
        Cookie sessionCookie = signInResponse.getCookie("SESSION");
        assertNotNull(sessionCookie, "SESSION cookie should not be null");

        MvcResult meResult = mockMvc.perform(get("/api/user/me")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = meResult.getResponse().getContentAsString();
        assertTrue(responseBody.contains("testUserName"), "Username should be present in response");
    }

    @Test
    void shouldSetCookieWhenUserSignIn() throws Exception {
        mockMvc.perform(post("/api/auth/sign-up").contentType("application/json").content(REQUEST_BODY)).andReturn();

        mockMvc.perform(post("/api/auth/sign-in")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("SESSION"));
    }

    @Test
    void shouldReturnNoContentWhenUserSignOut() throws Exception {
        MvcResult signUpResult = mockMvc.perform(post("/api/auth/sign-up")
                .contentType("application/json")
                .content(REQUEST_BODY))
            .andReturn();

        MockHttpServletResponse signUpResponse = signUpResult.getResponse();
        Cookie sessionCookie = signUpResponse.getCookie("SESSION");
        assertNotNull(sessionCookie, "SESSION cookie should not be null");

        mockMvc.perform(post("/api/auth/sign-out")
                .cookie(sessionCookie))
                .andExpect(status().isNoContent());
    }
}
