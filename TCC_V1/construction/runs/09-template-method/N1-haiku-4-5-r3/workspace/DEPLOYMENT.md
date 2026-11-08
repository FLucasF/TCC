# Guia de Deployment - Pousada Maré Alta

## Checklist Pré-Deployment

- [ ] PostgreSQL 18 instalado e rodando
- [ ] Banco de dados criado
- [ ] Java 21 instalado
- [ ] Arquivo JAR gerado com sucesso (`mvn clean verify`)

## Configuração do PostgreSQL

### Criar banco de dados:

```sql
CREATE DATABASE pousada OWNER seu_usuario;
```

### Variáveis de Ambiente

Configure as seguintes variáveis antes de iniciar o sistema:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="seu_usuario"
export DB_PASSWORD="sua_senha"
export PORT=7070
```

Você pode adicionar essas linhas em um arquivo `.env` e carregar com:
```bash
source .env
```

## Build

```bash
# Baixar dependências
mvn dependency:resolve

# Compilar e executar testes
mvn clean verify

# Gerar JAR executável
mvn clean package -DskipTests
```

O arquivo JAR executável será gerado em:
```
target/mare-alta-1.0.0.jar
```

## Executar o Sistema

```bash
java -jar target/mare-alta-1.0.0.jar
```

Você deverá ver:
```
Server started on port 7070
```

## Logs e Troubleshooting

### Erro: "Database environment variables not set"
- Solução: Verificar se DB_URL, DB_USER e DB_PASSWORD estão definidas

### Erro: "Failed to create database schema"
- Solução: Verificar se o PostgreSQL está rodando e a conexão está correta

### Erro: "Connection refused"
- Solução: Verificar se PostgreSQL está rodando na porta correta (padrão 5432)

## Verificação Pós-Deploy

Após iniciar o servidor, teste:

```bash
# 1. Verificar se API está respondendo
curl http://localhost:7070/rooms

# 2. Criar uma reserva de teste
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test User",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'

# 3. Verificar se banco de dados foi criado
# As tabelas devem estar criadas automaticamente:
# - rooms (5 quartos pré-cadastrados)
# - reservations
# - messages
```

## Sistema em Produção

Para rodar o sistema em produção com mais recursos:

```bash
java -Xmx512m -Xms256m -jar target/mare-alta-1.0.0.jar
```

Para rodar em background com logs:

```bash
nohup java -Xmx512m -Xms256m -jar target/mare-alta-1.0.0.jar > logs.txt 2>&1 &
```

## Backup do Banco de Dados

Para fazer backup do PostgreSQL:

```bash
pg_dump -U seu_usuario -h localhost pousada > backup.sql
```

Para restaurar:

```bash
psql -U seu_usuario -h localhost pousada < backup.sql
```

## Monitoramento

### Verificar processo Java:
```bash
ps aux | grep mare-alta
```

### Verificar porta:
```bash
netstat -tuln | grep 7070
```

ou

```bash
lsof -i :7070
```

## Health Check Script

Salve como `health-check.sh`:

```bash
#!/bin/bash

RESPONSE=$(curl -s http://localhost:7070/rooms)

if [ $? -eq 0 ]; then
    echo "✓ Sistema está operacional"
    exit 0
else
    echo "✗ Sistema não está respondendo"
    exit 1
fi
```

Execute periodicamente:
```bash
*/5 * * * * /path/to/health-check.sh >> /var/log/pousada-health.log 2>&1
```

## Estrutura de Diretórios Recomendada

```
/opt/pousada-mare-alta/
├── mare-alta-1.0.0.jar
├── .env (com variáveis de ambiente)
├── logs/
│   ├── application.log
│   └── error.log
├── scripts/
│   ├── start.sh
│   ├── stop.sh
│   └── health-check.sh
└── backups/
    └── daily/
```

## Script de Inicialização (start.sh)

```bash
#!/bin/bash

# Carregar variáveis de ambiente
source /opt/pousada-mare-alta/.env

# Iniciar aplicação
cd /opt/pousada-mare-alta
java -Xmx512m -Xms256m -jar mare-alta-1.0.0.jar >> logs/application.log 2>&1 &

echo $! > /tmp/pousada.pid
echo "Sistema iniciado (PID: $!)"
```

## Script de Parada (stop.sh)

```bash
#!/bin/bash

if [ -f /tmp/pousada.pid ]; then
    kill $(cat /tmp/pousada.pid)
    rm /tmp/pousada.pid
    echo "Sistema parado"
else
    echo "Sistema não está rodando"
fi
```

## Considerar para o Futuro

- [ ] Implementar autenticação/autorização
- [ ] Adicionar rate limiting
- [ ] Implementar logging estruturado
- [ ] Adicionar métricas e monitoramento (Prometheus/Grafana)
- [ ] Containerizar com Docker
- [ ] Setup de CI/CD com GitHub Actions
- [ ] Implementar testes de integração com testcontainers
- [ ] API documentation com Swagger/OpenAPI
- [ ] Caching de relatórios
- [ ] Suporte a múltiplas pousadas (multi-tenant)

## Contato e Suporte

Para dúvidas sobre o sistema, consulte:
- README.md - Documentação geral
- TESTING.md - Guia de testes
- CLAUDE.md - Instruções de desenvolvimento
