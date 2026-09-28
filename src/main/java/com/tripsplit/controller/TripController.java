package com.tripsplit.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripsplit.dto.TripDtos.AddExpenseRequest;
import com.tripsplit.dto.TripDtos.AddParticipantRequest;
import com.tripsplit.dto.TripDtos.CreateTripRequest;
import com.tripsplit.dto.TripDtos.ExpenseResponse;
import com.tripsplit.dto.TripDtos.ParticipantResponse;
import com.tripsplit.dto.TripDtos.SettlementSummaryResponse;
import com.tripsplit.dto.TripDtos.TripResponse;
import com.tripsplit.dto.TripDtos.UpdateExpenseRequest;
import com.tripsplit.dto.TripDtos.UpdateParticipantRequest;
import com.tripsplit.service.TripService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/trips")
@Tag(
    name = "Trips",
    description = "Trip, participant, expense and settlement operations"
)
public class TripController {

    private final TripService service;

    public TripController(TripService service) {
        this.service = service;
    }

    // =========================================================
    // TRIP
    // =========================================================

    @PostMapping
    @Operation(summary = "Create a trip")
    public ResponseEntity<TripResponse> create(
            @Valid @RequestBody CreateTripRequest req
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createTrip(req));
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "Get a trip with participants and expenses")
    public TripResponse get(
            @PathVariable UUID tripId
    ) {

        return service.getTrip(tripId);
    }

    // =========================================================
    // PARTICIPANT - CREATE
    // =========================================================

    @PostMapping("/{tripId}/participants")
    @Operation(summary = "Add a participant")
    public ResponseEntity<ParticipantResponse> addParticipant(
            @PathVariable UUID tripId,
            @Valid @RequestBody AddParticipantRequest req
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.addParticipant(tripId, req));
    }

    // =========================================================
    // PARTICIPANT - UPDATE
    // =========================================================

    @PutMapping("/{tripId}/participants/{participantId}")
    @Operation(summary = "Update a participant")
    public ParticipantResponse updateParticipant(
            @PathVariable UUID tripId,
            @PathVariable UUID participantId,
            @Valid @RequestBody UpdateParticipantRequest req
    ) {

        return service.updateParticipant(
                tripId,
                participantId,
                req
        );
    }

    // =========================================================
    // PARTICIPANT - DELETE
    // =========================================================

    @DeleteMapping("/{tripId}/participants/{participantId}")
    @Operation(summary = "Delete a participant")
    public ResponseEntity<Void> deleteParticipant(
            @PathVariable UUID tripId,
            @PathVariable UUID participantId
    ) {

        service.deleteParticipant(
                tripId,
                participantId
        );

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // EXPENSE - CREATE
    // =========================================================

    @PostMapping("/{tripId}/expenses")
    @Operation(summary = "Add an expense")
    public ResponseEntity<ExpenseResponse> addExpense(
            @PathVariable UUID tripId,
            @Valid @RequestBody AddExpenseRequest req
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.addExpense(tripId, req));
    }

    // =========================================================
    // EXPENSE - UPDATE
    // =========================================================

    @PutMapping("/{tripId}/expenses/{expenseId}")
    @Operation(summary = "Update an expense")
    public ExpenseResponse updateExpense(
            @PathVariable UUID tripId,
            @PathVariable UUID expenseId,
            @Valid @RequestBody UpdateExpenseRequest req
    ) {

        return service.updateExpense(
                tripId,
                expenseId,
                req
        );
    }

    // =========================================================
    // EXPENSE - DELETE
    // =========================================================

    @DeleteMapping("/{tripId}/expenses/{expenseId}")
    @Operation(summary = "Delete an expense")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable UUID tripId,
            @PathVariable UUID expenseId
    ) {

        service.deleteExpense(
                tripId,
                expenseId
        );

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // SETTLEMENT
    // =========================================================

    @GetMapping("/{tripId}/settlement")
    @Operation(summary = "Compute balances and minimal settlements")
    public SettlementSummaryResponse settlement(
            @PathVariable UUID tripId
    ) {

        return service.settle(tripId);
    }
}