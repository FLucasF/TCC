#!/bin/bash

echo "=========================================="
echo "  Pousada Maré Alta - Sistema de Reservas"
echo "=========================================="
echo ""

# Verificar Java
if ! command -v java &> /dev/null; then
    echo "❌ Java não encontrado. Instale Java 21."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21 ou superior necessário. Versão atual: $JAVA_VERSION"
    exit 1
fi
echo "✅ Java $JAVA_VERSION encontrado"

# Verificar PostgreSQL
if [ -z "$DB_URL" ]; then
    echo "⚠️  DB_URL não configurado. Usando padrão: jdbc:postgresql://localhost:5432/pousada"
    export DB_URL="jdbc:postgresql://localhost:5432/pousada"
fi

if [ -z "$DB_USER" ]; then
    echo "⚠️  DB_USER não configurado. Usando padrão: postgres"
    export DB_USER="postgres"
fi

if [ -z "$DB_PASSWORD" ]; then
    echo "⚠️  DB_PASSWORD não configurado. Usando padrão: postgres"
    export DB_PASSWORD="postgres"
fi

# Porta
if [ -z "$PORT" ]; then
    export PORT=7070
fi

# Modo de teste
if [ -z "$HOTEL_TEST_MODE" ]; then
    export HOTEL_TEST_MODE=false
fi

echo ""
echo "Configuração:"
echo "  DB_URL: $DB_URL"
echo "  DB_USER: $DB_USER"
echo "  PORT: $PORT"
echo "  HOTEL_TEST_MODE: $HOTEL_TEST_MODE"
echo ""

# Verificar se JAR existe
if [ ! -f "target/reservas-1.0.0.jar" ]; then
    echo "⚠️  JAR não encontrado. Compilando..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "❌ Erro ao compilar"
        exit 1
    fi
    echo "✅ Compilação concluída"
fi

echo ""
echo "Iniciando servidor..."
echo "=========================================="
echo ""

java -jar target/reservas-1.0.0.jar
