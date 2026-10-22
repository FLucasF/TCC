#!/bin/bash

# Script para executar a Pousada Maré Alta localmente com PostgreSQL em Docker

set -e

echo "🏖️  Pousada Maré Alta - Sistema de Reservas"
echo "=========================================="
echo ""

# Verificar se Docker está instalado
if ! command -v docker &> /dev/null; then
    echo "❌ Docker não encontrado. Instale Docker para continuar."
    exit 1
fi

# Verificar se Maven está instalado
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven não encontrado. Instale Maven para continuar."
    exit 1
fi

# Compilar o projeto
echo "📦 Compilando o projeto..."
mvn clean package -DskipTests -q

echo "✅ Projeto compilado com sucesso!"
echo ""

# Parar containers anteriores
echo "🛑 Parando containers anteriores..."
docker-compose down -v 2>/dev/null || true
echo "✅ Containers parados"
echo ""

# Iniciar PostgreSQL
echo "🐘 Iniciando PostgreSQL..."
docker-compose up -d postgres

# Esperar PostgreSQL ficar pronto
echo "⏳ Aguardando PostgreSQL estar pronto..."
for i in {1..30}; do
    if docker exec marealta-postgres pg_isready -U pousada >/dev/null 2>&1; then
        echo "✅ PostgreSQL pronto!"
        break
    fi
    sleep 1
done

echo ""
echo "🚀 Iniciando API..."
echo ""

# Executar a aplicação
export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="pousada"
export DB_PASSWORD="senha123"
export PORT="7070"

java -jar target/mare-alta-1.0.0.jar
