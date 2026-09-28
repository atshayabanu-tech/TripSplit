package com.tripsplit.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class TripDtos {

    private TripDtos() {
    }

    // =========================
    // TRIP
    // =========================

    public record CreateTripRequest(
            @NotBlank
            @Size(max = 160)
            String name
    ) {
    }

    // =========================
    // PARTICIPANT
    // =========================

    public record AddParticipantRequest(
            @NotBlank
            @Size(max = 120)
            String name,

            @NotBlank
            @Email
            @Size(max = 180)
            String email
    ) {
    }

    public record UpdateParticipantRequest(
            @NotBlank
            @Size(max = 120)
            String name,

            @NotBlank
            @Email
            @Size(max = 180)
            String email
    ) {
    }

    // =========================
    // EXPENSE
    // =========================

    public record AddExpenseRequest(
            @NotNull
            UUID payerId,

            @NotBlank
            @Size(max = 180)
            String description,

            @NotNull
            @DecimalMin("0.01")
            @Digits(integer = 17, fraction = 2)
            BigDecimal amount,

            @NotBlank
            @Pattern(regexp = "[A-Za-z]{3}")
            String currency,

            @NotEmpty
            List<UUID> participantIds
    ) {
    }

    public record UpdateExpenseRequest(
            @NotNull
            UUID payerId,

            @NotBlank
            @Size(max = 180)
            String description,

            @NotNull
            @DecimalMin("0.01")
            @Digits(integer = 17, fraction = 2)
            BigDecimal amount,

            @NotBlank
            @Pattern(regexp = "[A-Za-z]{3}")
            String currency,

            @NotEmpty
            List<UUID> participantIds
    ) {
    }

    // =========================
    // RESPONSES
    // =========================

    public record ParticipantResponse(
            UUID id,
            String name,
            String email,
            Instant createdAt
    ) {
    }

    public record ExpenseResponse(
            UUID id,
            UUID payerId,
            String payerName,
            String description,
            BigDecimal amount,
            String currency,
            Instant createdAt,
            List<ParticipantResponse> sharedBy
    ) {
    }

    public record TripResponse(
            UUID id,
            String name,
            Instant createdAt,
            List<ParticipantResponse> participants,
            List<ExpenseResponse> expenses
    ) {
    }

    // =========================
    // SETTLEMENT
    // =========================

    public record BalanceResponse(
            UUID participantId,
            String participantName,
            BigDecimal paid,
            BigDecimal share,
            BigDecimal net
    ) {
    }

    public record SettlementResponse(
            UUID fromParticipantId,
            String fromParticipantName,
            UUID toParticipantId,
            String toParticipantName,
            BigDecimal amount,
            String currency
    ) {
    }

    public record SettlementSummaryResponse(
            UUID tripId,
            String currency,
            List<BalanceResponse> balances,
            List<SettlementResponse> settlements
    ) {
    }
}