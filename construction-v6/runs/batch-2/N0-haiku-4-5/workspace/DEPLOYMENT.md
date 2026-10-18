# Guia de Deployment

## Pré-requisitos

- Java 21+
- PostgreSQL 18+
- 2+ GB de RAM por instância
- Acesso à rede da TransLog (para consulta de frete)

## Preparação do Banco de Dados

### 1. Criar banco de dados

```bash
sudo -u postgres createdb orderservice
```

### 2. Usuário específico (opcional, mais seguro)

```bash
sudo -u postgres createuser orderuser
sudo -u postgres psql -c "ALTER ROLE orderuser WITH PASSWORD 'senha-segura';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE orderservice TO orderuser;"
```

### 3. Conectividade

Certificar-se de que o PostgreSQL está configurado para aceitar conexões TCP/IP:

```bash
# Em /etc/postgresql/18/main/postgresql.conf
listen_addresses = 'localhost'  # ou '*' para aceitar de qualquer IP

# Em /etc/postgresql/18/main/pg_hba.conf
host    orderservice    orderuser    127.0.0.1/32    md5
host    orderservice    orderuser    ::1/128         md5
```

## Build e Pacote

### 1. Compilar

```bash
mvn clean package -DskipTests
```

Resultado: `target/order-service-1.0.0.jar`

### 2. Copiar para destino

```bash
cp target/order-service-1.0.0.jar /opt/services/order-service/
```

## Execução em Desenvolvimento

### Variáveis de ambiente

```bash
export DB_URL="jdbc:postgresql://localhost:5432/orderservice"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
export PORT="8080"
export TRANSLOG_URL="https://api.translog.com.br"
```

### Executar

```bash
java -jar order-service-1.0.0.jar
```

Logs aparecerão no stdout. Serviço disponível em `http://localhost:8080`

## Execução em Produção

### 1. Systemd Service

Criar `/etc/systemd/system/order-service.service`:

```ini
[Unit]
Description=Order Service
After=network.target postgresql.service

[Service]
Type=simple
User=orderservice
WorkingDirectory=/opt/services/order-service
Environment="DB_URL=jdbc:postgresql://db-server:5432/orderservice"
Environment="DB_USER=orderuser"
Environment="DB_PASSWORD=senha-segura"
Environment="PORT=8080"
Environment="TRANSLOG_URL=https://api.translog.com.br"
ExecStart=/usr/bin/java -jar order-service-1.0.0.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

### 2. Habilitar e iniciar

```bash
sudo systemctl daemon-reload
sudo systemctl enable order-service
sudo systemctl start order-service
```

### 3. Logs

```bash
sudo journalctl -u order-service -f
```

## Deployment com Load Balancer

### Múltiplas Instâncias

O serviço é stateless e pode ser escalado horizontalmente:

```
┌─────────────┐
│   Nginx     │ (Port 80/443)
│  Load       │
│ Balancer    │
└──────┬──────┘
       │
   ┌───┴───┬───────┬────────┐
   │       │       │        │
┌──▼──┐ ┌──▼──┐ ┌──▼──┐ ┌──▼──┐
│ JS1 │ │ JS2 │ │ JS3 │ │ JS4 │  (Port 8080)
└──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘
   │       │       │       │
   └───────┴───┬───┴───────┘
           ┌───▼────┐
           │PostgreSQL
           │ (Shared)
           └────────┘
```

### Configuração Nginx

```nginx
upstream order_service {
    server localhost:8080;
    server localhost:8081;
    server localhost:8082;
    server localhost:8083;
}

server {
    listen 80;
    server_name api.loja.com.br;
    
    location /orders {
        proxy_pass http://order_service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    location /products {
        proxy_pass http://order_service;
        # ...
    }
    
    location /customers {
        proxy_pass http://order_service;
        # ...
    }
}
```

### Executar 4 instâncias

```bash
PORT=8080 java -jar order-service-1.0.0.jar &
PORT=8081 java -jar order-service-1.0.0.jar &
PORT=8082 java -jar order-service-1.0.0.jar &
PORT=8083 java -jar order-service-1.0.0.jar &
```

## Health Check

Adicionar endpoint de health (recomendado para produção):

```bash
curl http://localhost:8080/health
```

(Não implementado atualmente - adicionar se necessário)

## Backup e Restore

### Backup

```bash
pg_dump -U orderuser -h localhost orderservice > orderservice-$(date +%Y%m%d-%H%M%S).sql
```

### Restore

```bash
psql -U orderuser -h localhost orderservice < orderservice-20261018.sql
```

## Monitoramento

### Connections ao PostgreSQL

```bash
psql -U postgres -c "SELECT * FROM pg_stat_activity WHERE datname='orderservice';"
```

### Tamanho do Banco

```bash
psql -U postgres -c "SELECT pg_size_pretty(pg_database_size('orderservice'));"
```

### Tabelas

```bash
psql -U orderuser -d orderservice -c "\dt"
```

### Queries Lentas

Ativar log de queries lentas em `postgresql.conf`:

```
log_statement = 'all'
log_min_duration_statement = 100  # milliseconds
```

## Troubleshooting

### Erro: Connection refused

```
org.postgresql.util.PSQLException: Connection to localhost:5432 refused
```

**Solução:**
- Verificar se PostgreSQL está rodando: `systemctl status postgresql`
- Verificar variáveis de ambiente: DB_URL, DB_USER, DB_PASSWORD
- Verificar firewall: `sudo ufw allow 5432`

### Erro: Role orderuser does not exist

```
ERROR: role "orderuser" does not exist
```

**Solução:**
- Criar usuário: `sudo -u postgres createuser orderuser`
- Ou usar postgres: `DB_USER=postgres DB_PASSWORD=...`

### Erro: FATAL: Ident authentication failed

**Solução:**
- Mudar método de autenticação em `pg_hba.conf` de ident para md5
- Recarregar PostgreSQL: `sudo systemctl reload postgresql`

### Erro: TransLog timeout

```
CARRIER_UNAVAILABLE
```

**Solução:**
- Verificar URL da TransLog: TRANSLOG_URL
- Verificar conectividade: `curl {TRANSLOG_URL}/health`
- Aumentar timeout (em CheckoutService se necessário)

## Performance Tuning

### Pool de conexões

Aumentar `maximumPoolSize` se observar muitas conexões esperando:

No `Database.java`:
```java
config.setMaximumPoolSize(20);  // aumentar de 10
```

### Índices no PostgreSQL

```sql
CREATE INDEX idx_orders_cpf ON orders(cpf);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_notifications_cpf ON notifications(cpf);
```

### Caching de Produtos

Adicionar cache em memória (Redis ou local):

```java
// Pseudocódigo
private static Map<String, Product> productCache = new ConcurrentHashMap<>();

public static Product getProduct(String sku) {
    return productCache.computeIfAbsent(sku, k -> {
        // query database
    });
}
```

## Segurança em Produção

### 1. HTTPS/TLS

- Usar certificado SSL
- Redirecionar HTTP → HTTPS
- Configurar no load balancer (Nginx/HAProxy)

### 2. Senhas

- Não usar valores padrão
- Usar environment variables (não hard-coded)
- Rotacionar periodicamente

### 3. Firewall

```bash
# Apenas aplicação
sudo ufw allow 8080/tcp
sudo ufw allow 5432/tcp from 127.0.0.1

# Via load balancer
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
```

### 4. Logs

- Armazenar logs de auditoria
- Monitorar tentativas de acesso
- Exemplo: ELK Stack, Splunk

### 5. Backups

- Backup diário do banco de dados
- Armazenar em local seguro (S3, NAS)
- Testar restore regularmente

## Escalabilidade Futura

Se a carga aumentar:

1. **Mais instâncias** (horizontal scaling) ✓ (já suportado)
2. **Read replicas** do PostgreSQL
3. **Cache distribuído** (Redis)
4. **Message queue** (RabbitMQ) para operações assíncronas
5. **Sharding** se dados crescerem muito

## Versionamento

Manter registro de versões:

```bash
git tag -a v1.0.0 -m "Initial production release"
git push origin v1.0.0
```

## Roadmap de Melhorias

- [ ] Health check endpoint
- [ ] Graceful shutdown
- [ ] Metrics (Prometheus)
- [ ] Distributed tracing (Jaeger)
- [ ] Circuit breaker para TransLog
- [ ] Async notifications com message queue
- [ ] API rate limiting
- [ ] CORS configuration
- [ ] Audit logging
- [ ] API versioning (v1, v2, etc)
