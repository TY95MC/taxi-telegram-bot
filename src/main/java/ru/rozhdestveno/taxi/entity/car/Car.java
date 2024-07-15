package ru.rozhdestveno.taxi.entity.car;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rozhdestveno.taxi.entity.employee.Employee;

@Entity
@Table(name = "cars")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    @JoinColumn(name = "driver_id")
    private Employee driver;

    @Column(name = "license_plate", unique = true)
    @Size(max = 9)
    private String licensePlate;

    @Column(name = "car_name")
    @Size(max = 64)
    private String carBrandAndModel;
}
