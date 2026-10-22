#!/bin/bash

# Script de teste da API da Pousada Maré Alta

BASE_URL="http://localhost:7070"

echo "=== Testando GET /rooms ==="
curl -s "$BASE_URL/rooms" | jq .

echo ""
echo "=== Testando POST /reservations (CPF) ==="
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

echo "$RESERVATION" | jq .

RESERVATION_ID=$(echo "$RESERVATION" | jq .id)
echo "Reservation ID: $RESERVATION_ID"

echo ""
echo "=== Testando GET /reservations/{id} ==="
curl -s "$BASE_URL/reservations/$RESERVATION_ID" | jq .

echo ""
echo "=== Testando POST /reservations/{id}/confirmation ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .

echo ""
echo "=== Testando GET /messages ==="
curl -s "$BASE_URL/messages" | jq .

echo ""
echo "=== Testando POST /reservations (CNPJ) ==="
RESERVATION2=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Ana Souza",
    "guestEmail": "ana@example.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

echo "$RESERVATION2" | jq .

RESERVATION2_ID=$(echo "$RESERVATION2" | jq .id)

echo ""
echo "=== Testando POST /reservations/{id}/confirmation (CNPJ) ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION2_ID/confirmation" | jq .

echo ""
echo "=== Testando POST /reservations (Passport) ==="
RESERVATION3=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')

echo "$RESERVATION3" | jq .

RESERVATION3_ID=$(echo "$RESERVATION3" | jq .id)

echo ""
echo "=== Testando POST /reservations/{id}/confirmation (Passport) ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION3_ID/confirmation" | jq .

echo ""
echo "=== Testando POST /reservations/{id}/check-in ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION3_ID/check-in" | jq .

echo ""
echo "=== Testando POST /reservations/{id}/check-out ==="
curl -s -X POST "$BASE_URL/reservations/$RESERVATION3_ID/check-out" | jq .

echo ""
echo "=== Testando GET /reservations/{id}/document ==="
curl -s "$BASE_URL/reservations/$RESERVATION3_ID/document" | jq .

echo ""
echo "=== Testando GET /reports/occupancy ==="
curl -s "$BASE_URL/reports/occupancy?date=2027-07-10"

echo ""
echo "=== Testando GET /reports/revenue ==="
curl -s "$BASE_URL/reports/revenue?month=2027-07"
