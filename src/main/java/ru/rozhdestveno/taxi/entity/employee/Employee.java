package ru.rozhdestveno.taxi.entity.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "employees")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Employee {
    @Id
    private long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;

    @Enumerated(EnumType.STRING)
    private EmployeeState state = EmployeeState.EMPLOYEE_ON_WEEKEND;

    @Column(name = "registered_on")
    private LocalDate registeredOn = LocalDate.now();
}
