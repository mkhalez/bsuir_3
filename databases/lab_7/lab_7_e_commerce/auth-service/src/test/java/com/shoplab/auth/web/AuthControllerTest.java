package com.shoplab.auth.web;

import com.shoplab.auth.dto.*;
import com.shoplab.auth.service.AuthService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @MockitoBean AuthService auth;

    @Test
    void signupReturns201WithStatus() throws Exception {
        UUID id = UUID.randomUUID();
        Mockito.when(auth.register(Mockito.any(), Mockito.any())).thenReturn(id);
        var req = new SignupRequest("bob", "secret123", "Bob", "Smith", "bob@shop.by", LocalDate.of(2000, 1, 1));
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationId").value(id.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void loginReturnsTokens() throws Exception {
        Mockito.when(auth.login(Mockito.any(), Mockito.any()))
                .thenReturn(new AuthResponse("A", "R", "bob", 1L, java.util.List.of("USER")));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("A"));
    }
}
