package com.jl.booking.config;

import com.jl.booking.model.entity.Cleaner;
import com.jl.booking.model.entity.Vehicle;
import com.jl.booking.repository.CleanerRepository;
import com.jl.booking.repository.VehicleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DataSeeder {

    @Bean
    @Transactional
    CommandLineRunner init(VehicleRepository vehicleRepository, CleanerRepository cleanerRepository) {
        return args -> {
            if (vehicleRepository.count() == 0) {
                System.out.println("🌱 Seeding 5 vehicles + 25 cleaners...");

                // Create 5 vehicles (Vehicle-A to Vehicle-E)
                for (int i = 1; i <= 5; i++) {
                    Vehicle vehicle = new Vehicle();
                    vehicle.setName("Vehicle-" + (char)('A' + i - 1));
                    vehicleRepository.save(vehicle);

                    // Create 5 cleaners per vehicle (C1-C5)
                    for (int j = 1; j <= 5; j++) {
                        Cleaner cleaner = new Cleaner();
                        cleaner.setName("Cleaner-V" + (char)('A' + i - 1) + "-C" + j);
                        cleaner.setVehicle(vehicle);
                        cleanerRepository.save(cleaner);
                    }
                }
                System.out.println("✅ Seeded 5 vehicles + 25 cleaners");
            }
        };
    }
}
