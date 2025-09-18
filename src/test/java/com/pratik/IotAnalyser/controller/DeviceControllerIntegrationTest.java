package com.pratik.IotAnalyser.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pratik.IotAnalyser.dtos.deviceDto.DeviceRegistrationDto;
import com.pratik.IotAnalyser.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeviceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeviceService deviceService;

    @Test
    @WithMockUser
    void addDevice() throws Exception {
        DeviceRegistrationDto registrationDto = new DeviceRegistrationDto("Test Device", "Test Type");

        mockMvc.perform(post("/device")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated());
    }
}