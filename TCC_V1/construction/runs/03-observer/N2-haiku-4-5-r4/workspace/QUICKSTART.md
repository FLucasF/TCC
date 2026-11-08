# Quick Start - Como Iniciar o Sistema Localmente

## Pré-requisitos

- Docker e Docker Compose
- Java 21+
- Maven 3.6+

## Passo 1: Compilar o projeto

```bash
mvn clean package
```

## Passo 2: Iniciar o PostgreSQL

```bash
docker-compose up -d
```

Aguarde o banco ficar pronto (~10 segundos). Você pode verificar com:

```bash
docker-compose logs -f postgres
```

Procure por `database system is ready to accept connections`.

## Passo 3: Executar a aplicação

```bash
DB_URL="jdbc:postgresql://localhost:5432/pousada" \
DB_USER="pousada" \
DB_PASSWORD="pousada123" \
PORT=7070 \
java -jar target/reserva-system.jar
```

A API estará disponível em `http://localhost:7070`.

## Passo 4: Testar a API

Em outro terminal:

```bash
# Listar quartos
curl http://localhost:7070/rooms

# Criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Silva",
    "guestEmail": "maria@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

Veja `API_EXAMPLES.md` para mais exemplos.

## Parar a aplicação

Pressione `Ctrl+C` na janela onde a aplicação está rodando.

## Parar o PostgreSQL

```bash
docker-compose down
```

Para deletar os dados também:

```bash
docker-compose down -v
```

## Acessar o banco de dados diretamente

```bash
docker-compose exec -it postgres psql -U pousada -d pousada
```

Dentro do PostgreSQL:

```sql
-- Ver tabelas
\dt

-- Ver quartos
SELECT * FROM rooms;

-- Ver reservas
SELECT id, room_number, guest_name, status, total FROM reservations;

-- Ver mensagens
SELECT id, "to", subject FROM messages;
```

## Troubleshooting

### Porta 5432 já está em uso
Se a porta 5432 já está em uso, você pode mudar na linha `ports` do `docker-compose.yml`:

```yaml
ports:
  - "5433:5432"  # Usar porta 5433 no host
```

E então na variável de ambiente:
```bash
DB_URL="jdbc:postgresql://localhost:5433/pousada"
```

### Erro de conexão ao banco
Certifique-se de que o PostgreSQL está pronto:

```bash
docker-compose logs postgres
```

Procure pela mensagem `ready to accept connections`.

### Erro "address already in use" na porta 7070
A API está tentando usar uma porta que já está em uso. Use outra:

```bash
PORT=8080 java -jar target/reserva-system.jar
```

## Setup de Produção

Para um ambiente de produção:

1. Use variáveis de ambiente seguras
2. Configure o PostgreSQL com SSL
3. Use um proxy reverso (nginx, HAProxy)
4. Implemente backup automático do banco
5. Configure logs em um arquivo ou sistema de logging centralizado
6. Use um container orchestration (Kubernetes, Docker Swarm)

Exemplo para Kubernetes:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pousada-reserva
spec:
  containers:
  - name: reserva-system
    image: pousada-reserva:1.0.0
    ports:
    - containerPort: 7070
    env:
    - name: DB_URL
      valueFrom:
        secretKeyRef:
          name: db-credentials
          key: url
    - name: DB_USER
      valueFrom:
        secretKeyRef:
          name: db-credentials
          key: user
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: db-credentials
          key: password
    - name: PORT
      value: "7070"
```

## Monitores Recomendados

Para monitorar a aplicação em produção:

- **Logs**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Métricas**: Prometheus + Grafana
- **APM**: Jaeger (tracing distribuído)
- **Alertas**: AlertManager

## Próximos Passos

Veja a documentação completa em:
- `README.md` - Overview do projeto
- `API_EXAMPLES.md` - Exemplos de requisições
- `DATABASE_SCHEMA.md` - Schema do banco de dados
