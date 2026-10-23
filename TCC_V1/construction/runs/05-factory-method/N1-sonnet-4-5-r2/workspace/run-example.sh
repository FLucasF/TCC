#!/bin/bash

# Script de exemplo para executar o sistema
# Ajuste as variáveis conforme seu ambiente

export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
export PORT=7070

echo "Iniciando Pousada Maré Alta..."
echo "Porta: $PORT"
echo "Banco: $DB_URL"
echo ""

java -jar target/mare-alta-1.0.0.jar
