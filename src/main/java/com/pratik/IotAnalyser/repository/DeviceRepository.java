package com.pratik.IotAnalyser.repository;

import com.pratik.IotAnalyser.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device,Long> {

    List<Device> findDeviceByStatus(Device.Status status);
    Optional<Device> findByDeviceId(Long deviceId);

}
