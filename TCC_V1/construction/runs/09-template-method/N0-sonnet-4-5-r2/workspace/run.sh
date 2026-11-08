#!/bin/bash

# Script para executar o sistema Pousada Maré Alta

# Configurações padrão
DB_URL=${DB_URL:-"jdbc:postgresql://localhost:5432/pousada"}
DB_USER=${DB_USER:-"pousada"}
DB_PASSWORD=${DB_PASSWORD:-"pousada123"}
PORT=${PORT:-7070}

# Build se necessário
if [ ! -f "target/reservas-1.0.0.jar" ]; then
    echo "Building project..."
    mvn clean package -DskipTests
fi

# Executar
echo "Starting Pousada Maré Alta..."
echo "Database: $DB_URL"
echo "Port: $PORT"
echo ""

DB_URL="$DB_URL" \
DB_USER="$DB_USER" \
DB_PASSWORD="$DB_PASSWORD" \
PORT="$PORT" \
java -jar target/reservas-1.0.0.jar
