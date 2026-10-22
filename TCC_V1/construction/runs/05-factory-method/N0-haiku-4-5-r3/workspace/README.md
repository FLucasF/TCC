# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Compilação

```bash
mvn clean package
```

O JAR será gerado em `target/mare-alta-reservations-1.0.0.jar`.

## Execução

```bash
java -jar target/mare-alta-reservations-1.0.0.jar
```

### Variáveis de Ambiente Obrigatórias

- `DB_URL`: URL de conexão do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados

### Variáveis de Ambiente Opcionais

- `PORT`: Porta para o servidor (padrão: 7070)

## Endpoints da API

### Quartos
- `GET /rooms` - Lista todos os quartos com capacidade e diária

### Reservas
- `POST /reservations` - Cria uma nova reserva (status PENDING)
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma pagamento (PENDING → CONFIRMED)
- `POST /reservations/{id}/cancellation` - Cancela a reserva
- `POST /reservations/{id}/check-in` - Faz check-in (CONFIRMED → CHECKED_IN)
- `POST /reservations/{id}/check-out` - Faz check-out (CHECKED_IN → CHECKED_OUT)
- `GET /reservations/{id}/document` - Obtém cópia do documento (segunda via)

### Mensagens
- `GET /messages` - Lista de e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Ocupação de quartos por data (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Receita do mês (CSV)

## Identificação de Hóspedes

Três formas de identificação na criação de reserva:

1. **CPF**: Para hóspedes pessoas físicas
   ```json
   {
     "guestCpf": "11122233344",
     ...
   }
   ```

2. **CNPJ**: Para empresas
   ```json
   {
     "companyCnpj": "12345678000190",
     "companyName": "Empresa Ltda",
     ...
   }
   ```

3. **Passaporte**: Para estrangeiros
   ```json
   {
     "guestPassport": "X1234567",
     "guestCountry": "United Kingdom",
     ...
   }
   ```

## Recursos

- **5 quartos**: 2 STANDARD (2 pessoas), 2 DELUXE (3 pessoas), 1 SUITE (4 pessoas)
- **Alta temporada**: Julho inteiro + 15/12 a 28/02 (multiplicador: 20%)
- **Planos de tarifa**: STANDARD e NON_REFUNDABLE (15% desconto)
- **Documentos**: Comprovante (PT), Fatura com ISS (PT), Booking Receipt (EN)
- **Arredondamento**: Meio para o par (HALF_EVEN) em todas as operações monetárias
- **Cálculo preciso**: Preço calculado noite por noite, considerando mudanças de estação

## Teste da Compilação

```bash
mvn verify
```

## Estrutura do Projeto

```
src/main/java/com/pousada/
├── Main.java                    - Entrada da aplicação
├── config/
│   └── DatabaseConfig.java      - Configuração e inicialização do banco
├── model/
│   ├── Room.java
│   ├── Reservation.java
│   └── Message.java
├── dao/
│   ├── RoomDAO.java
│   ├── ReservationDAO.java
│   └── MessageDAO.java
├── service/
│   ├── ReservationService.java
│   ├── MessageService.java
│   └── ReportService.java
├── handler/
│   ├── RoomHandler.java
│   ├── ReservationHandler.java
│   ├── MessageHandler.java
│   ├── OccupancyReportHandler.java
│   └── RevenueReportHandler.java
└── util/
    ├── MoneyUtil.java          - Cálculos monetários
    └── DocumentFormatter.java   - Formatação de documentos
```

## Códigos de Erro

- `400 INVALID_REQUEST` - Requisição inválida (campos faltando, datas inválidas, etc)
- `404 ROOM_NOT_FOUND` - Quarto não existe
- `404 RESERVATION_NOT_FOUND` - Reserva não existe
- `409 ROOM_UNAVAILABLE` - Quarto já tem reserva naquelas datas
- `409 OPERATION_NOT_ALLOWED` - Operação não permitida para o status atual
- `422 CAPACITY_EXCEEDED` - Número de hóspedes maior que capacidade do quarto
