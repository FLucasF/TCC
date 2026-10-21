#!/bin/bash

set -e

echo "=== Testando Sistema de Reservas da Pousada Maré Alta ==="
echo

# Inicia o servidor em background
echo "Iniciando servidor..."
PORT=7070 java -jar target/reservas-1.0.0.jar &
SERVER_PID=$!

# Aguarda o servidor iniciar
sleep 3

# Função para fazer requisições
function test_endpoint() {
    echo "[$1] $2"
    shift 2
    curl -s "$@"
    echo
    echo
}

# Testa GET /rooms
test_endpoint "✓" "Listando quartos" \
    http://localhost:7070/rooms | jq '.'

# Cria uma reserva
echo "[✓] Criando reserva"
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
    -H "Content-Type: application/json" \
    -d '{
        "roomNumber": "101",
        "guestName": "Ana Silva",
        "guestEmail": "ana@example.com",
        "guestCpf": "12345678901",
        "checkIn": "2027-03-10",
        "checkOut": "2027-03-12",
        "guests": 2,
        "ratePlan": "STANDARD"
    }')
echo "$RESERVATION" | jq '.'
RESERVATION_ID=$(echo "$RESERVATION" | jq -r '.id')
echo

# Confirma a reserva
echo "[✓] Confirmando reserva $RESERVATION_ID"
curl -s -X POST "http://localhost:7070/reservations/$RESERVATION_ID/confirmation" | jq '.'
echo

# Verifica mensagens enviadas
echo "[✓] Verificando mensagens enviadas"
curl -s http://localhost:7070/messages | jq '.'
echo

# Cria reserva com e-mail inválido
echo "[✓] Testando e-mail inválido (deve ser bloqueado)"
curl -s -X POST http://localhost:7070/reservations \
    -H "Content-Type: application/json" \
    -d '{
        "roomNumber": "102",
        "guestName": "João Santos",
        "guestEmail": "joao.gmail",
        "guestCpf": "98765432100",
        "checkIn": "2027-04-10",
        "checkOut": "2027-04-12",
        "guests": 2,
        "ratePlan": "STANDARD"
    }' | jq -r '.id' | xargs -I {} \
    curl -s -X POST "http://localhost:7070/reservations/{}/confirmation" >/dev/null 2>&1

echo "Mensagens após confirmação com e-mail inválido:"
curl -s http://localhost:7070/messages | jq '.[] | select(.to == "joao.gmail")'
echo

# Testa relatório de ocupação
echo "[✓] Gerando relatório de ocupação para 2027-03-10"
curl -s "http://localhost:7070/reports/occupancy?date=2027-03-10"
echo
echo

# Para o servidor
echo "Parando servidor..."
kill $SERVER_PID
wait $SERVER_PID 2>/dev/null || true

echo
echo "=== Testes concluídos com sucesso! ==="
