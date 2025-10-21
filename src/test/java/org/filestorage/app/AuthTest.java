package org.filestorage.app;

import org.filestorage.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

        mockMvc.perform(post("/auth/sign-up")
                .contentType("application/json")
                .content(invalidRequestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSaveUserInDbAndReturnUserName() throws Exception {
        mockMvc.perform(post("/auth/sign-up")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().json(EXPECTED_JSON));
    }

    @Test
    void shouldAuthenticateUserInSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/sign-up")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(context, "Security context is null");

        Authentication auth = context.getAuthentication();
        assertNotNull(auth, "Authentication is null");
        assertEquals("testUserName", auth.getName());
    }

    @Test
    void shouldSetCookieAtRegistration() throws Exception {
        mockMvc.perform(post("/auth/sign-up")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("JSESSIONID"));
    }

    @Test
    void shouldReturnConflictWhenUserAlreadyExists() throws Exception {
        mockMvc.perform(post("/auth/sign-up")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/sign-up")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldLoginAndReturnUserName() throws Exception {
        mockMvc.perform(post("/auth/sign-up").contentType("application/json").content(REQUEST_BODY)).andReturn();

        mockMvc.perform(post("/auth/sign-in")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(content().json(EXPECTED_JSON));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/auth/sign-in")
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

        mockMvc.perform(post("/auth/sign-in")
                .contentType("application/json")
                .content(invalidCredentials))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSetSessionWhenUserSignIn() throws Exception {
        mockMvc.perform(post("/auth/sign-up").contentType("application/json").content(REQUEST_BODY)).andReturn();

        MvcResult result = mockMvc.perform(post("/auth/sign-in").contentType("application/json").content(REQUEST_BODY)).andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session, "Session is null");

        SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        Authentication auth = context.getAuthentication();

        assertEquals("testUserName", auth.getName());
    }

    @Test
    void shouldSetCookieWhenUserSignIn() throws Exception {
        mockMvc.perform(post("/auth/sign-up").contentType("application/json").content(REQUEST_BODY)).andReturn();

        mockMvc.perform(post("/auth/sign-in")
                .contentType("application/json")
                .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("JSESSIONID"));
    }

    @Test
    void shouldReturnNoContentWhenUserSignOut() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/sign-up").contentType("application/json").content(REQUEST_BODY)).andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(post("/auth/sign-out")
                        .session(session))
                .andExpect(status().isNoContent());
    }
}
