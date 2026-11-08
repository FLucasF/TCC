# Sistema de Reservas - Pousada Maré Alta

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta.

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven

## Configuração

O sistema utiliza variáveis de ambiente para configuração:

- `DB_URL`: URL JDBC do PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco (padrão: `postgres`)
- `PORT`: Porta do servidor (padrão: `7070`)

## Compilação e Execução

```bash
# Compilar
mvn clean compile

# Rodar testes
mvn test

# Gerar JAR executável
mvn package

# Executar
java -jar target/reservas-1.0.0.jar
```

## Funcionalidades

### Quartos

5 quartos cadastrados automaticamente:
- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Planos de Tarifa

1. **STANDARD**: Sem desconto, cancelamento com devolução total
2. **NON_REFUNDABLE**: 15% de desconto, cancelamento sem devolução
3. **CORPORATE**: Desconto por empresa (Tecnosul 12%, Agrovale 18%, Farmavida 25%)
4. **WEEKEND_PACKAGE**: Pacote sexta a domingo com preço fixo (R$ 520/820/1200)
5. **LONG_STAY**: 7+ noites com 10% de desconto

### Alta Temporada

Acréscimo de 20% nas diárias:
- 15 de dezembro a fim de fevereiro
- Todo o mês de julho

### Fluxo de Reserva

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
           ↓
        CANCELLED
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out
- `GET /reservations/{id}/document` - Emite segunda via do comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Exemplo de Criação de Reserva

```json
POST /reservations
{
  "roomNumber": "201",
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

Para plano CORPORATE, adicionar:
```json
{
  "companyCode": "TECNOSUL"
}
```

## Códigos de Erro

- `400 INVALID_REQUEST`: Dados inválidos
- `404 ROOM_NOT_FOUND`: Quarto não encontrado
- `404 RESERVATION_NOT_FOUND`: Reserva não encontrada
- `409 ROOM_UNAVAILABLE`: Quarto indisponível nas datas
- `409 OPERATION_NOT_ALLOWED`: Operação não permitida no estado atual
- `422 CAPACITY_EXCEEDED`: Hóspedes excedem capacidade do quarto

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── Main.java                    # Inicialização da aplicação
├── controller/                  # Controllers REST
├── dto/                        # DTOs de request/response
├── model/                      # Entidades de domínio
├── repository/                 # Acesso ao banco de dados
└── service/                    # Lógica de negócio
```
