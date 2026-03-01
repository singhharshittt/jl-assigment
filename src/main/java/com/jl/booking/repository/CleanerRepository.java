package com.jl.booking.repository;

import com.jl.booking.model.entity.Cleaner;
import com.jl.booking.model.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CleanerRepository extends JpaRepository<Cleaner, Long> {

    @Query("""
        SELECT c FROM Cleaner c 
        WHERE c.vehicle.id = :vehicleId
        """)
    List<Cleaner> findByVehicleId(@Param("vehicleId") Long vehicleId);
}
