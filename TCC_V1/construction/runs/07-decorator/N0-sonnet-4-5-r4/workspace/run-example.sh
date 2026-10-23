#!/bin/bash

# Script de exemplo para executar o sistema
# Certifique-se de que o PostgreSQL está rodando

export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="pousada123"
export PORT="7070"
# Descomente a linha abaixo para ativar o modo de teste
# export HOTEL_TEST_MODE="true"

echo "Iniciando o sistema de reservas da Pousada Maré Alta..."
echo "Porta: $PORT"
echo "Banco de dados: $DB_URL"

java -jar target/reservas-1.0.0.jar
