package com.tripsplit.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripsplit.model.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findAllByTripIdOrderByCreatedAtDesc(UUID tripId);

    boolean existsByPayerId(UUID participantId);

    boolean existsBySharedById(UUID participantId);

    Optional<Expense> findByIdAndTripId(UUID id, UUID tripId);
}