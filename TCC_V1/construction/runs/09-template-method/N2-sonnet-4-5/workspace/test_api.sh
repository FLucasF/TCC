#!/bin/bash

echo "Iniciando testes da API..."

# Inicia o servidor em background
java -jar target/reservations-1.0-SNAPSHOT.jar &
SERVER_PID=$!
sleep 3

echo "1. Listando quartos..."
curl -s http://localhost:7070/rooms | jq -r '.[] | "\(.number) - \(.type) - R$ \(.nightlyRate)"'

echo -e "\n2. Criando reserva..."
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION" | jq .
RESERVATION_ID=$(echo "$RESERVATION" | jq -r .id)

echo -e "\n3. Confirmando reserva..."
curl -s -X POST "http://localhost:7070/reservations/$RESERVATION_ID/confirmation" | jq .

echo -e "\n4. Verificando mensagens enviadas..."
curl -s http://localhost:7070/messages | jq -r '.[0] | "Para: \(.to)\nAssunto: \(.subject)"'

echo -e "\n5. Gerando relatório de ocupação para 2027-03-10..."
curl -s "http://localhost:7070/reports/occupancy?date=2027-03-10"

echo -e "\n6. Gerando relatório financeiro para 2027-03..."
curl -s "http://localhost:7070/reports/revenue?month=2027-03"

echo -e "\n7. Gerando relatório de taxa de turismo para 2027-03..."
curl -s "http://localhost:7070/reports/tourism-tax?month=2027-03"

echo -e "\n8. Gerando relatório de planos de tarifa para 2027-03..."
curl -s "http://localhost:7070/reports/rate-plans?month=2027-03"

echo -e "\nFinalizando servidor..."
kill $SERVER_PID
wait $SERVER_PID 2>/dev/null

echo "Testes concluídos!"
