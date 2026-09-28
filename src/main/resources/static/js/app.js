const API_BASE = "http://localhost:8080/api/v1";

let currentTripId = localStorage.getItem("tripsplit_trip_id");
let currentTrip = null;


/* =========================================================
   BASIC HELPERS
   ========================================================= */

function $(id) {
    return document.getElementById(id);
}

function showToast(message) {
    const toast = $("toast");
    const toastMessage = $("toastMessage");

    toastMessage.textContent = message;
    toast.classList.remove("hidden");

    setTimeout(() => {
        toast.classList.add("hidden");
    }, 3000);
}

function openModal(id) {
    $(id).classList.remove("hidden");
}

function closeModal(id) {
    $(id).classList.add("hidden");
}

function escapeHtml(value) {
    const div = document.createElement("div");
    div.textContent = value ?? "";
    return div.innerHTML;
}

function getInitials(name) {
    if (!name) {
        return "?";
    }

    return name
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map(part => part.charAt(0).toUpperCase())
        .join("");
}

function formatMoney(amount, currency = "INR") {
    const number = Number(amount || 0);

    try {
        return new Intl.NumberFormat("en-IN", {
            style: "currency",
            currency: currency,
            minimumFractionDigits: 2
        }).format(number);
    } catch {
        return `${currency} ${number.toFixed(2)}`;
    }
}

function formatDate(dateValue) {
    if (!dateValue) {
        return "";
    }

    const date = new Date(dateValue);

    return date.toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric"
    });
}


/* =========================================================
   API HELPER
   ========================================================= */

async function apiRequest(url, options = {}) {

    const response = await fetch(`${API_BASE}${url}`, {
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        ...options
    });

    if (!response.ok) {

        let message = `Request failed (${response.status})`;

        try {
            const data = await response.json();

            message =
                data.message ||
                data.error ||
                data.detail ||
                message;

        } catch {
            // Keep default error message.
        }

        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}


/* =========================================================
   NAVIGATION
   ========================================================= */

function showSection(sectionId) {

    document.querySelectorAll(".page-section")
        .forEach(section => {
            section.classList.remove("active-section");
        });

    const section = $(sectionId);

    if (section) {
        section.classList.add("active-section");
    }

    document.querySelectorAll(".nav-item")
        .forEach(button => {
            button.classList.toggle(
                "active",
                button.dataset.section === sectionId
            );
        });

    const titles = {
        dashboardSection: "Trip Dashboard",
        participantsSection: "Participants",
        expensesSection: "Expenses",
        settlementSection: "Settlement"
    };

    $("pageTitle").textContent =
        titles[sectionId] || "TripSplit";
}


/* =========================================================
   LOAD TRIP
   ========================================================= */

async function loadTrip() {

    if (!currentTripId) {
        currentTrip = null;
        renderEmptyTrip();
        return;
    }

    try {

        currentTrip =
            await apiRequest(`/trips/${currentTripId}`);

        renderTrip();

    } catch (error) {

        console.error(error);

        currentTripId = null;
        currentTrip = null;

        localStorage.removeItem("tripsplit_trip_id");

        renderEmptyTrip();

        showToast(
            "The saved trip could not be loaded."
        );
    }
}


/* =========================================================
   EMPTY TRIP
   ========================================================= */

function renderEmptyTrip() {

    $("tripName").textContent =
        "No trip created yet";

    $("tripMeta").textContent =
        "Create a trip to start tracking expenses.";

    $("participantCount").textContent = "0";

    $("expenseCount").textContent = "0";

    $("expenseTotal").textContent = "₹0.00";

    $("settlementCount").textContent = "0";

    $("recentExpenses").innerHTML = `
        <div class="empty-state">
            <div class="empty-icon">₹</div>

            <h4>No expenses yet</h4>

            <p>
                Create a trip and add your first expense.
            </p>
        </div>
    `;

    $("dashboardParticipants").innerHTML = `
        <div class="empty-state small">
            <div class="empty-icon">♙</div>

            <h4>No participants</h4>

            <p>
                Add people to your trip.
            </p>
        </div>
    `;

    $("participantsList").innerHTML = `
        <div class="empty-state">
            <div class="empty-icon">♙</div>

            <h4>No trip yet</h4>

            <p>
                Create a trip first.
            </p>
        </div>
    `;

    $("expensesList").innerHTML = `
        <div class="empty-state">
            <div class="empty-icon">₹</div>

            <h4>No trip yet</h4>

            <p>
                Create a trip first.
            </p>
        </div>
    `;

    $("balancesList").innerHTML = "";

    $("settlementsList").innerHTML = `
        <div class="no-settlement">
            Create a trip and add expenses first.
        </div>
    `;
}


/* =========================================================
   RENDER TRIP
   ========================================================= */

function renderTrip() {

    if (!currentTrip) {
        renderEmptyTrip();
        return;
    }

    const participants =
        currentTrip.participants || [];

    const expenses =
        currentTrip.expenses || [];

    $("tripName").textContent =
        currentTrip.name;

    $("tripMeta").textContent =
        `${participants.length} participant${
            participants.length === 1 ? "" : "s"
        } • Created ${formatDate(currentTrip.createdAt)}`;

    $("participantCount").textContent =
        participants.length;

    $("expenseCount").textContent =
        expenses.length;

    const total =
        expenses.reduce(
            (sum, expense) =>
                sum + Number(expense.amount || 0),
            0
        );

    const currency =
        expenses.length > 0
            ? expenses[0].currency
            : "INR";

    $("expenseTotal").textContent =
        formatMoney(total, currency);

    renderDashboardParticipants();
    renderRecentExpenses();
    renderParticipants();
    renderExpenses();

    updateExpenseForm();
}


/* =========================================================
   DASHBOARD PARTICIPANTS
   ========================================================= */

function renderDashboardParticipants() {

    const participants =
        currentTrip?.participants || [];

    if (participants.length === 0) {

        $("dashboardParticipants").innerHTML = `
            <div class="empty-state small">
                <div class="empty-icon">♙</div>

                <h4>No participants</h4>

                <p>
                    Add people to your trip.
                </p>
            </div>
        `;

        return;
    }

    $("dashboardParticipants").innerHTML =
        participants
            .slice(0, 6)
            .map(participant => `
                <div class="person-row">

                    <div class="avatar">
                        ${escapeHtml(
                            getInitials(participant.name)
                        )}
                    </div>

                    <div class="person-info">

                        <strong>
                            ${escapeHtml(participant.name)}
                        </strong>

                        <span>
                            ${escapeHtml(participant.email)}
                        </span>

                    </div>

                </div>
            `)
            .join("");
}


/* =========================================================
   PARTICIPANTS PAGE
   ========================================================= */

function renderParticipants() {

    const participants =
        currentTrip?.participants || [];

    if (participants.length === 0) {

        $("participantsList").innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">♙</div>

                <h4>No participants yet</h4>

                <p>
                    Add everyone travelling with you.
                </p>
            </div>
        `;

        return;
    }

    $("participantsList").innerHTML =
        participants
            .map(participant => `
                <div class="participant-card">

                    <div class="avatar">
                        ${escapeHtml(
                            getInitials(participant.name)
                        )}
                    </div>

                    <h4>
                        ${escapeHtml(participant.name)}
                    </h4>

                    <p>
                        ${escapeHtml(participant.email)}
                    </p>

                </div>
            `)
            .join("");
}


/* =========================================================
   RECENT EXPENSES
   ========================================================= */

function renderRecentExpenses() {

    const expenses =
        currentTrip?.expenses || [];

    if (expenses.length === 0) {

        $("recentExpenses").innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">₹</div>

                <h4>No expenses yet</h4>

                <p>
                    Add your first expense to see it here.
                </p>
            </div>
        `;

        return;
    }

    $("recentExpenses").classList.remove("empty-state");

    $("recentExpenses").innerHTML =
        expenses
            .slice(0, 5)
            .map(expense => `
                <div class="expense-card">

                    <div class="expense-top">

                        <div>
                            <div class="expense-description">
                                ${escapeHtml(
                                    expense.description
                                )}
                            </div>

                            <div class="expense-meta">
                                Paid by
                                <strong>
                                    ${escapeHtml(
                                        expense.payerName
                                    )}
                                </strong>
                                • ${formatDate(
                                    expense.createdAt
                                )}
                            </div>
                        </div>

                        <div class="expense-amount">
                            ${formatMoney(
                                expense.amount,
                                expense.currency
                            )}
                        </div>

                    </div>

                </div>
            `)
            .join("");
}


/* =========================================================
   EXPENSES PAGE
   ========================================================= */

function renderExpenses() {

    const expenses =
        currentTrip?.expenses || [];

    if (expenses.length === 0) {

        $("expensesList").innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">₹</div>

                <h4>No expenses yet</h4>

                <p>
                    Add an expense to start tracking your trip.
                </p>
            </div>
        `;

        return;
    }

    $("expensesList").innerHTML =
        expenses
            .map(expense => {

                const sharedBy =
                    expense.sharedBy || [];

                return `
                    <div class="expense-card">

                        <div class="expense-top">

                            <div>

                                <div class="expense-description">
                                    ${escapeHtml(
                                        expense.description
                                    )}
                                </div>

                                <div class="expense-meta">
                                    Paid by
                                    <strong>
                                        ${escapeHtml(
                                            expense.payerName
                                        )}
                                    </strong>

                                    • ${formatDate(
                                        expense.createdAt
                                    )}
                                </div>

                            </div>

                            <div class="expense-amount">
                                ${formatMoney(
                                    expense.amount,
                                    expense.currency
                                )}
                            </div>

                        </div>

                        <div class="expense-shared">

                            ${sharedBy
                                .map(person => `
                                    <span class="person-chip">
                                        ${escapeHtml(
                                            person.name
                                        )}
                                    </span>
                                `)
                                .join("")}

                        </div>

                    </div>
                `;
            })
            .join("");
}


/* =========================================================
   EXPENSE FORM
   ========================================================= */

function updateExpenseForm() {

    const participants =
        currentTrip?.participants || [];

    const payerSelect =
        $("expensePayerInput");

    const participantList =
        $("expenseParticipants");

    payerSelect.innerHTML = `
        <option value="">
            Select payer
        </option>
    `;

    participantList.innerHTML = "";

    participants.forEach(participant => {

        payerSelect.innerHTML += `
            <option value="${participant.id}">
                ${escapeHtml(participant.name)}
            </option>
        `;

        participantList.innerHTML += `
            <label class="checkbox-item">

                <input
                    type="checkbox"
                    value="${participant.id}"
                    name="expenseParticipant"
                >

                <span>
                    ${escapeHtml(participant.name)}
                </span>

            </label>
        `;
    });
}


/* =========================================================
   CREATE TRIP
   ========================================================= */

async function createTrip(event) {

    event.preventDefault();

    const name =
        $("tripNameInput").value.trim();

    if (!name) {
        showToast("Enter a trip name.");
        return;
    }

    try {

        const trip =
            await apiRequest("/trips", {
                method: "POST",
                body: JSON.stringify({
                    name: name
                })
            });

        currentTripId = trip.id;

        localStorage.setItem(
            "tripsplit_trip_id",
            currentTripId
        );

        currentTrip = trip;

        closeModal("tripModal");

        $("tripForm").reset();

        await loadTrip();

        showSection("dashboardSection");

        showToast("Trip created successfully.");

    } catch (error) {

        console.error(error);

        showToast(
            error.message ||
            "Unable to create trip."
        );
    }
}


/* =========================================================
   ADD PARTICIPANT
   ========================================================= */

async function addParticipant(event) {

    event.preventDefault();

    if (!currentTripId) {

        showToast(
            "Create a trip first."
        );

        closeModal("participantModal");

        return;
    }

    const name =
        $("participantNameInput")
            .value
            .trim();

    const email =
        $("participantEmailInput")
            .value
            .trim();

    if (!name || !email) {

        showToast(
            "Enter name and email."
        );

        return;
    }

    try {

        await apiRequest(
            `/trips/${currentTripId}/participants`,
            {
                method: "POST",

                body: JSON.stringify({
                    name: name,
                    email: email
                })
            }
        );

        closeModal("participantModal");

        $("participantForm").reset();

        await loadTrip();

        showToast(
            "Participant added successfully."
        );

    } catch (error) {

        console.error(error);

        showToast(
            error.message ||
            "Unable to add participant."
        );
    }
}


/* =========================================================
   ADD EXPENSE
   ========================================================= */

async function addExpense(event) {

    event.preventDefault();

    if (!currentTripId) {

        showToast(
            "Create a trip first."
        );

        return;
    }

    const description =
        $("expenseDescriptionInput")
            .value
            .trim();

    const amount =
        Number(
            $("expenseAmountInput").value
        );

    const currency =
        $("expenseCurrencyInput").value;

    const payerId =
        $("expensePayerInput").value;

    const participantIds =
        Array.from(
            document.querySelectorAll(
                'input[name="expenseParticipant"]:checked'
            )
        )
        .map(input => input.value);

    if (!description) {

        showToast(
            "Enter an expense description."
        );

        return;
    }

    if (!amount || amount <= 0) {

        showToast(
            "Enter a valid amount."
        );

        return;
    }

    if (!payerId) {

        showToast(
            "Select who paid."
        );

        return;
    }

    if (participantIds.length === 0) {

        showToast(
            "Select at least one participant."
        );

        return;
    }

    try {

        await apiRequest(
            `/trips/${currentTripId}/expenses`,
            {
                method: "POST",

                body: JSON.stringify({
                    payerId: payerId,
                    description: description,
                    amount: amount,
                    currency: currency,
                    participantIds: participantIds
                })
            }
        );

        closeModal("expenseModal");

        $("expenseForm").reset();

        await loadTrip();

        showToast(
            "Expense added successfully."
        );

    } catch (error) {

        console.error(error);

        showToast(
            error.message ||
            "Unable to add expense."
        );
    }
}


/* =========================================================
   SETTLEMENT
   ========================================================= */

async function loadSettlement() {

    if (!currentTripId) {

        $("balancesList").innerHTML = "";

        $("settlementsList").innerHTML = `
            <div class="no-settlement">
                Create a trip first.
            </div>
        `;

        return;
    }

    try {

        const settlement =
            await apiRequest(
                `/trips/${currentTripId}/settlement`
            );

        renderSettlement(settlement);

    } catch (error) {

        console.error(error);

        showToast(
            error.message ||
            "Unable to calculate settlement."
        );
    }
}


function renderSettlement(data) {

    const balances =
        data.balances || [];

    const settlements =
        data.settlements || [];

    $("balancesList").innerHTML =
        balances
            .map(balance => {

                let netClass = "net-zero";

                if (
                    Number(balance.net) > 0
                ) {
                    netClass = "net-positive";
                } else if (
                    Number(balance.net) < 0
                ) {
                    netClass = "net-negative";
                }

                return `
                    <div class="balance-card">

                        <h4>
                            ${escapeHtml(
                                balance.participantName
                            )}
                        </h4>

                        <div class="balance-line">
                            <span>Paid</span>

                            <strong>
                                ${formatMoney(
                                    balance.paid,
                                    data.currency
                                )}
                            </strong>
                        </div>

                        <div class="balance-line">
                            <span>Share</span>

                            <strong>
                                ${formatMoney(
                                    balance.share,
                                    data.currency
                                )}
                            </strong>
                        </div>

                        <div
                            class="balance-line balance-net ${netClass}"
                        >
                            <span>Net</span>

                            <strong>
                                ${formatMoney(
                                    balance.net,
                                    data.currency
                                )}
                            </strong>
                        </div>

                    </div>
                `;
            })
            .join("");

    $("settlementCount").textContent =
        settlements.length;

    if (settlements.length === 0) {

        $("settlementsList").innerHTML = `
            <div class="no-settlement">
                Everyone is settled up.
            </div>
        `;

        return;
    }

    $("settlementsList").innerHTML =
        settlements
            .map(settlement => `
                <div class="settlement-row">

                    <div class="settlement-person">
                        ${escapeHtml(
                            settlement.fromParticipantName
                        )}
                    </div>

                    <div class="settlement-arrow">
                        →
                    </div>

                    <div class="settlement-person">
                        ${escapeHtml(
                            settlement.toParticipantName
                        )}
                    </div>

                    <div class="settlement-amount">
                        ${formatMoney(
                            settlement.amount,
                            settlement.currency
                        )}
                    </div>

                </div>
            `)
            .join("");
}


/* =========================================================
   SELECT ALL PARTICIPANTS
   ========================================================= */

function selectAllParticipants() {

    const checkboxes =
        document.querySelectorAll(
            'input[name="expenseParticipant"]'
        );

    checkboxes.forEach(
        checkbox => {
            checkbox.checked = true;
        }
    );
}


/* =========================================================
   EVENT LISTENERS
   ========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    async () => {

        /* Navigation */

        document.querySelectorAll(".nav-item")
            .forEach(button => {

                button.addEventListener(
                    "click",
                    async () => {

                        const section =
                            button.dataset.section;

                        showSection(section);

                        if (
                            section ===
                            "settlementSection"
                        ) {
                            await loadSettlement();
                        }
                    }
                );
            });


        /* New trip */

        $("newTripButton")
            .addEventListener(
                "click",
                () => {
                    openModal("tripModal");
                    $("tripNameInput").focus();
                }
            );


        /* Dashboard participant */

        $("addParticipantDashboardButton")
            .addEventListener(
                "click",
                () => {

                    if (!currentTripId) {
                        showToast(
                            "Create a trip first."
                        );
                        return;
                    }

                    openModal("participantModal");
                    $("participantNameInput").focus();
                }
            );


        /* Dashboard expense */

        $("addExpenseDashboardButton")
            .addEventListener(
                "click",
                () => {

                    if (!currentTripId) {
                        showToast(
                            "Create a trip first."
                        );
                        return;
                    }

                    if (
                        !currentTrip?.participants?.length
                    ) {
                        showToast(
                            "Add participants first."
                        );
                        return;
                    }

                    updateExpenseForm();

                    openModal("expenseModal");
                }
            );


        /* Participants page */

        $("addParticipantButton")
            .addEventListener(
                "click",
                () => {

                    if (!currentTripId) {
                        showToast(
                            "Create a trip first."
                        );
                        return;
                    }

                    openModal("participantModal");
                }
            );


        /* Expenses page */

        $("addExpenseButton")
            .addEventListener(
                "click",
                () => {

                    if (!currentTripId) {
                        showToast(
                            "Create a trip first."
                        );
                        return;
                    }

                    if (
                        !currentTrip?.participants?.length
                    ) {
                        showToast(
                            "Add participants first."
                        );
                        return;
                    }

                    updateExpenseForm();

                    openModal("expenseModal");
                }
            );


        /* Forms */

        $("tripForm")
            .addEventListener(
                "submit",
                createTrip
            );

        $("participantForm")
            .addEventListener(
                "submit",
                addParticipant
            );

        $("expenseForm")
            .addEventListener(
                "submit",
                addExpense
            );


        /* Settlement */

        $("refreshSettlementButton")
            .addEventListener(
                "click",
                loadSettlement
            );


        /* Select all */

        $("selectAllParticipantsButton")
            .addEventListener(
                "click",
                selectAllParticipants
            );


        /* View buttons */

        $("viewExpensesButton")
            .addEventListener(
                "click",
                () => {
                    showSection("expensesSection");
                }
            );

        $("viewParticipantsButton")
            .addEventListener(
                "click",
                () => {
                    showSection("participantsSection");
                }
            );


        /* Close buttons */

        document.querySelectorAll(
            "[data-close]"
        ).forEach(button => {

            button.addEventListener(
                "click",
                () => {
                    closeModal(
                        button.dataset.close
                    );
                }
            );
        });


        /* Click outside modal */

        document.querySelectorAll(
            ".modal-overlay"
        ).forEach(modal => {

            modal.addEventListener(
                "click",
                event => {

                    if (
                        event.target === modal
                    ) {
                        modal.classList.add(
                            "hidden"
                        );
                    }
                }
            );
        });


        /* Escape key */

        document.addEventListener(
            "keydown",
            event => {

                if (event.key === "Escape") {

                    document.querySelectorAll(
                        ".modal-overlay"
                    ).forEach(modal => {
                        modal.classList.add(
                            "hidden"
                        );
                    });
                }
            }
        );


        /* Load saved trip */

        await loadTrip();
    }
);