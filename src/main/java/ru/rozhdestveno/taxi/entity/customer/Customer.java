package ru.rozhdestveno.taxi.entity.customer;

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
@Table(name = "customers")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Customer {
    @Id
    private long id;

    @Enumerated(EnumType.STRING)
    private CustomerBanStatus status = CustomerBanStatus.NO_WARN;

    @Enumerated(EnumType.STRING)
    private CustomerState state;

    @Column(name = "registered_on")
    private LocalDate registeredOn = LocalDate.now();

    @Column(name = "first_warn")
    private LocalDate firstWarn;

    @Column(name = "second_warn")
    private LocalDate secondWarn;

    @Column(name = "banned_on")
    private LocalDate bannedOn;
}
