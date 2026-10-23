# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- Maven 3.8+
- PostgreSQL 18 (ou use o Docker Compose incluído)

## Configuração

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT` (opcional): Porta do servidor (padrão: 7070)

## Executando com Docker Compose

Para facilitar o desenvolvimento, inclui-se um arquivo `docker-compose.yml` que sobe um PostgreSQL:

```bash
docker-compose up -d
```

Isso criará um banco PostgreSQL na porta 5432 com:
- Banco: `pousada`
- Usuário: `pousada`
- Senha: `pousada123`

## Compilação

```bash
mvn clean package
```

Isso gerará um JAR executável em `target/pousada-marealta-1.0.0.jar`.

## Execução

Com as variáveis de ambiente configuradas:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=pousada
export DB_PASSWORD=pousada123
export PORT=7070

java -jar target/pousada-marealta-1.0.0.jar
```

Ou diretamente:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=pousada \
DB_PASSWORD=pousada123 \
java -jar target/pousada-marealta-1.0.0.jar
```

## Testes

Para executar os testes de integração:

```bash
mvn verify
```

Os testes requerem um banco PostgreSQL acessível. Configure as variáveis `DB_URL`, `DB_USER` e `DB_PASSWORD` se necessário.

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - Obtém segunda via do documento

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Tipos de Hóspede

O sistema suporta três tipos de identificação:

1. **Pessoa Física** - fornece `guestCpf`
2. **Empresa** - fornece `companyCnpj` e `companyName`
3. **Estrangeiro** - fornece `guestPassport` e `guestCountry`

Cada tipo recebe um documento diferente (comprovante, fatura ou booking receipt).

## Estrutura do Banco de Dados

O sistema cria automaticamente as seguintes tabelas:

- `rooms` - Quartos da pousada
- `reservations` - Reservas
- `messages` - Mensagens enviadas (e-mails)

## Quartos Disponíveis

- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

## Alta Temporada

Acréscimo de 20% nas diárias:
- 15 de dezembro a 28 de fevereiro
- Julho inteiro
