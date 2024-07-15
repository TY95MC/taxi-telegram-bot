package ru.rozhdestveno.taxi.entity.contact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomContactRepository extends JpaRepository<CustomContact, Integer> {
    @Query("SELECT c " +
            "FROM CustomContact c " +
            "ORDER BY c.id DESC")
    List<CustomContact> getAllContacts();

    CustomContact findByPhoneNumber(String phoneNumber);
}
