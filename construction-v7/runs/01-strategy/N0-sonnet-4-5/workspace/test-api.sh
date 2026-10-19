#!/bin/bash

# Inicia o servidor em background
java -jar target/reservas-1.0.0.jar &
SERVER_PID=$!
echo "Servidor iniciado com PID $SERVER_PID"

# Aguarda o servidor iniciar
sleep 5

# Testa GET /rooms
echo "=== Testando GET /rooms ==="
curl -s http://localhost:7070/rooms | jq .

# Cria uma reserva STANDARD
echo -e "\n=== Criando reserva STANDARD ==="
RESERVATION_1=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Test Guest",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
echo "$RESERVATION_1" | jq .

# Cria uma reserva CORPORATE
echo -e "\n=== Criando reserva CORPORATE (Tecnosul) ==="
RESERVATION_2=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }')
echo "$RESERVATION_2" | jq .

# Cria uma reserva WEEKEND_PACKAGE
echo -e "\n=== Criando reserva WEEKEND_PACKAGE ==="
RESERVATION_3=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Weekend Guest",
    "guestEmail": "weekend@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }')
echo "$RESERVATION_3" | jq .

# Cria uma reserva LONG_STAY
echo -e "\n=== Criando reserva LONG_STAY ==="
RESERVATION_4=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Long Stay Guest",
    "guestEmail": "longstay@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 2,
    "ratePlan": "LONG_STAY"
  }')
echo "$RESERVATION_4" | jq .

# Confirma a primeira reserva
echo -e "\n=== Confirmando reserva 1 ==="
curl -s -X POST http://localhost:7070/reservations/1/confirmation | jq .

# Testa GET /messages para ver o e-mail enviado
echo -e "\n=== Verificando mensagens enviadas ==="
curl -s http://localhost:7070/messages | jq .

# Cancela a reserva 3 e verifica reembolso
echo -e "\n=== Confirmando reserva 3 ==="
curl -s -X POST http://localhost:7070/reservations/3/confirmation | jq .

echo -e "\n=== Cancelando reserva 3 (deve reembolsar 50%) ==="
curl -s -X POST http://localhost:7070/reservations/3/cancellation | jq .

# Mata o servidor
echo -e "\n=== Parando servidor ==="
kill $SERVER_PID
