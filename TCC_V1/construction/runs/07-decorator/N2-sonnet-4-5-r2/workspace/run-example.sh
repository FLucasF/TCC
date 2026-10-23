#!/bin/bash

# Script de exemplo para executar o sistema localmente com PostgreSQL em Docker

# 1. Inicia PostgreSQL em container Docker
echo "Iniciando PostgreSQL..."
docker run -d --name pousada-db \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=pousada \
  -e POSTGRES_PASSWORD=pousada123 \
  -p 5432:5432 \
  postgres:18

# Aguarda o PostgreSQL iniciar
sleep 5

# 2. Executa o sistema
echo "Iniciando o sistema..."
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="pousada123"
export PORT=7070
export HOTEL_TEST_MODE=true

java -jar target/hotel-reservation-system-1.0.0.jar

# Para parar o container:
# docker stop pousada-db
# docker rm pousada-db
