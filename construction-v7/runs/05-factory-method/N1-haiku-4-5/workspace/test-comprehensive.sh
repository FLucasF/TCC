#!/bin/bash

API="http://localhost:7070"

echo "=== Comprehensive Pousada Maré Alta Tests ==="
echo

# Setup: Create multiple reservations for testing
echo "Setup: Creating multiple reservations"
echo

# Reservation 1: STANDARD, 2 nights, July (high season), will be CHECKED_OUT
echo "1. STANDARD room, July (high season), 2 nights - will checkout"
R1=$(curl -s -X POST "$API/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Guest One",
    "guestEmail": "one@example.com",
    "guestCpf": "111.111.111-11",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq '.id')
echo "Reservation: $R1"

# Reservation 2: DELUXE, 3 nights, January (high season), STANDARD plan, will cancel
echo "2. DELUXE room, January (high season), 3 nights, STANDARD - will cancel"
R2=$(curl -s -X POST "$API/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 202,
    "guestName": "Guest Two",
    "guestEmail": "two@example.com",
    "guestCpf": "222.222.222-22",
    "checkIn": "2027-01-15",
    "checkOut": "2027-01-18",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq '.id')
echo "Reservation: $R2"

# Reservation 3: SUITE, 2 nights, July (high season), NON_REFUNDABLE, will cancel
echo "3. SUITE room, July (high season), 2 nights, NON_REFUNDABLE - will cancel"
R3=$(curl -s -X POST "$API/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Guest Three",
    "guestEmail": "three@example.com",
    "guestCpf": "333.333.333-33",
    "checkIn": "2027-07-15",
    "checkOut": "2027-07-17",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq '.id')
echo "Reservation: $R3"

echo
echo "--- Confirming reservations ---"
curl -s -X POST "$API/reservations/$R1/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}' | jq '{id: .id, status: .status, amountPaid: .amountPaid, total: .total}'

curl -s -X POST "$API/reservations/$R2/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 1169.70}' | jq '{id: .id, status: .status, amountPaid: .amountPaid, total: .total}'

curl -s -X POST "$API/reservations/$R3/confirmation" \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 1847.04}' | jq '{id: .id, status: .status, amountPaid: .amountPaid, total: .total}'

echo
echo "--- Testing cancellations ---"
echo "Cancelling R2 (STANDARD plan - full refund)"
curl -s -X POST "$API/reservations/$R2/cancellation" | jq '{id: .id, status: .status, amountPaid: .amountPaid, refund: .refund}'

echo "Cancelling R3 (NON_REFUNDABLE - no refund)"
curl -s -X POST "$API/reservations/$R3/cancellation" | jq '{id: .id, status: .status, amountPaid: .amountPaid, refund: .refund}'

echo
echo "--- Testing check-in/check-out ---"
curl -s -X POST "$API/reservations/$R1/check-in" | jq '{id: .id, status: .status}'

echo "Checking out R1..."
curl -s -X POST "$API/reservations/$R1/check-out" | jq -r '.text' | head -5

echo
echo "--- Testing occupancy report ---"
echo "Occupancy for 2027-07-10:"
curl -s "$API/reports/occupancy?date=2027-07-10"

echo "Occupancy for 2027-07-15:"
curl -s "$API/reports/occupancy?date=2027-07-15"

echo
echo "--- Testing revenue report ---"
echo "Revenue for July 2027 (should include checked out R1, cancelled R3):"
curl -s "$API/reports/revenue?month=2027-07"

echo "Revenue for January 2027 (should include cancelled R2):"
curl -s "$API/reports/revenue?month=2027-01"

echo
echo "--- Testing validation errors ---"
echo "Invalid: Capacity exceeded"
curl -s -X POST "$API/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "444.444.444-44",
    "checkIn": "2027-09-01",
    "checkOut": "2027-09-03",
    "guests": 5,
    "ratePlan": "STANDARD"
  }' | jq '{error: .error, message: .message}'

echo "Invalid: No guests"
curl -s -X POST "$API/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "444.444.444-44",
    "checkIn": "2027-09-01",
    "checkOut": "2027-09-03",
    "guests": 0,
    "ratePlan": "STANDARD"
  }' | jq '{error: .error, message: .message}'

echo "Invalid: Checkout before checkin"
curl -s -X POST "$API/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "444.444.444-44",
    "checkIn": "2027-09-03",
    "checkOut": "2027-09-01",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq '{error: .error, message: .message}'

echo
echo "=== All comprehensive tests complete ==="
