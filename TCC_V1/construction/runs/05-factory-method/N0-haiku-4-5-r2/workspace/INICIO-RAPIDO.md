# Início Rápido - Pousada Maré Alta

## 1. Pré-requisitos

- Java 21 ou superior
- Maven 3.9 ou superior
- Docker e Docker Compose (recomendado)
- curl (para testar)

## 2. Compilação

```bash
cd /workspace
mvn clean package
```

O JAR será gerado em `target/mare-alta-1.0.0.jar`

## 3. Executar com Docker Compose (Recomendado)

```bash
# Terminal 1: Iniciar PostgreSQL
docker-compose up postgres

# Terminal 2: Iniciar a API
export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="pousada"
export DB_PASSWORD="senha123"
export PORT=7070

java -jar target/mare-alta-1.0.0.jar
```

Ou simplesmente:
```bash
chmod +x run.sh
./run.sh
```

## 4. Executar Localmente com PostgreSQL

```bash
# 1. Iniciar PostgreSQL em Docker
docker run --name marealta-db \
  -e POSTGRES_USER=pousada \
  -e POSTGRES_PASSWORD=senha123 \
  -e POSTGRES_DB=marealta \
  -p 5432:5432 \
  postgres:18

# 2. Em outro terminal, executar a aplicação
DB_URL="jdbc:postgresql://localhost:5432/marealta" \
DB_USER="pousada" \
DB_PASSWORD="senha123" \
PORT=7070 \
java -jar target/mare-alta-1.0.0.jar
```

## 5. Verificar se está funcionando

```bash
# Listar quartos
curl http://localhost:7070/rooms | jq .

# Criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .

# Ver mensagens enviadas
curl http://localhost:7070/messages | jq .
```

## 6. Parar a aplicação

```bash
# Parar o docker-compose
docker-compose down

# Ou apenas Ctrl+C no terminal onde a aplicação está rodando
```

## 7. Documentação Completa

- Veja `EXEMPLOS.md` para exemplos detalhados de requisições/respostas
- Veja `README.md` para documentação completa da API

## Estrutura dos Dados

### Quartos (pré-cadastrados)

| Número | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101, 102 | STANDARD | 2 | R$ 249,90 |
| 201, 202 | DELUXE | 3 | R$ 389,90 |
| 301 | SUITE | 4 | R$ 579,90 |

### Tarifas

- **Normal (STANDARD)**: Preço cheio, reembolso integral em cancelamento
- **Não Reembolsável (NON_REFUNDABLE)**: 15% de desconto, sem reembolso

### Alta Temporada

- Julho inteiro
- 15 de dezembro até 28 de fevereiro
- **+20% na diária**

## Tipos de Identificação

### 1. CPF (Brasileiro)
```json
{
  "guestCpf": "123.456.789-00"
}
```
→ Comprovante de Reserva em português

### 2. CNPJ (Empresa)
```json
{
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Sua Empresa Ltda"
}
```
→ Fatura de Hospedagem com ISS retido (5%)

### 3. Passaporte (Estrangeiro)
```json
{
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom"
}
```
→ Booking Receipt em inglês

## Fluxo de Reserva

1. **Criar** (PENDING): `POST /reservations`
2. **Confirmar** (CONFIRMED): `POST /reservations/{id}/confirmation`
   - Email com comprovante é enviado
3. **Check-in** (CHECKED_IN): `POST /reservations/{id}/check-in`
4. **Check-out** (CHECKED_OUT): `POST /reservations/{id}/check-out`
   - Documento é retornado

**Cancelamento** (antes do check-in): `POST /reservations/{id}/cancellation`
- STANDARD: reembolso integral
- NON_REFUNDABLE: sem reembolso

## Relatórios

```bash
# Ocupação de um dia (CSV)
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"

# Receita de um mês (CSV)
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

## Testes

```bash
mvn verify
```

Executa testes de pricing (arredondamento banker's rounding).

## Variáveis de Ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| DB_URL | - | URL JDBC do PostgreSQL |
| DB_USER | - | Usuário do banco |
| DB_PASSWORD | - | Senha do banco |
| PORT | 7070 | Porta da API |

## Troubleshooting

**Erro: "Cannot connect to PostgreSQL"**
- Verifique se PostgreSQL está rodando
- Verifique as credenciais em DB_USER e DB_PASSWORD

**Erro: "Port 7070 already in use"**
- Use uma porta diferente: `PORT=8080 java -jar ...`

**Erro: "Connection refused"**
- Aguarde alguns segundos para o banco estar pronto
- Verifique logs com `docker logs marealta-postgres`

## Próximos Passos

1. Explore a API com os exemplos em `EXEMPLOS.md`
2. Verifique logs de mensagens: `GET /messages`
3. Gere relatórios: `/reports/occupancy` e `/reports/revenue`
