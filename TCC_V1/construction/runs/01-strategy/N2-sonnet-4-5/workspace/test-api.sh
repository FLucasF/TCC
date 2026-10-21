#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Teste 1: Criação de reserva STANDARD em alta temporada (10-12/07/2027) ==="
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq '{id, subtotal, discount, total, nights}'

echo ""
echo "=== Teste 2: Reserva CORPORATE com TECNOSUL (10-12/03/2027) ==="
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
  }' | jq '{id, subtotal, discount, total}'

echo ""
echo "=== Teste 3: WEEKEND_PACKAGE no DELUXE (sexta 12/03 a domingo 14/03/2027) ==="
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "Pedro Oliveira",
    "guestEmail": "pedro@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 3,
    "ratePlan": "WEEKEND_PACKAGE"
  }' | jq '{id, subtotal, discount, total}'

echo ""
echo "=== Teste 4: LONG_STAY no STANDARD (01-08/03/2027) ==="
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
  }' | jq '{id, subtotal, discount, total}'

echo ""
echo "=== Teste 5: Confirmar reserva 3 (weekend package) e verificar mensagem ==="
curl -s -X POST "$BASE_URL/reservations/3/confirmation" | jq '{id, status, amountPaid}'

echo ""
echo "=== Teste 6: Cancelar reserva 3 e verificar reembolso (50%) ==="
curl -s -X POST "$BASE_URL/reservations/3/cancellation" | jq '{id, status, amountPaid, refund}'

echo ""
echo "=== Teste 7: Verificar e-mail enviado ==="
curl -s "$BASE_URL/messages" | jq '.[0] | {channel, to, subject}'

echo ""
echo "=== Teste 8: Obter comprovante da reserva 3 ==="
curl -s "$BASE_URL/reservations/3/document" | jq '{title, reservationId, total}'

echo ""
echo "=== Teste 9: Relatório de ocupação para 12/03/2027 ==="
curl -s "$BASE_URL/reports/occupancy?date=2027-03-12" | head -5

echo ""
echo "=== Teste 10: Relatório financeiro de março/2027 ==="
curl -s "$BASE_URL/reports/revenue?month=2027-03" | head -5

echo ""
echo "=== Teste 11: Erro - CORPORATE sem companyCode ==="
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "Carlos Lima",
    "guestEmail": "carlos@example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 2,
    "ratePlan": "CORPORATE"
  }' | jq '{error, message}'

echo ""
echo "=== Teste 12: Erro - WEEKEND_PACKAGE em dia errado ==="
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "Carlos Lima",
    "guestEmail": "carlos@example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }' | jq '{error, message}'

echo ""
echo "=== Teste 13: Erro - LONG_STAY com menos de 7 noites ==="
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "Carlos Lima",
    "guestEmail": "carlos@example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-07",
    "guests": 2,
    "ratePlan": "LONG_STAY"
  }' | jq '{error, message}'

echo ""
echo "=== Todos os testes concluídos ==="
