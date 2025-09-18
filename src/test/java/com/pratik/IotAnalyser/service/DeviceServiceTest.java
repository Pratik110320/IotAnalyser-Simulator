package com.pratik.IotAnalyser.service;

import com.pratik.IotAnalyser.dtos.deviceDto.DeviceRegistrationDto;
import com.pratik.IotAnalyser.dtos.deviceDto.DeviceResponseDto;
import com.pratik.IotAnalyser.model.Device;
import com.pratik.IotAnalyser.repository.DeviceRepository;
import com.pratik.IotAnalyser.dtos.deviceDto.DeviceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceMapper deviceMapper;

    @InjectMocks
    private DeviceService deviceService;

    @Test
    void addDevice() {
        DeviceRegistrationDto registrationDto = new DeviceRegistrationDto("Test Device", "Test Type");
        Device device = new Device();
        DeviceResponseDto expectedResponse = new DeviceResponseDto();
        expectedResponse.setDeviceName("Test Device");

        when(deviceMapper.deviceToEntity(any(DeviceRegistrationDto.class))).thenReturn(device);
        when(deviceRepository.save(any(Device.class))).thenReturn(device);
        when(deviceMapper.toDeviceResponseDto(any(Device.class))).thenReturn(expectedResponse);

        DeviceResponseDto actualResponse = deviceService.addDevice(registrationDto);

        assertEquals(expectedResponse.getDeviceName(), actualResponse.getDeviceName());
    }
}