#!/bin/bash

# Script de exemplo para executar o sistema
# Configure as variáveis de ambiente conforme sua instalação

export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
export PORT=7070
# export HOTEL_TEST_MODE=true  # Descomente para modo de teste

java -jar target/marealta-1.0.0.jar
