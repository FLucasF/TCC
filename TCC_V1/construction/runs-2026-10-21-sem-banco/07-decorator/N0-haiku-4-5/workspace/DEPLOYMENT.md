# Guia de Deployment - Pousada Maré Alta

## Pré-requisitos

- Java 21 (JDK ou JRE)
- Maven 3.6+ (apenas para compilação)
- 512MB RAM mínimo
- Conexão com internet (para logs e monitoramento, opcional)

## Compilação

### Compila local em desenvolvimento:

```bash
cd /workspace
mvn clean verify
```

Resultado: `target/reservas-1.0.0.jar` (~12MB)

### Compilação sem testes (mais rápida):

```bash
mvn clean package -DskipTests
```

## Execução

### Modo Padrão (Produção)

```bash
java -jar target/reservas-1.0.0.jar
```

- Servidor inicia em `http://localhost:7070`
- Emails redirecionados para `arquivo@pousadamarealta.com.br`
- Clientes reais recebem confirmações

### Modo Teste

```bash
HOTEL_TEST_MODE=true java -jar target/reservas-1.0.0.jar
```

- Servidor inicia em `http://localhost:7070`
- Todos os emails redirecionados para `testes@pousadamarealta.com.br`
- Clientes reais **não recebem** emails

### Porta Customizada

```bash
# Porta 8080
PORT=8080 java -jar target/reservas-1.0.0.jar

# Com modo teste
HOTEL_TEST_MODE=true PORT=3000 java -jar target/reservas-1.0.0.jar
```

### Executar em Background

```bash
# Unix/Linux/Mac
nohup java -jar target/reservas-1.0.0.jar > app.log 2>&1 &

# Com modo teste
nohup env HOTEL_TEST_MODE=true java -jar target/reservas-1.0.0.jar > app.log 2>&1 &
```

### Usando systemd (Linux)

Criar `/etc/systemd/system/pousada-reservas.service`:

```ini
[Unit]
Description=Pousada Maré Alta - Sistema de Reservas
After=network.target

[Service]
Type=simple
User=pousada
WorkingDirectory=/opt/pousada-reservas
ExecStart=/usr/bin/java -jar target/reservas-1.0.0.jar
Environment=PORT=7070
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Iniciar:
```bash
sudo systemctl daemon-reload
sudo systemctl start pousada-reservas
sudo systemctl enable pousada-reservas
sudo systemctl status pousada-reservas
```

Ver logs:
```bash
sudo journalctl -u pousada-reservas -f
```

## Verificação de Saúde

### Endpoint simples (sem autenticação)

```bash
curl http://localhost:7070/rooms
```

Se retornar JSON com 5 quartos → sistema está funcionando ✓

## Monitoramento

### Ver logs em tempo real

```bash
tail -f app.log
```

### Verificar se está rodando

```bash
# Unix/Linux/Mac
ps aux | grep java | grep reservas

# Ver conexões ativas
netstat -tlnp | grep 7070
```

## Backup e Recuperação

### IMPORTANTE: Dados em Memória

O sistema atual **não persiste dados em disco**. Ao reiniciar:
- Todas as reservas são perdidas
- Todos os emails são perdidos
- Relatórios ficam vazios

### Recomendações

1. **Curto prazo**: Integrar com banco de dados
2. **Médio prazo**: Backup diário de dados em JSON
3. **Longo prazo**: Usar banco de dados externo (PostgreSQL/MySQL)

### Script de Backup (futuro)

```bash
#!/bin/bash
# Quando integrado com BD

BACKUP_DIR="/backups/pousada-reservas"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Exportar dados via API e salvar em JSON
curl -s http://localhost:7070/reservations > $BACKUP_DIR/reservations_$DATE.json
curl -s http://localhost:7070/messages > $BACKUP_DIR/messages_$DATE.json

# Comprimir
tar -czf $BACKUP_DIR/backup_$DATE.tar.gz $BACKUP_DIR/reservations_$DATE.json $BACKUP_DIR/messages_$DATE.json

# Manter apenas últimos 7 dias
find $BACKUP_DIR -name "backup_*.tar.gz" -mtime +7 -delete
```

## Docker (Futuro)

Exemplo de Dockerfile para containerizar:

```dockerfile
FROM openjdk:21-slim

WORKDIR /app

COPY target/reservas-1.0.0.jar app.jar

EXPOSE 7070

ENV HOTEL_TEST_MODE=false
ENV PORT=7070

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build:
```bash
docker build -t pousada-reservas:1.0.0 .
```

Run:
```bash
docker run -d -p 7070:7070 -e HOTEL_TEST_MODE=false pousada-reservas:1.0.0
```

## Troubleshooting

### Erro: Port already in use

```bash
# Encontrar processo usando a porta
lsof -i :7070

# Matar processo
kill -9 <PID>

# Usar porta diferente
PORT=8080 java -jar target/reservas-1.0.0.jar
```

### Erro: Not enough memory

```bash
# Aumentar heap size
java -Xmx2g -jar target/reservas-1.0.0.jar
```

### Erro: Class not found

```bash
# Garantir que o JAR foi compilado corretamente
mvn clean package

# Verificar se é o JAR correto (com dependências)
jar tf target/reservas-1.0.0.jar | grep "com/pousadamarealta"
```

### Erro: Cannot bind to port

```bash
# Verificar privilégios (porta < 1024 requer root em Unix)
sudo java -jar target/reservas-1.0.0.jar
```

## Performance Tuning

### JVM Flags

```bash
# Para melhor performance
java -server \
  -Xms512m \
  -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar target/reservas-1.0.0.jar
```

### Configuração Recomendada

- **Servidor baixo**: -Xmx512m
- **Servidor médio**: -Xmx1024m
- **Servidor alto**: -Xmx2048m+

## Atualização

### Procedimento

1. **Backup** (importante com BD futuro)
   ```bash
   # Dados de reservas/mensagens se implementado com BD
   ```

2. **Compilar nova versão**
   ```bash
   git pull origin main
   mvn clean verify
   ```

3. **Parar servidor**
   ```bash
   # Se usando systemd
   sudo systemctl stop pousada-reservas
   
   # Ou kill process
   kill <PID>
   ```

4. **Backup do JAR atual**
   ```bash
   cp target/reservas-1.0.0.jar target/reservas-1.0.0.jar.backup
   ```

5. **Iniciar nova versão**
   ```bash
   java -jar target/reservas-1.0.0.jar
   ```

## Checklist de Deploy

### Pré-deployment
- [ ] Código compilado sem erros
- [ ] Todos os testes passam (`mvn verify`)
- [ ] JAR criado (~12MB)
- [ ] Documentação atualizada

### Deploy
- [ ] Java 21 instalado no servidor
- [ ] PORT disponível
- [ ] Espaço em disco suficiente (logs)
- [ ] Permissões de arquivo corretas

### Pós-deployment
- [ ] GET /rooms retorna 5 quartos
- [ ] Nenhum erro nos logs
- [ ] API respondendo (< 200ms)
- [ ] Teste de criação de reserva funciona

## Rollback

Se algo der errado após deploy:

```bash
# 1. Parar aplicação
kill <PID>

# 2. Restaurar JAR anterior
cp target/reservas-1.0.0.jar.backup target/reservas-1.0.0.jar

# 3. Reiniciar
java -jar target/reservas-1.0.0.jar
```

## Licença e Suporte

- Desenvolvido para Pousada Maré Alta
- Código disponível em Java 21 + Javalin
- Suporte: Contatar desenvolvedor
