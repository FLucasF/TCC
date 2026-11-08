#!/bin/bash

# Script de exemplo para testar o Sistema de Reservas da Pousada Maré Alta
# Certifique-se de que o servidor está rodando em http://localhost:7070

BASE_URL="http://localhost:7070"

echo "=== Pousada Maré Alta - Exemplos de API ==="
echo ""

# 1. Listar quartos
echo "1. Listando quartos disponíveis..."
curl -s "$BASE_URL/rooms" | jq .
echo ""

# 2. Criar reserva STANDARD
echo "2. Criando reserva STANDARD (João Silva)..."
RESERVATION_1=$(curl -s -X POST "$BASE_URL/reservations" \
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
  }')
echo "$RESERVATION_1" | jq .
ID_1=$(echo "$RESERVATION_1" | jq -r .id)
echo ""

# 3. Confirmar reserva
echo "3. Confirmando reserva $ID_1..."
curl -s -X POST "$BASE_URL/reservations/$ID_1/confirmation" | jq .
echo ""

# 4. Criar reserva NON_REFUNDABLE
echo "4. Criando reserva NON_REFUNDABLE (Maria Santos)..."
RESERVATION_2=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')
echo "$RESERVATION_2" | jq .
ID_2=$(echo "$RESERVATION_2" | jq -r .id)
echo ""

# 5. Confirmar e cancelar reserva não reembolsável
echo "5. Confirmando e cancelando reserva não reembolsável..."
curl -s -X POST "$BASE_URL/reservations/$ID_2/confirmation" | jq .
echo ""
curl -s -X POST "$BASE_URL/reservations/$ID_2/cancellation" | jq .
echo ""

# 6. Criar reserva CORPORATE
echo "6. Criando reserva CORPORATE Tecnosul (Ana Costa)..."
RESERVATION_3=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "Ana Costa",
    "guestEmail": "ana@example.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }')
echo "$RESERVATION_3" | jq .
ID_3=$(echo "$RESERVATION_3" | jq -r .id)
echo ""

# 7. Criar reserva WEEKEND_PACKAGE
echo "7. Criando reserva WEEKEND_PACKAGE (Pedro Souza)..."
RESERVATION_4=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Pedro Souza",
    "guestEmail": "pedro@example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 3,
    "ratePlan": "WEEKEND_PACKAGE"
  }')
echo "$RESERVATION_4" | jq .
ID_4=$(echo "$RESERVATION_4" | jq -r .id)
echo ""

# 8. Criar reserva LONG_STAY
echo "8. Criando reserva LONG_STAY (Lucia Ferreira)..."
RESERVATION_5=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Lucia Ferreira",
    "guestEmail": "lucia@example.com",
    "guestCpf": "44455566677",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 2,
    "ratePlan": "LONG_STAY"
  }')
echo "$RESERVATION_5" | jq .
ID_5=$(echo "$RESERVATION_5" | jq -r .id)
echo ""

# 9. Teste de alta temporada
echo "9. Criando reserva em alta temporada - Julho (Carlos Lima)..."
RESERVATION_6=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Carlos Lima",
    "guestEmail": "carlos@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_6" | jq .
ID_6=$(echo "$RESERVATION_6" | jq -r .id)
echo ""

# 10. Confirmar algumas reservas
echo "10. Confirmando reservas..."
curl -s -X POST "$BASE_URL/reservations/$ID_3/confirmation" > /dev/null
curl -s -X POST "$BASE_URL/reservations/$ID_4/confirmation" > /dev/null
curl -s -X POST "$BASE_URL/reservations/$ID_5/confirmation" > /dev/null
curl -s -X POST "$BASE_URL/reservations/$ID_6/confirmation" > /dev/null
echo "Reservas $ID_3, $ID_4, $ID_5, $ID_6 confirmadas"
echo ""

# 11. Check-in e Check-out
echo "11. Fazendo check-in da reserva $ID_1..."
curl -s -X POST "$BASE_URL/reservations/$ID_1/check-in" | jq .
echo ""

echo "12. Fazendo check-out da reserva $ID_1..."
curl -s -X POST "$BASE_URL/reservations/$ID_1/check-out" | jq .
echo ""

# 13. Segunda via do comprovante
echo "13. Solicitando segunda via do comprovante da reserva $ID_3..."
curl -s "$BASE_URL/reservations/$ID_3/document" | jq .
echo ""

# 14. Listar mensagens enviadas
echo "14. Listando mensagens enviadas (e-mails)..."
curl -s "$BASE_URL/messages" | jq .
echo ""

# 15. Relatório de ocupação
echo "15. Relatório de ocupação para 10/03/2027..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""

# 16. Relatório financeiro
echo "16. Relatório financeiro de Março/2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""

# 17. Teste de erro - Quarto já reservado
echo "17. Testando conflito de reserva (deve falhar)..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Conflito Test",
    "guestEmail": "conflito@example.com",
    "guestCpf": "00000000000",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-13",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
echo ""

# 18. Teste de erro - Capacidade excedida
echo "18. Testando capacidade excedida (deve falhar)..."
curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Many Guests",
    "guestEmail": "many@example.com",
    "guestCpf": "33333333333",
    "checkIn": "2027-06-01",
    "checkOut": "2027-06-02",
    "guests": 5,
    "ratePlan": "STANDARD"
  }' | jq .
echo ""

echo "=== Testes concluídos ==="
