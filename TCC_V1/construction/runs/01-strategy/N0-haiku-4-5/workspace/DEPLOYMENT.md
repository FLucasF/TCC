# Guia de Implantação - Pousada Maré Alta

## Pré-requisitos

- Java 21 ou superior
- PostgreSQL 18 ou superior
- Linux/macOS ou Windows com WSL2

## Passo 1: Preparar o Banco de Dados

```bash
# Criar banco de dados PostgreSQL
createdb pousada

# Verificar conexão
psql -U postgres -d pousada -c "SELECT version();"
```

## Passo 2: Configurar Variáveis de Ambiente

```bash
# Linux/macOS
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha_aqui"
export PORT=7070

# Windows (PowerShell)
$env:DB_URL = "jdbc:postgresql://localhost:5432/pousada"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "sua_senha_aqui"
$env:PORT = "7070"

# Windows (CMD)
set DB_URL=jdbc:postgresql://localhost:5432/pousada
set DB_USER=postgres
set DB_PASSWORD=sua_senha_aqui
set PORT=7070
```

## Passo 3: Executar a Aplicação

```bash
# Diretamente
java -jar target/mare-alta-1.0.0.jar

# Ou em background (Linux/macOS)
nohup java -jar target/mare-alta-1.0.0.jar > pousada.log 2>&1 &

# Ou em background (Windows PowerShell)
Start-Process java -ArgumentList "-jar target/mare-alta-1.0.0.jar" -WindowStyle Hidden
```

## Passo 4: Verificar Saúde da Aplicação

```bash
# Listar quartos (teste básico)
curl http://localhost:7070/rooms

# Deve retornar um JSON com 5 quartos:
# [
#   {"number": 101, "type": "STANDARD", "capacity": 2, "nightlyRate": 249.90},
#   {"number": 102, "type": "STANDARD", "capacity": 2, "nightlyRate": 249.90},
#   ...
# ]
```

## Passo 5: Configurar Firewall (Produção)

Se a aplicação estiver em um servidor remoto:

```bash
# Linux (iptables)
iptables -A INPUT -p tcp --dport 7070 -j ACCEPT

# Linux (UFW)
ufw allow 7070/tcp

# Windows
netsh advfirewall firewall add rule name="Pousada Maré Alta" dir=in action=allow protocol=tcp localport=7070
```

## Passo 6: Configurar Systemd (Linux - Produção)

Criar arquivo `/etc/systemd/system/pousada.service`:

```ini
[Unit]
Description=Pousada Maré Alta - Sistema de Reservas
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=pousada
WorkingDirectory=/opt/pousada
ExecStart=/usr/bin/java -jar /opt/pousada/mare-alta-1.0.0.jar
Restart=always
RestartSec=10
Environment="DB_URL=jdbc:postgresql://localhost:5432/pousada"
Environment="DB_USER=postgres"
Environment="DB_PASSWORD=senha_segura"
Environment="PORT=7070"

[Install]
WantedBy=multi-user.target
```

Ativar o serviço:

```bash
sudo systemctl daemon-reload
sudo systemctl enable pousada
sudo systemctl start pousada
sudo systemctl status pousada
```

## Passo 7: Configurar Nginx como Proxy Reverso (Produção)

Criar arquivo `/etc/nginx/sites-available/pousada`:

```nginx
upstream pousada {
    server localhost:7070;
}

server {
    listen 80;
    server_name api.pousadamareal.com.br;

    # Redirecionar HTTP para HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.pousadamareal.com.br;

    # Certificados SSL
    ssl_certificate /etc/letsencrypt/live/pousadamareal.com.br/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/pousadamareal.com.br/privkey.pem;

    # Configurações de segurança
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "DENY" always;

    # Proxy para aplicação
    location / {
        proxy_pass http://pousada;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
        proxy_connect_timeout 60s;
    }
}
```

Ativar:

```bash
sudo ln -s /etc/nginx/sites-available/pousada /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

## Passo 8: Backup Regular do Banco de Dados

Script de backup (`/opt/pousada/backup.sh`):

```bash
#!/bin/bash

BACKUP_DIR="/backups/pousada"
DB_NAME="pousada"
DB_USER="postgres"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Backup do banco
pg_dump -U $DB_USER $DB_NAME | gzip > $BACKUP_DIR/pousada_$DATE.sql.gz

# Manter últimos 30 dias
find $BACKUP_DIR -name "pousada_*.sql.gz" -mtime +30 -delete

echo "Backup realizado: $BACKUP_DIR/pousada_$DATE.sql.gz"
```

Executar diariamente via cron:

```bash
# Adicionar ao crontab (crontab -e)
0 2 * * * /opt/pousada/backup.sh
```

## Passo 9: Monitoramento

### Verificar Logs

```bash
# Systemd
sudo journalctl -u pousada -f

# Arquivo de log
tail -f pousada.log
```

### Verificar Saúde

```bash
# Endpoint de teste
curl -s http://localhost:7070/rooms | jq '.length'

# Deve retornar: 5
```

### Monitorar Uso de Recursos

```bash
# CPU e Memória
ps aux | grep java

# Conexões do PostgreSQL
psql -U postgres -d pousada -c "SELECT count(*) FROM pg_stat_activity;"
```

## Troubleshooting

### Erro: "Conexão recusada" (PostgreSQL)

```bash
# Verificar se PostgreSQL está rodando
sudo systemctl status postgresql

# Verificar credenciais
psql -U postgres -h localhost -d pousada -c "SELECT 1"
```

### Erro: "Porta já em uso"

```bash
# Encontrar processo na porta 7070
lsof -i :7070

# Matar processo (se necessário)
kill -9 <PID>

# Ou usar porta diferente
export PORT=7071
```

### Erro: "Out of Memory"

```bash
# Aumentar memória heap da JVM
java -Xmx2g -Xms512m -jar target/mare-alta-1.0.0.jar
```

### Erro: "Connection pool exhausted"

```bash
# Aumentar pool de conexões (no código Database.java)
config.setMaximumPoolSize(20);
```

## Performance

### Índices Recomendados

Já incluídos no schema:

```sql
CREATE INDEX idx_reservations_room_dates 
  ON reservations(room_number, check_in, check_out);
  
CREATE INDEX idx_reservations_status 
  ON reservations(status);
  
CREATE INDEX idx_reservations_check_in 
  ON reservations(check_in);
```

### Análise de Performance

```bash
# Conectar ao PostgreSQL
psql -U postgres -d pousada

# Ver plano de execução
EXPLAIN ANALYZE 
SELECT * FROM reservations 
WHERE check_in <= '2027-07-10' AND check_out > '2027-07-10';

# Analisar tabelas
ANALYZE;
```

## Segurança

### Checklist de Produção

- [ ] Usar HTTPS (SSL/TLS)
- [ ] Firewall configurado
- [ ] Senha PostgreSQL forte
- [ ] Backup automático habilitado
- [ ] Monitoramento ativo
- [ ] Logs centralizados
- [ ] Rate limiting configurado
- [ ] Validação de entrada (já implementada)
- [ ] CORS configurado se necessário
- [ ] Autenticação/Autorização implementada (future)

## Suporte

### Informações de Versão

```bash
# Ver versão Java
java -version

# Ver versão PostgreSQL
psql --version

# Ver logs da aplicação
curl http://localhost:7070/rooms -v
```

### Contato

Para problemas técnicos, consultar documentação em:
- README.md - API completa
- VALIDATION.md - Validações e cálculos
- Logs da aplicação

## Rollback

Se necessário reverter para versão anterior:

```bash
# Parar serviço
sudo systemctl stop pousada

# Restaurar arquivo JAR
cp mare-alta-1.0.0-previous.jar target/mare-alta-1.0.0.jar

# Restaurar banco de dados
gunzip -c /backups/pousada/pousada_20271010_120000.sql.gz | psql -U postgres pousada

# Reiniciar
sudo systemctl start pousada
```
