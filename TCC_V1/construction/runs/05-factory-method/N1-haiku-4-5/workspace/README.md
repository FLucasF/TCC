# Pousada Maré Alta - Sistema de Reservas

Sistema de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Pré-requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18
- Docker (opcional, para usar com docker-compose)

## Compilação

```bash
mvn clean package
```

Isso irá gerar um JAR executável em `target/pousada-mare-alta-1.0.0.jar`.

## Execução

### Variáveis de Ambiente

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta HTTP (opcional, padrão: 7070)

### Exemplo com Docker Compose

```bash
docker-compose up
```

### Exemplo Manual

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="senha123"
export PORT=7070

java -jar target/pousada-mare-alta-1.0.0.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Listar todos os quartos

### Reservas
- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out (retorna comprovante)
- `GET /reservations/{id}/document` - Obter segunda via do comprovante

### Mensagens
- `GET /messages` - Listar mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação em CSV
- `GET /reports/revenue?month=2027-07` - Relatório financeiro em CSV

## Testes

```bash
mvn verify
```

## Estrutura de Dados

### Quartos

- **101**: STANDARD, até 2 pessoas, R$ 249,90
- **102**: STANDARD, até 2 pessoas, R$ 249,90
- **201**: DELUXE, até 3 pessoas, R$ 389,90
- **202**: DELUXE, até 3 pessoas, R$ 389,90
- **301**: SUITE, até 4 pessoas, R$ 579,90

### Alta Temporada

- 15 de dezembro a 28 de fevereiro
- Mês de julho
- Acréscimo de 20% na diária

### Planos de Tarifa

- **STANDARD**: Sem desconto, reembolso total em cancelamento
- **NON_REFUNDABLE**: 15% desconto, sem reembolso em cancelamento

## Tipos de Documento

A reserva pode ser feita de três formas, cada uma gerando um documento específico:

1. **CPF**: Comprovante de reserva em português
2. **CNPJ**: Fatura de hospedagem em português (com ISS retido de 5%)
3. **Passaporte**: Booking receipt em inglês

## Observações

- Arredondamento de valores usa o método "meio para o par" (banker's rounding)
- Mensagens de confirmação são armazenadas em lista (não enviadas por e-mail real)
- O sistema cria automaticamente as tabelas no banco de dados na inicialização
