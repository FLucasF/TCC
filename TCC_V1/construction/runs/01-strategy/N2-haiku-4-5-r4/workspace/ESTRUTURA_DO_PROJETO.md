# Estrutura do Projeto - Pousada Maré Alta

## Arquivos e Diretórios

```
pousada-marae-alta/
│
├── src/
│   ├── main/java/br/com/mareaalta/
│   │   ├── Application.java                    # Entrada da aplicação
│   │   ├── api/
│   │   │   └── ReservationAPI.java            # Endpoints REST
│   │   ├── domain/
│   │   │   ├── Message.java                   # Entidade: Mensagem
│   │   │   ├── Reservation.java               # Entidade: Reserva
│   │   │   ├── Room.java                      # Entidade: Quarto
│   │   │   ├── RatePlanType.java              # Enum: Tipo de plano
│   │   │   ├── ReservationStatus.java         # Enum: Status da reserva
│   │   │   ├── RoomType.java                  # Enum: Tipo de quarto
│   │   │   └── rateplan/
│   │   │       ├── RatePlan.java              # Interface: Estratégia
│   │   │       ├── StandardRatePlan.java      # Plano: Normal
│   │   │       ├── NonRefundableRatePlan.java # Plano: Não reembolsável
│   │   │       ├── CorporateRatePlan.java     # Plano: Convênio
│   │   │       ├── WeekendPackageRatePlan.java # Plano: Fim de semana
│   │   │       ├── LongStayRatePlan.java      # Plano: Estadia longa
│   │   │       └── RatePlanFactory.java       # Factory: Criação de planos
│   │   ├── repository/
│   │   │   └── Database.java                  # Acesso ao PostgreSQL
│   │   ├── service/
│   │   │   └── ReservationService.java        # Lógica de negócio
│   │   └── util/
│   │       ├── DateUtils.java                 # Utilitários de data
│   │       └── Money.java                     # Utilitários de moeda
│   └── test/java/br/com/mareaalta/
│       ├── DateUtilsTest.java                 # Testes de data
│       └── MoneyTest.java                     # Testes de arredondamento
│
├── pom.xml                                     # Configuração Maven
├── Dockerfile                                  # Build Docker
├── docker-compose.yml                          # Orquestração Docker
├── .gitignore                                  # Arquivos ignorados pelo Git
│
├── README.md                                   # Guia principal
├── IMPLEMENTACAO.md                            # Detalhes técnicos
├── EXEMPLOS_API.md                             # Exemplos de requisições
└── ESTRUTURA_DO_PROJETO.md                     # Este arquivo

target/
├── pousada.jar                                # JAR executável (7.2 MB)
└── ...                                        # Outros arquivos de build
```

## Arquivo: pom.xml

Dependências principais:
- **Javalin 6.7.0**: Framework web
- **PostgreSQL Driver 42.7.3**: Conexão com banco
- **Gson 2.10.1**: Serialização JSON
- **SLF4J 2.0.11**: Logging
- **Maven Shade Plugin**: Gera JAR executável (uber jar)

## Classe: Application

Ponto de entrada da aplicação. Lê variáveis de ambiente, inicializa banco e inicia servidor.

```java
public class Application {
    public static void main(String[] args) {
        // 1. Lê DB_URL, DB_USER, DB_PASSWORD, PORT
        // 2. Inicializa banco (cria tabelas e dados padrão)
        // 3. Cria serviço de negócio
        // 4. Inicia Javalin na porta
    }
}
```

## Classe: ReservationAPI

Define todos os endpoints HTTP:

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | /rooms | Lista quartos |
| POST | /reservations | Cria reserva |
| GET | /reservations/{id} | Obtém reserva |
| POST | /reservations/{id}/confirmation | Confirma pagamento |
| POST | /reservations/{id}/check-in | Check-in |
| POST | /reservations/{id}/check-out | Check-out |
| POST | /reservations/{id}/cancellation | Cancela |
| GET | /reservations/{id}/document | Segunda via |
| GET | /messages | Mensagens enviadas |
| GET | /reports/occupancy?date=... | Ocupação (CSV) |
| GET | /reports/revenue?month=... | Receita (CSV) |

## Entidades do Domínio

### Room
- `number`: Número (101-301)
- `type`: Tipo (STANDARD/DELUXE/SUITE)

### Reservation
- `id`: Auto-incrementado
- `roomNumber`, `guestName`, `guestEmail`, `guestCpf`
- `checkIn`, `checkOut`: LocalDate
- `guests`: Quantidade de hóspedes
- `ratePlan`: Tipo do plano
- `companyCode`: Código da empresa (para CORPORATE)
- `nights`, `subtotal`, `discount`, `total`
- `status`: PENDING/CONFIRMED/CHECKED_IN/CHECKED_OUT/CANCELLED
- `amountPaid`: Valor pago
- `refund`: Valor devolvido (null até não ser cancelada)

### Message
- `id`: Auto-incrementado
- `channel`: EMAIL (padrão)
- `to`: Endereço de email
- `subject`: Assunto
- `body`: Corpo do email

## Padrão Strategy: RatePlan

Cada plano implementa:

```java
interface RatePlan {
    ValidationError validate(...);       // Validações específicas
    PricingResult calculatePrice(...);   // Cálculo do preço
    double getRefundPercentage();        // % de reembolso
}
```

### Implementações

| Classe | Validação | Cálculo | Reembolso |
|--------|-----------|---------|-----------|
| StandardRatePlan | 1+ hóspede | Noites × diária (20% alta temporada) | 100% |
| NonRefundableRatePlan | Idem | Idem - 15% desconto | 0% |
| CorporateRatePlan | Empresa válida | Idem - % da empresa | 100% |
| WeekendPackageRatePlan | Sexta→Domingo | Preço fixo | 50% |
| LongStayRatePlan | 7+ noites | Noites × diária - 10% | 100% |

## Utilitários

### DateUtils
- `isHighSeason(LocalDate)`: Verifica se está em alta temporada
- `nightsBetween(LocalDate, LocalDate)`: Calcula número de noites
- `format(LocalDate)`: Formato ISO (2027-07-10)
- `formatBr(LocalDate)`: Formato BR (10/07/2027)

### Money
- `round(double)`: Banker's rounding (HALF_EVEN)
- `format(double)`: 599.76
- `formatBr(double)`: R$ 599,76

## Fluxo de Dados

### Criar Reserva

```
POST /reservations (JSON)
         ↓
ReservationAPI.createReservation()
         ↓
ReservationService.createReservation()
         ↓
RatePlanFactory.create(tipo)
         ↓
RatePlan.validate()  ← Valida
         ↓
RatePlan.calculatePrice()  ← Calcula preço
         ↓
Database.createReservation()  ← Persiste
         ↓
Resposta 201 com Reservation
```

### Confirmar Pagamento

```
POST /reservations/{id}/confirmation
         ↓
ReservationService.confirmReservation()
         ↓
Database.updateReservation()  ← Atualiza status
         ↓
sendConfirmationEmail()  ← Gera email
         ↓
Database.saveMessage()  ← Armazena email
         ↓
Resposta 200 com Reservation
```

## Build e Execução

### Compilar
```bash
mvn clean package
```
Gera: `target/pousada.jar` (7.2 MB, uber jar)

### Testes
```bash
mvn verify
```
5 testes: DateUtils e Money

### Executar

**Opção 1: Localmente**
```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=pousada \
DB_PASSWORD=pousada123 \
java -jar target/pousada.jar
```

**Opção 2: Docker**
```bash
docker-compose up
```

## Variáveis de Ambiente

| Variável | Obrigatória | Padrão | Exemplo |
|----------|-------------|--------|---------|
| DB_URL | Sim | - | jdbc:postgresql://localhost:5432/pousada |
| DB_USER | Sim | - | pousada |
| DB_PASSWORD | Sim | - | pousada123 |
| PORT | Não | 7070 | 8080 |

## Banco de Dados

**Inicialização**: Automática na primeira execução

Tabelas criadas:
- `rooms`: 5 quartos pré-configurados
- `reservations`: Histórico completo
- `messages`: Emails enviados (auditoria)

## Validações de Entrada

- JSON inválido → 400 INVALID_REQUEST
- Campo obrigatório faltando → 400 INVALID_REQUEST
- Data/mês em formato errado → 400 INVALID_REQUEST
- Quarto não existe → 404 ROOM_NOT_FOUND
- Reserva não existe → 404 RESERVATION_NOT_FOUND
- Quarto ocupado → 409 ROOM_UNAVAILABLE
- Operação não permitida no estado → 409 OPERATION_NOT_ALLOWED
- Hóspedes > capacidade → 422 CAPACITY_EXCEEDED

## Testes

**DateUtilsTest**
- Alta temporada (julho, dez-fev)
- Cálculo de noites
- Datas de fim de semana

**MoneyTest**
- Banker's rounding
- Formatação

Todos passando ✓

## Próximas Etapas Recomendadas

1. **Pool de Conexão**: HikariCP para melhor performance
2. **Autenticação**: JWT ou OAuth2 para diferentes perfis
3. **Testes Integrados**: Testcontainers para PostgreSQL em testes
4. **Logs Estruturados**: JSON logs com ELK Stack
5. **Monitoramento**: Micrometer + Prometheus
6. **Email Real**: Integração com SMTP (SendGrid, AWS SES)
7. **Documentação Interativa**: Swagger/OpenAPI
