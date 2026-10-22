#!/bin/bash

# Script para iniciar a Pousada Maré Alta

set -e

# Cores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Pousada Maré Alta - Sistema de Reservas ===${NC}\n"

# Verificar se o JAR existe
if [ ! -f "target/mare-alta.jar" ]; then
    echo -e "${YELLOW}JAR não encontrado. Compilando...${NC}"
    mvn clean package -DskipTests
fi

# Carregar variáveis de ambiente do .env se existir
if [ -f ".env" ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo -e "${GREEN}Configurações carregadas de .env${NC}"
fi

# Usar valores padrão se não configurados
DB_URL=${DB_URL:-"jdbc:postgresql://localhost:5432/pousada"}
DB_USER=${DB_USER:-"postgres"}
DB_PASSWORD=${DB_PASSWORD:-"password"}
PORT=${PORT:-"7070"}

echo "Configuração:"
echo "  DB_URL: $DB_URL"
echo "  DB_USER: $DB_USER"
echo "  PORT: $PORT"

if [ -n "$HOTEL_TEST_MODE" ]; then
    echo -e "  ${YELLOW}HOTEL_TEST_MODE: $HOTEL_TEST_MODE (modo teste ativo)${NC}"
fi

echo ""
echo -e "${GREEN}Iniciando servidor...${NC}\n"

DB_URL="$DB_URL" \
DB_USER="$DB_USER" \
DB_PASSWORD="$DB_PASSWORD" \
PORT="$PORT" \
HOTEL_TEST_MODE="${HOTEL_TEST_MODE}" \
HOTEL_ARCHIVE_DISABLED="${HOTEL_ARCHIVE_DISABLED}" \
java -jar target/mare-alta.jar
