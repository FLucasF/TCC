#!/bin/bash

# Pousada Maré Alta - Script de Inicialização

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Pousada Maré Alta - Sistema de Reservas${NC}"
echo "========================================"

# Verificar se Java está instalado
if ! command -v java &> /dev/null; then
    echo -e "${RED}Erro: Java 21 não foi encontrado. Por favor, instale Java 21+${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K[0-9]+' | head -1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${RED}Erro: Java 21+ é requerido. Versão atual: $JAVA_VERSION${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Java $JAVA_VERSION detectado${NC}"

# Verificar se o jar existe
if [ ! -f "target/marealta-reservations-1.0.0.jar" ]; then
    echo -e "${YELLOW}JAR não encontrado. Compilando...${NC}"
    mvn clean package -q
    echo -e "${GREEN}✓ JAR compilado com sucesso${NC}"
fi

# Configurar variáveis de ambiente
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/marealta}"
export DB_USER="${DB_USER:-postgres}"
export DB_PASSWORD="${DB_PASSWORD:-postgres}"
export PORT="${PORT:-7070}"

echo ""
echo "Configuração:"
echo "  - Banco de Dados: $DB_URL"
echo "  - Usuário: $DB_USER"
echo "  - Porta: $PORT"
echo ""

# Iniciar a aplicação
echo -e "${GREEN}Iniciando servidor...${NC}"
echo ""

java -jar target/marealta-reservations-1.0.0.jar
