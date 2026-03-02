package com.jl.booking.repository;

import com.jl.booking.model.entity.Booking;
import com.jl.booking.model.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
                SELECT DISTINCT b FROM Booking b
                LEFT JOIN FETCH b.cleaners
                WHERE b.vehicle = :vehicle
                AND b.status = 'ACTIVE'
                AND :testStart < b.endTime
                AND :testEnd > b.startTime
                ORDER BY b.startTime
            """)
    List<Booking> findOverlappingBookingsWithCleaners(@Param("vehicle") Vehicle vehicle,
                                                      @Param("testStart") LocalDateTime testStart,
                                                      @Param("testEnd") LocalDateTime testEnd);

    @Query("""
                SELECT DISTINCT b FROM Booking b
                WHERE b.vehicle = :vehicle
                AND b.status = 'ACTIVE'
                AND FUNCTION('DATE', b.startTime) = :date
            """)
    List<Booking> findByVehicleAndDate(@Param("vehicle") Vehicle vehicle,
                                       @Param("date") LocalDate date);

    @Query("""
                SELECT DISTINCT b FROM Booking b
                LEFT JOIN FETCH b.cleaners
                WHERE b.status = 'ACTIVE'
                AND FUNCTION('DATE', b.startTime) = :date
                AND EXISTS (
                    SELECT 1 FROM b.cleaners c WHERE c.id = :cleanerId
                )
            """)
    List<Booking> findByCleanerIdAndDate(@Param("cleanerId") Long cleanerId,
                                         @Param("date") LocalDate date);
}

