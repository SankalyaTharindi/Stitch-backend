package com.stitch.app.repository;

import com.stitch.app.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    // Use customer_id to query by the relationship's ID
    List<Appointment> findByCustomer_Id(Long customerId);
    List<Appointment> findByStatus(Appointment.Status status);
    List<Appointment> findByDeadlineBetween(LocalDate start, LocalDate end);
    List<Appointment> findAllByOrderByCreatedAtDesc();
    Optional<Appointment> findByIdAndCustomer_Id(Long id, Long customerId);
}