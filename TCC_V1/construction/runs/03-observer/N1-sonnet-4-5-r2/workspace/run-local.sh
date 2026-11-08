#!/bin/bash

# Script para rodar o sistema localmente

echo "=== Pousada Maré Alta - Sistema de Reservas ==="
echo

# Verificar se o JAR existe
if [ ! -f "target/pousada-reservas-1.0.0.jar" ]; then
    echo "JAR não encontrado. Compilando..."
    mvn clean package
    if [ $? -ne 0 ]; then
        echo "Erro na compilação!"
        exit 1
    fi
fi

# Configurar variáveis de ambiente
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/pousada}"
export DB_USER="${DB_USER:-postgres}"
export DB_PASSWORD="${DB_PASSWORD:-postgres}"
export PORT="${PORT:-7070}"

echo "Configuração:"
echo "  DB_URL: $DB_URL"
echo "  DB_USER: $DB_USER"
echo "  PORT: $PORT"
echo

echo "Iniciando o sistema..."
java -jar target/pousada-reservas-1.0.0.jar
