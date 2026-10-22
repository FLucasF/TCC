# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java com Javalin e PostgreSQL.

## Compilação e Execução

### Compilar o projeto
```bash
mvn clean package
```

### Rodar os testes
```bash
mvn test
mvn verify
```

### Executar a aplicação
```bash
export DB_URL="jdbc:postgresql://localhost:5432/mare_alta"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT=7070

java -jar target/mare-alta-api-1.0.0.jar
```

## Estrutura do Projeto

### Domínio (`com.pousada.domain`)
- **Room**: Representa um quarto da pousada
- **Reservation**: Representa uma reserva com todos os campos necessários
- **Message**: Representa um e-mail enviado
- **Enums**: RoomType, RatePlan, ReservationStatus

### Documentos (`com.pousada.document`)
Implementa o padrão Strategy para diferentes tipos de documentos:
- **CpfDocumentGenerator**: Gera comprovante em português (pessoa física com CPF)
- **CnpjDocumentGenerator**: Gera fatura de hospedagem (empresa com CNPJ e ISS)
- **PassportDocumentGenerator**: Gera booking receipt em inglês (estrangeiro com passaporte)

### Serviços (`com.pousada.service`)
- **ReservationService**: Lógica de negócio para reservas
- **ReportService**: Gera relatórios de ocupação e receita

### Utilitários (`com.pousada.util`)
- **PriceCalculator**: Cálculo de preços com alta temporada e descontos
- **Formatter**: Formatação de datas, valores e documentos

### Banco de Dados (`com.pousada.db`)
- **Database**: Inicializa conexão e cria tabelas

### Handlers (`com.pousada.api.handler`)
- **RoomHandler**: GET /rooms
- **ReservationHandler**: CRUD de reservas e operações
- **MessageHandler**: GET /messages
- **ReportHandler**: Relatórios CSV

## Características

### Quartos
- 101/102: STANDARD - 2 pessoas - R$ 249,90/noite
- 201/202: DELUXE - 3 pessoas - R$ 389,90/noite
- 301: SUITE - 4 pessoas - R$ 579,90/noite

### Temporadas
- Alta temporada (+20%): 15/12 a 28/02 e julho inteiro
- Baixa temporada: resto do ano

### Planos de Tarifa
- STANDARD: preço normal, reembolso total se cancelado
- NON_REFUNDABLE: 15% de desconto, sem reembolso se cancelado

### Identificação do Hóspede (escolha uma):
1. **CPF**: Pessoa física brasileira → Comprovante de Reserva
2. **CNPJ + Razão Social**: Empresa → Fatura de Hospedagem com ISS
3. **Passaporte + País**: Estrangeiro → Booking Receipt (inglês)

### Fluxo de Reserva
```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
   ↓(cancelar)
CANCELLED
```

## APIs

### Rooms
```
GET /rooms
```
Retorna lista de quartos com capacidade e diária.

### Reservations
```
POST /reservations
GET /reservations/{id}
POST /reservations/{id}/confirmation
POST /reservations/{id}/check-in
POST /reservations/{id}/check-out
POST /reservations/{id}/cancellation
GET /reservations/{id}/document
```

### Messages
```
GET /messages
```
Retorna lista de e-mails enviados (confirmação de reserva).

### Reports
```
GET /reports/occupancy?date=2027-07-10
GET /reports/revenue?month=2027-07
```
Retorna relatórios em CSV (charset UTF-8).

## Exemplo de Criação de Reserva

```json
POST /reservations
{
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

## Arredondamento de Valores

Todos os valores monetários são arredondados usando HALF_EVEN (meio para o par) em cada etapa:
1. Preço da noite (com ajuste de alta temporada)
2. Soma das noites
3. Desconto (se NON_REFUNDABLE)
4. Total (soma - desconto)

## Padrões de Projeto Utilizados

- **Strategy**: Diferentes geradores de documentos (CpfDocumentGenerator, CnpjDocumentGenerator, PassportDocumentGenerator)
- **Factory Method**: DocumentGeneratorFactory para criar o gerador correto
- **Singleton**: Services e Repositories
