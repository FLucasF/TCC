#!/bin/bash

# Start PostgreSQL
docker-compose up -d

# Wait for PostgreSQL to be ready
echo "Esperando PostgreSQL iniciar..."
sleep 5

# Run the application
DB_URL="jdbc:postgresql://localhost:5432/pousada" \
DB_USER="pousada" \
DB_PASSWORD="pousada123" \
PORT=7070 \
java -jar target/mare-alta-reservas-1.0.0.jar
