package ru.rozhdestveno.taxi.entity.car;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarRepository extends JpaRepository<Car, Integer> {
    @Query("SELECT c " +
            "FROM Car c " +
            "WHERE c.licensePlate = ?1")
    Optional<Car> getCar(String licensePlate);

    @Query("SELECT c " +
            "FROM Car c " +
            "WHERE c.driver.id = ?1")
    Car findDriverCar(long driverId);

    @Query("SELECT c " +
            "FROM Car c " +
            "WHERE c.driver.id in (?1)")
    List<Car> findDriversCars(List<Long> ids);
}
