package com.jl.booking.repository;

import com.jl.booking.model.entity.Vehicle;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.id = :vehicleId")
    Optional<Vehicle> findByIdForBooking(@Param("vehicleId") Long vehicleId);

    List<Vehicle> findAll();

    @Query("SELECT v FROM Vehicle v LEFT JOIN FETCH v.cleaners")
    List<Vehicle> findAllWithCleaners();
}

