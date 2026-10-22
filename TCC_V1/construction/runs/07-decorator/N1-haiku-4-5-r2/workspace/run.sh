#!/bin/bash

# Script para rodar a Pousada Maré Alta

# Configurar variáveis de ambiente padrão se não estiverem definidas
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/pousada}"
export DB_USER="${DB_USER:-pousada}"
export DB_PASSWORD="${DB_PASSWORD:-pousada}"
export PORT="${PORT:-7070}"

echo "Starting Pousada Maré Alta"
echo "Database: $DB_URL"
echo "Port: $PORT"
echo "Test Mode: $HOTEL_TEST_MODE"

java -jar target/app.jar
