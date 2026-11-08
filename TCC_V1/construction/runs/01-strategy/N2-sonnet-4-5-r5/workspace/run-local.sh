#!/bin/bash

echo "=== Iniciando Pousada Maré Alta - Ambiente Local ==="
echo

# Verificar se docker-compose está disponível
if ! command -v docker-compose &> /dev/null; then
    echo "docker-compose não encontrado. Instale o Docker Compose para continuar."
    exit 1
fi

# Subir PostgreSQL
echo "1. Subindo PostgreSQL..."
docker-compose up -d postgres

# Aguardar PostgreSQL ficar pronto
echo "2. Aguardando PostgreSQL inicializar..."
sleep 5

# Configurar variáveis de ambiente
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada_user"
export DB_PASSWORD="pousada_pass"
export PORT=7070

echo "3. Compilando aplicação..."
mvn -q clean package

if [ $? -ne 0 ]; then
    echo "Erro na compilação. Abortando."
    exit 1
fi

echo "4. Iniciando aplicação..."
echo
java -jar target/pousada-mare-alta-1.0.0.jar
