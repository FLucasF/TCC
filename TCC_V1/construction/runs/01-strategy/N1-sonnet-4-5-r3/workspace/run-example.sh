#!/bin/bash

# Exemplo de como executar o sistema
# Configure as variáveis de ambiente antes de executar

export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT=7070

java -jar target/pousada-mare-alta-1.0.0.jar
