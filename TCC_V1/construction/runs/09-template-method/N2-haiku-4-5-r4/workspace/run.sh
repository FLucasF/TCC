#!/bin/bash

# Script para executar a Pousada Maré Alta

set -e

# Verificar se o Maven está instalado
if ! command -v mvn &> /dev/null; then
    echo "Maven não encontrado. Por favor, instale Maven."
    exit 1
fi

# Verificar se Docker Compose está instalado
if ! command -v docker-compose &> /dev/null; then
    echo "Docker Compose não encontrado. Por favor, instale Docker e Docker Compose."
    exit 1
fi

echo "=== Iniciando Pousada Maré Alta ==="
echo ""

# Iniciar PostgreSQL
echo "1. Iniciando PostgreSQL..."
docker-compose up -d postgres

# Aguardar PostgreSQL estar pronto
echo "2. Aguardando PostgreSQL estar pronto..."
max_attempts=30
attempt=0
while ! docker-compose exec -T postgres pg_isready -U pousada_user > /dev/null 2>&1; do
    if [ $attempt -eq $max_attempts ]; then
        echo "Timeout aguardando PostgreSQL. Verifique os logs com: docker-compose logs postgres"
        exit 1
    fi
    attempt=$((attempt+1))
    sleep 1
done
echo "PostgreSQL está pronto!"
echo ""

# Compilar o projeto
echo "3. Compilando projeto..."
mvn clean package -DskipTests

echo ""
echo "4. Iniciando aplicação..."
echo "   - URL: http://localhost:7070"
echo "   - Banco de dados: pousada"
echo ""

# Executar aplicação
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=pousada_user
export DB_PASSWORD=pousada_pass
export PORT=7070

java -jar target/mare-alta.jar
