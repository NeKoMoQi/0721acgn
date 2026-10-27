package com.example.demo.controller;

import com.example.demo.dto.Live2dAgentChatRequest;
import com.example.demo.service.agent.PilgrimageRouteLibrary;
import com.example.demo.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PilgrimageRouteControllerTests {
    @Test void savedRoutesRequireVerifiedAccountEvenWhenCalledWithoutFilter() throws Exception {
        var library = mock(PilgrimageRouteLibrary.class);
        var mvc = MockMvcBuilders.standaloneSetup(new PilgrimageRouteController(library)).build();
        mvc.perform(get("/api/xunli/agent/routes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/xunli/agent/routes").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(library);
    }
    @Test void accountComesFromTokenRatherThanRequestPayload() throws Exception {
        var library = mock(PilgrimageRouteLibrary.class);
        when(library.list("user:42")).thenReturn(List.of());
        when(library.save(eq("user:42"), any())).thenReturn(Map.of("id", "fixture"));
        var mvc = MockMvcBuilders.standaloneSetup(new PilgrimageRouteController(library)).build();
        String token = JwtUtils.generateJwt(new java.util.HashMap<>(Map.of("id", 42, "username", "test-user")));
        mvc.perform(get("/api/xunli/agent/routes").header("token", token)).andExpect(status().isOk());
        mvc.perform(post("/api/xunli/agent/routes").header("token", token).contentType("application/json")
                .content("{\"owner\":\"user:other\",\"title\":\"我的\",\"plan\":{}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(library).save(eq("user:42"), any());
    }
    @Test void chatJsonCannotOverrideServerBoundOwner() throws Exception {
        var dto = new ObjectMapper().readValue("{\"query\":\"你好\",\"ownerKey\":\"user:other\"}", Live2dAgentChatRequest.class);
        assertEquals("legacy", dto.getOwnerKey());
    }
}
