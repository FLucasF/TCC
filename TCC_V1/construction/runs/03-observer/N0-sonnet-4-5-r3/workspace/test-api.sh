#!/bin/bash

# Script de teste para a API da Pousada Maré Alta
# Demonstra o sistema de notificações extensível

BASE_URL="http://localhost:7070"

echo "======================================"
echo "Pousada Maré Alta - Teste da API"
echo "======================================"
echo ""

echo "1. Listando quartos disponíveis..."
curl -s "$BASE_URL/rooms" | jq .
echo ""
echo ""

echo "2. Criando uma reserva..."
RESERVATION=$(curl -s -X POST "$BASE_URL/reservations" \
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

RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')
echo "Reserva criada com ID: $RESERVATION_ID"
echo "$RESERVATION" | jq .
echo ""
echo ""

echo "3. Confirmando a reserva (dispara notificações)..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION_ID/confirmation" | jq .
echo ""
echo ""

echo "4. Verificando mensagens enviadas..."
echo "IMPORTANTE: Sistema extensível - 3 notificações automáticas:"
echo "  - Hóspede: comprovante"
echo "  - Governança: preparar quarto"
echo "  - Financeiro: pagamento recebido"
echo ""
curl -s "$BASE_URL/messages" | jq .
echo ""
echo ""

echo "5. Criando reserva não reembolsável em alta temporada..."
RESERVATION2=$(curl -s -X POST "$BASE_URL/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')

RESERVATION2_ID=$(echo "$RESERVATION2" | jq -r '.id')
echo "Reserva criada com ID: $RESERVATION2_ID"
echo "Note o cálculo: alta temporada (20%) + desconto não reembolsável (15%)"
echo "$RESERVATION2" | jq .
echo ""
echo ""

echo "6. Confirmando a segunda reserva..."
curl -s -X POST "$BASE_URL/reservations/$RESERVATION2_ID/confirmation" | jq .
echo ""
echo ""

echo "7. Cancelando a reserva não reembolsável..."
echo "Sistema extensível - 1 notificação (governança):"
echo "  - Governança: datas liberadas"
echo "  - Financeiro: NÃO notificado (sem reembolso)"
echo ""
curl -s -X POST "$BASE_URL/reservations/$RESERVATION2_ID/cancellation" | jq .
echo ""
echo ""

echo "8. Total de mensagens agora (deve ter +4 = 7 total)..."
MSG_COUNT=$(curl -s "$BASE_URL/messages" | jq 'length')
echo "Total de mensagens: $MSG_COUNT"
echo ""
echo ""

echo "9. Relatório de ocupação para 2027-03-10..."
curl -s "$BASE_URL/reports/occupancy?date=2027-03-10"
echo ""
echo ""

echo "10. Relatório financeiro de março/2027..."
curl -s "$BASE_URL/reports/revenue?month=2027-03"
echo ""
echo ""

echo "======================================"
echo "Testes concluídos!"
echo "======================================"
echo ""
echo "ARQUITETURA DE NOTIFICAÇÕES:"
echo "Para adicionar novos destinatários (ex: restaurante, transfer):"
echo "  1. Criar classe: RestaurantNotificationListener implements EventListener"
echo "  2. Registrar no Main.java: eventPublisher.subscribe(..., new RestaurantNotificationListener(...))"
echo "  3. Pronto! Sem modificar código existente."
