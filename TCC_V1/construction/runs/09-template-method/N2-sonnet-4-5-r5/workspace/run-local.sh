#!/bin/bash

export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070

echo "Iniciando Sistema de Reservas da Pousada Maré Alta..."
echo "Banco de dados: $DB_URL"
echo "Porta: $PORT"
echo ""

java -jar target/reservations.jar
