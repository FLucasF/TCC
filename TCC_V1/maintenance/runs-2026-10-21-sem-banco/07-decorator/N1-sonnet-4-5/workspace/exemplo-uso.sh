#!/bin/bash

# Exemplo de uso do sistema com as novas funcionalidades

echo "=== Iniciando o sistema em modo NORMAL ==="
java -jar target/hotel.jar &
PID=$!
sleep 3

echo ""
echo "=== Criando uma reserva ==="
RESERVA=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Costa",
    "guestEmail": "ana@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
ID=$(echo $RESERVA | grep -o '"id":[0-9]*' | grep -o '[0-9]*')
echo "Reserva criada com ID: $ID"

echo ""
echo "=== Confirmando a reserva (envia e-mail) ==="
curl -s -X POST http://localhost:7070/reservations/$ID/confirmation

echo ""
echo ""
echo "=== Consultando mensagens enviadas ==="
curl -s http://localhost:7070/messages | json_pp

echo ""
echo "Observe que foram enviadas 2 mensagens:"
echo "1. Para ana@example.com (o hóspede)"
echo "2. Para arquivo@pousadamarealta.com.br (cópia)"
echo "Ambas com status: SENT"

echo ""
echo "=== Parando o servidor ==="
kill $PID

echo ""
echo ""
echo "=== Agora testando com e-mail INVÁLIDO ==="
echo "Reiniciando o sistema..."
java -jar target/hotel.jar &
PID=$!
sleep 3

echo ""
echo "=== Criando reserva com e-mail inválido ==="
RESERVA=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 202,
    "guestName": "Bruno Silva",
    "guestEmail": "bruno@gmail",
    "guestCpf": "987.654.321-00",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
ID=$(echo $RESERVA | grep -o '"id":[0-9]*' | grep -o '[0-9]*')

echo ""
echo "=== Confirmando (tenta enviar e-mail) ==="
curl -s -X POST http://localhost:7070/reservations/$ID/confirmation

echo ""
echo ""
echo "=== Consultando mensagens ==="
curl -s http://localhost:7070/messages | json_pp

echo ""
echo "Observe que a mensagem foi BLOQUEADA (status: BLOCKED)"
echo "E não foi criada cópia para arquivo"

echo ""
echo "=== Parando o servidor ==="
kill $PID

echo ""
echo ""
echo "=== Agora em MODO DE TESTE ==="
echo "Reiniciando com HOTEL_TEST_MODE=true..."
HOTEL_TEST_MODE=true java -jar target/hotel.jar &
PID=$!
sleep 3

echo ""
echo "=== Criando e confirmando reserva ==="
RESERVA=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-08-15",
    "checkOut": "2027-08-17",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
ID=$(echo $RESERVA | grep -o '"id":[0-9]*' | grep -o '[0-9]*')
curl -s -X POST http://localhost:7070/reservations/$ID/confirmation

echo ""
echo ""
echo "=== Consultando mensagens ==="
curl -s http://localhost:7070/messages | json_pp

echo ""
echo "Em modo de teste:"
echo "- Mensagem foi para: testes@pousadamarealta.com.br"
echo "- Assunto tem prefixo: [TESTE]"
echo "- Corpo indica: Destinatário original: carla@example.com"
echo "- Não foi criada cópia para arquivo"

echo ""
echo "=== Parando o servidor ==="
kill $PID

echo ""
echo "Demonstração concluída!"
