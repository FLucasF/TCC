#!/bin/bash

echo "=== Demo do Sistema de Notificações - Pousada Maré Alta ==="
echo

# Inicia o servidor
echo "Iniciando servidor..."
java -jar target/pousada-mare-alta-1.0.0.jar > /dev/null 2>&1 &
SERVER_PID=$!
sleep 3

echo "✓ Servidor iniciado na porta 7070"
echo

# Cria uma reserva
echo "1. Criando reserva para o quarto 201 (DELUXE)..."
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "room_number": 201,
    "guest_name": "Carla Dias",
    "guest_email": "carla@example.com",
    "guest_cpf": "11122233344",
    "check_in": "2027-03-10",
    "check_out": "2027-03-12",
    "guests": 2,
    "rate_plan": "STANDARD"
  }')

echo "$RESERVATION" | python3 -m json.tool
echo

# Confirma a reserva (dispara as notificações)
echo "2. Confirmando a reserva (aqui o Observer entra em ação!)..."
curl -s -X POST http://localhost:7070/reservations/1/confirmation > /dev/null
echo "✓ Reserva confirmada"
echo

# Lista as mensagens enviadas
echo "3. Mensagens enviadas (3 notificações via Observer):"
echo
MESSAGES=$(curl -s http://localhost:7070/messages)
echo "$MESSAGES" | python3 -c "
import sys, json
messages = json.load(sys.stdin)
for i, msg in enumerate(messages, 1):
    print(f'   Mensagem {i}:')
    print(f'     Para: {msg[\"to\"]}')
    print(f'     Assunto: {msg[\"subject\"]}')
    print()
"

# Cancela a reserva
echo "4. Cancelando a reserva confirmada..."
curl -s -X POST http://localhost:7070/reservations/1/cancellation > /dev/null
echo "✓ Reserva cancelada"
echo

# Lista as novas mensagens
echo "5. Novas mensagens enviadas (2 notificações via Observer):"
echo
MESSAGES=$(curl -s http://localhost:7070/messages)
echo "$MESSAGES" | python3 -c "
import sys, json
messages = json.load(sys.stdin)
for i, msg in enumerate(messages[3:], 4):
    print(f'   Mensagem {i}:')
    print(f'     Para: {msg[\"to\"]}')
    print(f'     Assunto: {msg[\"subject\"]}')
    print()
"

echo "=== Observer Pattern em Ação ==="
echo
echo "Quando a reserva foi CONFIRMADA, 3 listeners foram notificados:"
echo "  • GuestNotifier → enviou e-mail para o hóspede"
echo "  • HousekeepingNotifier → enviou e-mail para a governança"
echo "  • FinanceNotifier → enviou e-mail para o financeiro"
echo
echo "Quando a reserva foi CANCELADA, 2 listeners foram notificados:"
echo "  • HousekeepingNotifier → avisou que o quarto ficou livre"
echo "  • FinanceNotifier → avisou sobre o reembolso"
echo
echo "Para adicionar um novo interessado (ex: RestaurantNotifier):"
echo "  1. Criar a classe implementando ReservationListener"
echo "  2. Adicionar à lista de listeners em Main.java"
echo "  3. Pronto! Nenhuma mudança no ReservationService"
echo

# Encerra o servidor
kill $SERVER_PID 2>/dev/null
echo "✓ Servidor encerrado"
