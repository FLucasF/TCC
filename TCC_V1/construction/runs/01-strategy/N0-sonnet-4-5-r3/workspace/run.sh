#!/bin/bash

# Script para executar o sistema de reservas da Pousada Maré Alta

echo "Iniciando PostgreSQL..."
docker-compose up -d

echo "Aguardando PostgreSQL ficar pronto..."
sleep 5

echo "Executando aplicação..."
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070

java -jar target/reservas-1.0.0.jar
