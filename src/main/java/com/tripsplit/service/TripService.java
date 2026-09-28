package com.tripsplit.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripsplit.dto.TripDtos.*;
import com.tripsplit.exception.BusinessRuleException;
import com.tripsplit.exception.NotFoundException;
import com.tripsplit.model.Expense;
import com.tripsplit.model.Participant;
import com.tripsplit.model.Trip;
import com.tripsplit.repository.ExpenseRepository;
import com.tripsplit.repository.ParticipantRepository;
import com.tripsplit.repository.TripRepository;

@Service
@Transactional
public class TripService {

    private final TripRepository trips;
    private final ParticipantRepository participants;
    private final ExpenseRepository expenses;

    public TripService(
            TripRepository trips,
            ParticipantRepository participants,
            ExpenseRepository expenses
    ) {
        this.trips = trips;
        this.participants = participants;
        this.expenses = expenses;
    }

    // =========================================================
    // TRIP
    // =========================================================

    public TripResponse createTrip(CreateTripRequest req) {

        return response(
                trips.save(
                        new Trip(req.name().trim())
                )
        );
    }

    public TripResponse getTrip(UUID id) {

        return response(findTrip(id));
    }

    // =========================================================
    // PARTICIPANT - CREATE
    // =========================================================

    public ParticipantResponse addParticipant(
            UUID tripId,
            AddParticipantRequest req
    ) {

        Trip trip = findTrip(tripId);

        String email =
                req.email()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (participants.existsByTripIdAndEmail(
                tripId,
                email
        )) {

            throw new BusinessRuleException(
                    "A participant with this email already exists in the trip"
            );
        }

        return participantResponse(
                participants.save(
                        new Participant(
                                trip,
                                req.name().trim(),
                                email
                        )
                )
        );
    }

    // =========================================================
    // PARTICIPANT - UPDATE
    // =========================================================

    public ParticipantResponse updateParticipant(
            UUID tripId,
            UUID participantId,
            UpdateParticipantRequest req
    ) {

        findTrip(tripId);

        Participant participant =
                participants.findByIdAndTripId(
                        participantId,
                        tripId
                ).orElseThrow(
                        () -> new NotFoundException(
                                "Participant not found in this trip"
                        )
                );

        String email =
                req.email()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (!participant.getEmail().equalsIgnoreCase(email)
                && participants.existsByTripIdAndEmail(
                        tripId,
                        email
                )) {

            throw new BusinessRuleException(
                    "A participant with this email already exists in the trip"
            );
        }

        participant.setName(
                req.name().trim()
        );

        participant.setEmail(email);

        return participantResponse(
                participants.save(participant)
        );
    }

    // =========================================================
    // PARTICIPANT - DELETE
    // =========================================================

    public void deleteParticipant(
            UUID tripId,
            UUID participantId
    ) {

        findTrip(tripId);

        Participant participant =
                participants.findByIdAndTripId(
                        participantId,
                        tripId
                ).orElseThrow(
                        () -> new NotFoundException(
                                "Participant not found in this trip"
                        )
                );

        /*
         * We do not allow deleting a participant who is
         * already used in an expense.
         *
         * This prevents broken expense records.
         */
        boolean usedAsPayer =
                expenses.existsByPayerId(participantId);

        boolean usedInExpense =
                expenses.existsBySharedById(participantId);

        if (usedAsPayer || usedInExpense) {

            throw new BusinessRuleException(
                    "Cannot delete this participant because they are already used in an expense"
            );
        }

        participants.delete(participant);
    }

    // =========================================================
    // EXPENSE - CREATE
    // =========================================================

    public ExpenseResponse addExpense(
            UUID tripId,
            AddExpenseRequest req
    ) {

        Trip trip = findTrip(tripId);

        Participant payer =
                participants.findByIdAndTripId(
                        req.payerId(),
                        tripId
                ).orElseThrow(
                        () -> new NotFoundException(
                                "Payer does not belong to this trip"
                        )
                );

        List<Participant> sharedBy =
                findSharedParticipants(
                        tripId,
                        req.participantIds()
                );

        Expense expense =
                new Expense(
                        trip,
                        payer,
                        req.description().trim(),
                        req.amount().setScale(2),
                        req.currency()
                                .toUpperCase(Locale.ROOT)
                );

        expense.setSharedBy(sharedBy);

        return expenseResponse(
                expenses.save(expense)
        );
    }

    // =========================================================
    // EXPENSE - UPDATE
    // =========================================================

    public ExpenseResponse updateExpense(
            UUID tripId,
            UUID expenseId,
            UpdateExpenseRequest req
    ) {

        findTrip(tripId);

        Expense expense =
                expenses.findByIdAndTripId(
                        expenseId,
                        tripId
                ).orElseThrow(
                        () -> new NotFoundException(
                                "Expense not found in this trip"
                        )
                );

        Participant payer =
                participants.findByIdAndTripId(
                        req.payerId(),
                        tripId
                ).orElseThrow(
                        () -> new NotFoundException(
                                "Payer does not belong to this trip"
                        )
                );

        List<Participant> sharedBy =
                findSharedParticipants(
                        tripId,
                        req.participantIds()
                );

        /*
         * Update all editable expense fields.
         */
        expense.setPayer(payer);

        expense.setDescription(
                req.description().trim()
        );

        expense.setAmount(
                req.amount().setScale(2)
        );

        expense.setCurrency(
                req.currency()
                        .toUpperCase(Locale.ROOT)
        );

        expense.setSharedBy(sharedBy);

        return expenseResponse(
                expenses.save(expense)
        );
    }

    // =========================================================
    // EXPENSE - DELETE
    // =========================================================

    public void deleteExpense(
            UUID tripId,
            UUID expenseId
    ) {

        findTrip(tripId);

        Expense expense =
                expenses.findByIdAndTripId(
                        expenseId,
                        tripId
                ).orElseThrow(
                        () -> new NotFoundException(
                                "Expense not found in this trip"
                        )
                );

        expenses.delete(expense);
    }

    // =========================================================
    // SETTLEMENT
    // =========================================================

    @Transactional(readOnly = true)
    public SettlementSummaryResponse settle(
            UUID tripId
    ) {

        Trip trip = findTrip(tripId);

        List<Participant> people =
                trip.getParticipants();

        List<Expense> all =
                expenses.findAllByTripIdOrderByCreatedAtDesc(
                        tripId
                );

        if (people.isEmpty()) {

            throw new BusinessRuleException(
                    "Add at least one participant before settlement"
            );
        }

        Set<String> currencies =
                all.stream()
                        .map(Expense::getCurrency)
                        .collect(Collectors.toSet());

        if (currencies.size() > 1) {

            throw new BusinessRuleException(
                    "Settlement requires a single currency; found: "
                            + currencies
            );
        }

        String currency =
                currencies.stream()
                        .findFirst()
                        .orElse("INR");

        Map<UUID, BigDecimal> paid =
                new LinkedHashMap<>();

        Map<UUID, BigDecimal> shares =
                new LinkedHashMap<>();

        people.forEach(
                p -> {

                    paid.put(
                            p.getId(),
                            BigDecimal.ZERO.setScale(2)
                    );

                    shares.put(
                            p.getId(),
                            BigDecimal.ZERO.setScale(2)
                    );
                }
        );

        for (Expense expense : all) {

            UUID payerId =
                    expense.getPayer().getId();

            paid.merge(
                    payerId,
                    expense.getAmount(),
                    BigDecimal::add
            );

            List<Participant> sharedBy =
                    expense.getSharedBy();

            if (sharedBy == null ||
                    sharedBy.isEmpty()) {

                throw new BusinessRuleException(
                        "Expense '" +
                                expense.getDescription() +
                                "' has no participants assigned"
                );
            }

            BigDecimal amount =
                    expense.getAmount()
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            long totalCents =
                    amount
                            .movePointRight(2)
                            .longValueExact();

            long participantCount =
                    sharedBy.size();

            long baseCents =
                    totalCents / participantCount;

            long remainderCents =
                    totalCents % participantCount;

            for (
                    int index = 0;
                    index < sharedBy.size();
                    index++
            ) {

                Participant participant =
                        sharedBy.get(index);

                long centsForParticipant =
                        baseCents;

                if (index < remainderCents) {
                    centsForParticipant++;
                }

                BigDecimal participantShare =
                        BigDecimal
                                .valueOf(
                                        centsForParticipant
                                )
                                .movePointLeft(2)
                                .setScale(2);

                shares.merge(
                        participant.getId(),
                        participantShare,
                        BigDecimal::add
                );
            }
        }

        List<BalanceResponse> balances =
                people.stream()
                        .map(
                                p -> {

                                    BigDecimal pPaid =
                                            paid.get(
                                                    p.getId()
                                            );

                                    BigDecimal pShare =
                                            shares.get(
                                                    p.getId()
                                            );

                                    BigDecimal net =
                                            pPaid
                                                    .subtract(
                                                            pShare
                                                    )
                                                    .setScale(
                                                            2,
                                                            RoundingMode.HALF_UP
                                                    );

                                    return new BalanceResponse(
                                            p.getId(),
                                            p.getName(),
                                            pPaid.setScale(2),
                                            pShare.setScale(2),
                                            net
                                    );
                                }
                        )
                        .toList();

        List<MutableBalance> creditors =
                balances.stream()
                        .filter(
                                b -> b.net()
                                        .compareTo(
                                                BigDecimal.ZERO
                                        ) > 0
                        )
                        .map(
                                b -> new MutableBalance(
                                        b.participantId(),
                                        b.participantName(),
                                        b.net()
                                )
                        )
                        .collect(
                                Collectors.toCollection(
                                        ArrayList::new
                                )
                        );

        List<MutableBalance> debtors =
                balances.stream()
                        .filter(
                                b -> b.net()
                                        .compareTo(
                                                BigDecimal.ZERO
                                        ) < 0
                        )
                        .map(
                                b -> new MutableBalance(
                                        b.participantId(),
                                        b.participantName(),
                                        b.net().abs()
                                )
                        )
                        .collect(
                                Collectors.toCollection(
                                        ArrayList::new
                                )
                        );

        List<SettlementResponse> settlements =
                new ArrayList<>();

        int i = 0;
        int j = 0;

        while (
                i < debtors.size()
                        && j < creditors.size()
        ) {

            MutableBalance debtor =
                    debtors.get(i);

            MutableBalance creditor =
                    creditors.get(j);

            BigDecimal amount =
                    debtor.amount
                            .min(creditor.amount)
                            .setScale(2);

            if (
                    amount.compareTo(
                            BigDecimal.ZERO
                    ) > 0
            ) {

                settlements.add(
                        new SettlementResponse(
                                debtor.id,
                                debtor.name,
                                creditor.id,
                                creditor.name,
                                amount,
                                currency
                        )
                );
            }

            debtor.amount =
                    debtor.amount
                            .subtract(amount)
                            .setScale(2);

            creditor.amount =
                    creditor.amount
                            .subtract(amount)
                            .setScale(2);

            if (debtor.amount.signum() == 0) {
                i++;
            }

            if (creditor.amount.signum() == 0) {
                j++;
            }
        }

        return new SettlementSummaryResponse(
                tripId,
                currency,
                balances,
                settlements
        );
    }

    // =========================================================
    // HELPER - FIND SHARED PARTICIPANTS
    // =========================================================

    private List<Participant> findSharedParticipants(
            UUID tripId,
            List<UUID> participantIds
    ) {

        if (
                participantIds == null
                        || participantIds.isEmpty()
        ) {

            throw new BusinessRuleException(
                    "Select at least one participant who shares this expense"
            );
        }

        List<Participant> sharedBy =
                new ArrayList<>();

        for (UUID participantId : participantIds) {

            Participant participant =
                    participants.findByIdAndTripId(
                            participantId,
                            tripId
                    ).orElseThrow(
                            () -> new NotFoundException(
                                    "A selected participant does not belong to this trip"
                            )
                    );

            if (!sharedBy.contains(participant)) {
                sharedBy.add(participant);
            }
        }

        return sharedBy;
    }

    // =========================================================
    // FIND TRIP
    // =========================================================

    private Trip findTrip(UUID id) {

        return trips.findById(id)
                .orElseThrow(
                        () -> new NotFoundException(
                                "Trip not found: " + id
                        )
                );
    }

    // =========================================================
    // RESPONSE HELPERS
    // =========================================================

    private TripResponse response(Trip trip) {

        return new TripResponse(
                trip.getId(),
                trip.getName(),
                trip.getCreatedAt(),
                trip.getParticipants()
                        .stream()
                        .map(this::participantResponse)
                        .toList(),
                trip.getExpenses()
                        .stream()
                        .map(this::expenseResponse)
                        .toList()
        );
    }

    private ParticipantResponse participantResponse(
            Participant participant
    ) {

        return new ParticipantResponse(
                participant.getId(),
                participant.getName(),
                participant.getEmail(),
                participant.getCreatedAt()
        );
    }

    private ExpenseResponse expenseResponse(
            Expense expense
    ) {

        return new ExpenseResponse(
                expense.getId(),
                expense.getPayer().getId(),
                expense.getPayer().getName(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getCreatedAt(),
                expense.getSharedBy()
                        .stream()
                        .map(this::participantResponse)
                        .toList()
        );
    }

    // =========================================================
    // MUTABLE BALANCE
    // =========================================================

    private static class MutableBalance {

        UUID id;
        String name;
        BigDecimal amount;

        MutableBalance(
                UUID id,
                String name,
                BigDecimal amount
        ) {

            this.id = id;
            this.name = name;
            this.amount = amount;
        }
    }
}