#!/bin/bash

# Script de teste da API da Pousada Maré Alta

BASE_URL="http://localhost:7070"

echo "=== Listando quartos ==="
curl -s "$BASE_URL/rooms" | jq .

echo -e "\n=== Criando reserva STANDARD ==="
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .

echo -e "\n=== Criando reserva CORPORATE (Tecnosul) ==="
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }' | jq .

echo -e "\n=== Criando reserva WEEKEND_PACKAGE ==="
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "Carlos Oliveira",
    "guestEmail": "carlos@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }' | jq .

echo -e "\n=== Criando reserva LONG_STAY ==="
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Ana Costa",
    "guestEmail": "ana@example.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }' | jq .

echo -e "\n=== Confirmando reserva 1 ==="
curl -s -X POST "$BASE_URL/reservations/1/confirmation" | jq .

echo -e "\n=== Consultando mensagens enviadas ==="
curl -s "$BASE_URL/messages" | jq .

echo -e "\n=== Consultando reserva 1 ==="
curl -s "$BASE_URL/reservations/1" | jq .

echo -e "\n=== Obtendo documento da reserva 1 ==="
curl -s "$BASE_URL/reservations/1/document" | jq .

echo -e "\n=== Fazendo check-in da reserva 1 ==="
curl -s -X POST "$BASE_URL/reservations/1/check-in" | jq .

echo -e "\n=== Fazendo check-out da reserva 1 ==="
curl -s -X POST "$BASE_URL/reservations/1/check-out" | jq .

echo -e "\n=== Relatório de ocupação para 10/03/2027 ==="
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"

echo -e "\n=== Relatório financeiro de março/2027 ==="
curl -s "$BASE_URL/reports/revenue?month=2027-03"
