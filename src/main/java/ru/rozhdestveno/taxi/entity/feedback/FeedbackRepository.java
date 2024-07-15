package ru.rozhdestveno.taxi.entity.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    @Query("SELECT f " +
            "FROM Feedback f " +
            "WHERE f.publishedOn BETWEEN ?1 AND ?2")
    List<Feedback> findByPeriod(LocalDate start, LocalDate end);

    @Query("SELECT f " +
            "FROM Feedback f " +
            "WHERE f.client.id = ?1 " +
            "AND f.publishedOn = ?2 " +
            "ORDER BY f.publishedOn DESC " +
            "LIMIT 3")
    List<Feedback> findLastFeedbacks(long clientId, LocalDate date);
}
