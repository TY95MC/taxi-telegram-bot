package ru.rozhdestveno.taxi.entity.util;

import lombok.Data;
import ru.rozhdestveno.taxi.entity.customer.Customer;

import java.time.LocalDate;

@Data
public abstract class ClientRequest {
    private Long id;

    private String text;

    private Customer client;

    private LocalDate publishedOn;
}
