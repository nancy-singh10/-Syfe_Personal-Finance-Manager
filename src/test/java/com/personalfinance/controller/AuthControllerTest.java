package com.personalfinance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalfinance.dto.auth.LoginRequest;
import com.personalfinance.dto.auth.RegisterRequest;
import com.personalfinance.dto.auth.RegisterResponse;
import com.personalfinance.dto.common.MessageResponse;
import com.personalfinance.exception.ConflictException;
import com.personalfinance.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void testRegisterEndpointReturns201Created() throws Exception {
        RegisterRequest req = new RegisterRequest("john@example.com", "password123", "John Doe", "+1234567890");
        RegisterResponse res = new RegisterResponse("User registered successfully", 1L);

        when(authService.register(any(RegisterRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void testRegisterDuplicateUsernameReturns409Conflict() throws Exception {
        RegisterRequest req = new RegisterRequest("john@example.com", "password123", "John Doe", "+1234567890");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ConflictException("User with username john@example.com already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testRegisterInvalidEmailReturns400BadRequest() throws Exception {
        RegisterRequest req = new RegisterRequest("not-an-email", "password123", "John Doe", "+1234567890");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testLoginEndpointReturns200Ok() throws Exception {
        LoginRequest req = new LoginRequest("john@example.com", "password123");
        MessageResponse res = new MessageResponse("Login successful");

        when(authService.login(any(LoginRequest.class), any(), any())).thenReturn(res);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }
}
