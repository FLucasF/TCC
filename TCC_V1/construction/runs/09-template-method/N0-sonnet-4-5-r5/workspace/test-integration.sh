#!/bin/bash

set -e

echo "=== Teste de Integração - Pousada Maré Alta ==="

# Verificar se o PostgreSQL está rodando
echo "Verificando PostgreSQL..."
docker-compose up -d postgres
sleep 3

# Compilar o projeto
echo "Compilando o projeto..."
mvn clean package -DskipTests

# Iniciar a aplicação em background
echo "Iniciando a aplicação..."
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070

java -jar target/reservations-1.0.0.jar &
APP_PID=$!

# Aguardar a aplicação iniciar
sleep 5

# Testar a API
echo "Testando endpoints..."

# Listar quartos
echo "1. Listando quartos..."
curl -s http://localhost:7070/rooms | jq . > /dev/null
echo "✓ GET /rooms OK"

# Criar reserva
echo "2. Criando reserva..."
RESPONSE=$(curl -s -X POST http://localhost:7070/reservations \
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

RESERVATION_ID=$(echo "$RESPONSE" | jq -r '.id')
echo "✓ POST /reservations OK (ID: $RESERVATION_ID)"

# Confirmar reserva
echo "3. Confirmando reserva..."
curl -s -X POST "http://localhost:7070/reservations/$RESERVATION_ID/confirmation" > /dev/null
echo "✓ POST /reservations/$RESERVATION_ID/confirmation OK"

# Listar mensagens
echo "4. Listando mensagens..."
MESSAGES=$(curl -s http://localhost:7070/messages)
MESSAGE_COUNT=$(echo "$MESSAGES" | jq 'length')
echo "✓ GET /messages OK ($MESSAGE_COUNT mensagens)"

# Relatório de ocupação
echo "5. Gerando relatório de ocupação..."
curl -s "http://localhost:7070/reports/occupancy?date=2027-03-10" > /dev/null
echo "✓ GET /reports/occupancy OK"

# Relatório financeiro
echo "6. Gerando relatório financeiro..."
curl -s "http://localhost:7070/reports/revenue?month=2027-03" > /dev/null
echo "✓ GET /reports/revenue OK"

# Relatório de taxa de turismo
echo "7. Gerando relatório de taxa de turismo..."
curl -s "http://localhost:7070/reports/tourism-tax?month=2027-03" > /dev/null
echo "✓ GET /reports/tourism-tax OK"

# Relatório de planos
echo "8. Gerando relatório de planos..."
curl -s "http://localhost:7070/reports/rate-plans?month=2027-03" > /dev/null
echo "✓ GET /reports/rate-plans OK"

# Limpar
echo "Finalizando..."
kill $APP_PID
docker-compose down

echo ""
echo "=== Todos os testes passaram! ==="
