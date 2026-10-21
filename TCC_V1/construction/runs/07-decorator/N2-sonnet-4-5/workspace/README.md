# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para pousada, desenvolvido em Java 21 com Javalin 6.7.0.

## Características

- **Gestão de Quartos**: 5 quartos com tipos STANDARD, DELUXE e SUITE
- **Reservas**: Criação, confirmação, check-in, check-out e cancelamento
- **Cálculo de Preços**: Alta temporada (+20%) e plano não-reembolsável (-15%)
- **Sistema de Mensagens**: Validação de e-mail, cópia para arquivo e modo de teste
- **Relatórios**: Ocupação diária e receita mensal em CSV

## Arquitetura

O sistema de mensagens utiliza o padrão **Chain of Responsibility** para processar e-mails através de regras encadeadas:

- **EmailValidationRule**: Valida e bloqueia e-mails inválidos
- **TestModeRule**: Redireciona mensagens para e-mail de teste
- **ArchiveCopyRule**: Cria cópia automática para arquivo

Novas regras podem ser adicionadas implementando `MessageRule` e registrando na cadeia.

## Compilar e Executar

```bash
# Compilar
mvn clean package

# Executar
java -jar target/reservas-1.0-SNAPSHOT.jar

# Executar em modo de teste
HOTEL_TEST_MODE=true java -jar target/reservas-1.0-SNAPSHOT.jar

# Executar em porta customizada
PORT=8080 java -jar target/reservas-1.0-SNAPSHOT.jar
```

## Testes

```bash
mvn test      # Executar testes
mvn verify    # Verificar build completo
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Buscar reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório de receita

## Exemplo de Uso

```bash
# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva (envia e-mail)
curl -X POST http://localhost:7070/reservations/1/confirmation

# Listar mensagens enviadas
curl http://localhost:7070/messages

# Relatório de ocupação
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── domain/          # Modelos de domínio
├── service/         # Lógica de negócio
├── messaging/       # Sistema de mensagens (Chain of Responsibility)
├── api/             # Controllers REST
└── App.java         # Ponto de entrada
```
