#!/bin/bash

# Script de inicialização da Pousada Maré Alta
# Ajuste as variáveis conforme seu ambiente

# Configuração do banco de dados
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/pousada}"
export DB_USER="${DB_USER:-postgres}"
export DB_PASSWORD="${DB_PASSWORD:-postgres}"

# Porta do servidor (opcional)
export PORT="${PORT:-7070}"

echo "================================================"
echo "  Pousada Maré Alta - Sistema de Reservas"
echo "================================================"
echo ""
echo "Configuração:"
echo "  Database: $DB_URL"
echo "  User: $DB_USER"
echo "  Port: $PORT"
echo ""
echo "Iniciando servidor..."
echo ""

# Inicia o servidor
java -jar target/pousada-mare-alta-1.0.0.jar
