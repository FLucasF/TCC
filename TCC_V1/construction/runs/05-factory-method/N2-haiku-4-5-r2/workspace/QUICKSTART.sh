#!/bin/bash

# QUICKSTART - Pousada Maré Alta
# Script de inicialização rápida do sistema

set -e

echo "=== Pousada Maré Alta - Quick Start ==="
echo ""

# 1. Verificar Java
echo "1. Verificando Java 21..."
java_version=$(java -version 2>&1 | grep -oP 'version "\K[^"]+')
echo "   ✓ Java $java_version"

# 2. Build
echo ""
echo "2. Compilando projeto..."
mvn clean package -q
echo "   ✓ JAR gerado em target/mare-alta-1.0.jar"

# 3. Verificar PostgreSQL
echo ""
echo "3. Configurando banco de dados..."
echo ""
echo "   IMPORTANTE: Configure as variáveis de ambiente antes de iniciar o servidor:"
echo ""
echo "   export DB_URL='jdbc:postgresql://localhost:5432/mare_alta'"
echo "   export DB_USER='postgres'"
echo "   export DB_PASSWORD='sua_senha'"
echo "   export PORT='7070'  (opcional, padrão: 7070)"
echo ""

# 4. Instruções de execução
echo "4. Para iniciar o servidor:"
echo ""
echo "   java -jar target/mare-alta-1.0.jar"
echo ""

# 5. Testes de API
echo "5. Testar API:"
echo ""
echo "   # Listar quartos"
echo "   curl http://localhost:7070/rooms"
echo ""
echo "   # Criar reserva"
echo "   curl -X POST http://localhost:7070/reservations \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{\"roomNumber\":201,\"guestName\":\"Carla Dias\",\"guestEmail\":\"carla@email.com\",\"guestCpf\":\"11122233344\",\"checkIn\":\"2027-03-10\",\"checkOut\":\"2027-03-12\",\"guests\":2,\"ratePlan\":\"STANDARD\"}'"
echo ""

echo ""
echo "=== Setup Completo ==="
echo "Veja EXEMPLO_USO.md para exemplos detalhados"
echo "Veja ARQUITETURA.md para documentação técnica"
