#!/bin/bash

# Script para executar o Sistema de Reservas da Pousada Maré Alta

# Configurações padrão (pode sobrescrever com variáveis de ambiente)
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/pousada}"
export DB_USER="${DB_USER:-postgres}"
export DB_PASSWORD="${DB_PASSWORD:-postgres}"
export PORT="${PORT:-7070}"

echo "==================================="
echo "Pousada Maré Alta - Sistema de Reservas"
echo "==================================="
echo "Banco: $DB_URL"
echo "Porta: $PORT"
echo "==================================="

# Executar o JAR
java -jar target/reservas-1.0.0.jar
