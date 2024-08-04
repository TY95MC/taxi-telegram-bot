package ru.rozhdestveno.taxi.entity.lost;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rozhdestveno.taxi.entity.customer.Customer;
import ru.rozhdestveno.taxi.entity.util.ClientRequest;

import java.time.LocalDate;

@Entity
@Table(name = "losts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Lost extends ClientRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "lost_text")
    @Size(max = 1500)
    private String text;

    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Customer client;

    @Column(name = "published_on")
    private LocalDate publishedOn = LocalDate.now();
}
