package ru.rozhdestveno.taxi.entity.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findAllByStatus(EmployeeStatus status);

    @Query("SELECT id " +
            "FROM Employee e " +
            "WHERE e.status = 'DRIVER' " +
            "AND e.state = 'EMPLOYEE_ON_DUTY'")
    List<Long> findWaitingDriversIds();

    @Query("SELECT e " +
            "FROM Employee e " +
            "WHERE e.status = 'DRIVER' " +
            "AND e.state = 'EMPLOYEE_ON_DUTY'")
    List<Employee> findWaitingDrivers();

    @Query ("SELECT e.id " +
            "FROM Employee e " +
            "WHERE e.status = 'DISPATCHER' " +
            "AND e.state <> 'EMPLOYEE_ON_WEEKEND' " +
            "ORDER BY e.id " +
            "LIMIT 1")
    Long findDispatcherOnDuty();
}
