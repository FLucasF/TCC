#!/bin/bash

set -e

echo "🏨 Pousada Maré Alta - Setup"
echo "================================"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java not found. Please install Java 21+${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K[0-9.]+' | head -1)
echo -e "${GREEN}✓ Java $JAVA_VERSION found${NC}"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}✗ Maven not found. Please install Maven${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Maven found${NC}"

# Build
echo ""
echo "Building project..."
mvn clean package -q

if [ -f "target/reserva-system.jar" ]; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

# Check PostgreSQL Docker
echo ""
read -p "Setup PostgreSQL in Docker? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}✗ Docker not found${NC}"
        exit 1
    fi

    echo "Starting PostgreSQL container..."
    docker rm pousada-db 2>/dev/null || true

    docker run -d \
      --name pousada-db \
      -e POSTGRES_USER=pousada \
      -e POSTGRES_PASSWORD=senha \
      -e POSTGRES_DB=pousada \
      -p 5432:5432 \
      postgres:18-alpine

    echo -e "${GREEN}✓ PostgreSQL container started${NC}"
    echo "  Host: localhost:5432"
    echo "  User: pousada"
    echo "  Password: senha"
    echo "  Database: pousada"
fi

# Create .env file
echo ""
echo "Creating .env file..."
cat > .env.local << 'EOF'
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="senha"
export PORT=7070
EOF

echo -e "${GREEN}✓ .env.local created${NC}"

echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Load environment variables:"
echo "   source .env.local"
echo ""
echo "2. Run the application:"
echo "   java -jar target/reserva-system.jar"
echo ""
echo "3. Test the API:"
echo "   curl http://localhost:7070/rooms"
echo ""
echo "Documentation:"
echo "  - README.md     - Main documentation"
echo "  - DEVELOPMENT.md - Development guide"
echo "  - EXAMPLES.md   - Usage examples"
