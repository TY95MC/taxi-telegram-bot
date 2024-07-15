package ru.rozhdestveno.taxi.entity.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contacts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CustomContact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "phone_number", unique = true)
    @Size(min = 17, max = 17)
    private String phoneNumber;
}
