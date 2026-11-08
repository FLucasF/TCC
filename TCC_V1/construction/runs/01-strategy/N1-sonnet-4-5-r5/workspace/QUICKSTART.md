# Guia Rápido de Execução

## 1. Configurar o Banco de Dados

Inicie um container PostgreSQL:

```bash
docker run --name pousada-db \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=pousada \
  -p 5432:5432 \
  -d postgres:18
```

Ou use um PostgreSQL existente e crie o database:

```sql
CREATE DATABASE pousada;
```

## 2. Compilar o Projeto

```bash
mvn clean package
```

Isso gera o JAR executável em `target/pousada-reservas-1.0.0.jar`

## 3. Executar o Sistema

Com as configurações padrão (PostgreSQL local):

```bash
java -jar target/pousada-reservas-1.0.0.jar
```

Ou com configuração customizada:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070

java -jar target/pousada-reservas-1.0.0.jar
```

O sistema irá:
- Conectar ao banco de dados
- Criar as tabelas necessárias
- Inicializar os 5 quartos da pousada
- Iniciar o servidor na porta configurada

## 4. Testar a API

Listar os quartos:

```bash
curl http://localhost:7070/rooms
```

Criar uma reserva com convênio Tecnosul (exemplo do enunciado):

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

Você deve receber uma reserva com:
- subtotal: 779.80
- discount: 93.58
- total: 686.22

## 5. Executar os Testes

```bash
mvn test
```

Ou para validação completa:

```bash
mvn verify
```

## 6. Parar o Sistema

Pressione `Ctrl+C` no terminal onde o sistema está rodando.

Para parar o banco de dados Docker:

```bash
docker stop pousada-db
docker rm pousada-db
```

## Planos de Tarifa Disponíveis

1. **STANDARD**: Normal, reembolso total
2. **NON_REFUNDABLE**: 15% desconto, sem reembolso
3. **CORPORATE**: Desconto por empresa, reembolso total
   - TECNOSUL: 12%
   - AGROVALE: 18%
   - FARMAVIDA: 25%
4. **WEEKEND_PACKAGE**: Sexta a domingo, preço fixo, reembolso 50%
5. **LONG_STAY**: 7+ noites, 10% desconto, reembolso total

Veja mais exemplos em `API_EXAMPLES.md`
