package com.tripsplit.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripsplit.model.Participant;

public interface ParticipantRepository
        extends JpaRepository<Participant, UUID> {

    boolean existsByTripIdAndEmail(
            UUID tripId,
            String email
    );

    Optional<Participant> findByIdAndTripId(
            UUID id,
            UUID tripId
    );
}