package ru.rozhdestveno.taxi.entity.lost;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LostRepository extends JpaRepository<Lost, Long> {
    @Query("SELECT l " +
            "FROM Lost l " +
            "WHERE l.publishedOn BETWEEN ?1 AND ?2")
    List<Lost> findByPeriod(LocalDate start, LocalDate end);

    @Query("SELECT l " +
            "FROM Lost l " +
            "WHERE l.client.id = ?1 " +
            "AND l.publishedOn = ?2 " +
            "ORDER BY l.publishedOn DESC " +
            "LIMIT 3")
    List<Lost> findLastLosts(long clientId, LocalDate date);
}
