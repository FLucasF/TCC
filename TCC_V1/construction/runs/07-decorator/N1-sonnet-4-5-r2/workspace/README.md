# Pousada Maré Alta - Sistema de Reservas

Sistema de reservas para pousadas, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL.

## Arquitetura

O sistema foi projetado seguindo o princípio Open/Closed: aberto para extensão, fechado para modificação. O ponto central do design está no sistema de **políticas de mensagens**.

### Sistema de Políticas de Mensagens

Cada regra de envio de mensagens é uma classe independente que implementa `MessagePolicy`:

- **BlockInvalidEmailPolicy**: Valida e bloqueia e-mails inválidos
- **FileCopyPolicy**: Gera cópia automática para arquivo@pousadamarealta.com.br
- **TestModePolicy**: Redireciona mensagens para testes@pousadamarealta.com.br no modo de teste

Para adicionar uma nova regra (ex: limite de mensagens por dia, bloqueio de horário noturno):
1. Crie uma nova classe implementando `MessagePolicy`
2. Adicione-a à lista de políticas em `MessageProcessor.createDefault()`
3. **Não é necessário modificar nenhuma classe existente**

### Estrutura do Projeto

```
src/main/java/com/pousada/
├── Main.java                      # Ponto de entrada da aplicação
├── api/                           # Camada de API REST
│   ├── ApiController.java         # Endpoints REST
│   ├── ApiMappers.java            # Conversão domain → JSON
│   └── ErrorResponse.java         # Estrutura de erros
├── domain/                        # Modelos de negócio
│   ├── Room.java
│   ├── Reservation.java
│   ├── Message.java
│   └── enums (RoomType, RatePlan, ReservationStatus, etc.)
├── messaging/                     # Sistema de políticas
│   ├── MessagePolicy.java         # Interface de política
│   ├── PolicyResult.java          # Resultado da aplicação de políticas
│   ├── MessageProcessor.java      # Orquestrador de políticas
│   ├── BlockInvalidEmailPolicy.java
│   ├── FileCopyPolicy.java
│   └── TestModePolicy.java
├── service/                       # Lógica de negócio
│   ├── RoomService.java
│   ├── ReservationService.java
│   ├── MessageService.java
│   └── ReportService.java
├── repository/                    # Acesso a dados
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
└── infrastructure/                # Infraestrutura
    └── Database.java              # Setup do banco de dados
```

## Regras de Negócio

### Quartos

- **101, 102**: STANDARD, 2 pessoas, R$ 249,90/noite
- **201, 202**: DELUXE, 3 pessoas, R$ 389,90/noite
- **301**: SUITE, 4 pessoas, R$ 579,90/noite

### Preços

- **Alta temporada** (15/dez a fim de fev + mês de julho): +20% na diária
- **Plano não reembolsável**: -15% no total (sobre a soma das diárias)
- **Arredondamento**: HALF_EVEN (meio para o par) em cada etapa

### Fluxo de Reserva

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
    ↓
CANCELLED (antes do check-in)
```

### Regras de Mensagens

1. **Validação de e-mail**: Bloqueia endereços sem @ ou sem domínio válido
2. **Cópia para arquivo**: Toda mensagem enviada gera cópia com "[Cópia]" no assunto
3. **Modo de teste**: Com `HOTEL_TEST_MODE=true`, redireciona para testes@... com "[TESTE]"

## API REST

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Configuração

### Variáveis de Ambiente

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada
DB_USER=postgres
DB_PASSWORD=postgres
PORT=7070
HOTEL_TEST_MODE=true  # Ativa modo de teste
```

### Executando

```bash
# Compilar
mvn clean package

# Executar
java -jar target/marealta-1.0.0.jar

# Ou com variáveis de ambiente
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=user \
DB_PASSWORD=pass \
PORT=8080 \
java -jar target/marealta-1.0.0.jar
```

### Testes

```bash
# Testes de integração com Testcontainers
mvn verify
```

## Design Principles

O código segue as orientações do CLAUDE.md:

> "Antes de escrever cada parte, pergunte: para acrescentar o próximo caso parecido com este, quantos lugares do código teriam de mudar?"

**Exemplo prático**: Para adicionar uma nova regra de mensagem ("não enviar após às 22h"):

```java
public class NoNightTimePolicy implements MessagePolicy {
    @Override
    public void apply(PolicyResult result) {
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(22, 0))) {
            result.transform(PolicyResult.block(result.getMessage()).getMessage());
        }
    }
}
```

Adicione em `MessageProcessor.createDefault()`:
```java
policies.add(new NoNightTimePolicy());
```

**Nenhuma outra classe precisa ser modificada.**
