# Arquitetura - Pousada Maré Alta

Documentação da estrutura e organização do código.

## Estrutura de Pacotes

```
com.pousada.mare_alta/
├── Main.java                          # Entrada da aplicação
├── model/                             # Entidades de domínio
│   ├── Room.java                      # Quarto
│   ├── RoomType.java                  # Enum: STANDARD, DELUXE, SUITE
│   ├── Reservation.java               # Reserva (imutável)
│   ├── ReservationStatus.java         # Enum: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
│   ├── RatePlan.java                  # Enum: STANDARD, NON_REFUNDABLE
│   ├── Document.java                  # Documento (Comprovante/Fatura/Booking)
│   └── Message.java                   # E-mail armazenado
├── repository/                        # Acesso a dados (JDBC)
│   ├── RoomRepository.java            # Operações sobre quartos
│   ├── ReservationRepository.java     # Operações sobre reservas
│   └── MessageRepository.java         # Operações sobre mensagens
├── service/                           # Lógica de negócio
│   ├── ReservationService.java        # Orquestração de reservas
│   └── ReportService.java             # Geração de relatórios
├── http/                              # Camada HTTP (Javalin)
│   ├── AppRoutes.java                 # Definição de endpoints
│   ├── request/                       # Modelos de requisição
│   │   └── CreateReservationRequest.java
│   └── response/                      # Modelos de resposta
│       ├── ReservationResponse.java
│       ├── RoomResponse.java
│       ├── DocumentResponse.java
│       ├── MessageResponse.java
│       └── ErrorResponse.java
├── document/                          # Strategy Pattern para documentos
│   ├── DocumentFormatter.java         # Interface da estratégia
│   ├── DocumentData.java              # Dados para formatação
│   ├── DocumentFormatterFactory.java  # Factory para seleção
│   ├── CpfDocumentFormatter.java      # Estratégia: Comprovante em português
│   ├── CnpjDocumentFormatter.java     # Estratégia: Fatura em português com ISS
│   └── PassportDocumentFormatter.java # Estratégia: Booking em inglês
├── database/                          # Setup do banco de dados
│   └── Database.java                  # Inicialização e gerenciamento de conexões
└── util/                              # Utilitários
    ├── NumberFormatter.java           # Formatação de moeda com arredondamento HALF_EVEN
    └── DateUtil.java                  # Cálculo de alta temporada
```

## Padrões de Design

### 1. Strategy Pattern (Document Formatters)
**Problema**: Três tipos de documentos com formatações completamente diferentes (português/inglês, CPF/CNPJ/Passaporte, etc).

**Solução**: 
```java
interface DocumentFormatter {
    Document format(DocumentData data);
}

class CpfDocumentFormatter implements DocumentFormatter { ... }
class CnpjDocumentFormatter implements DocumentFormatter { ... }
class PassportDocumentFormatter implements DocumentFormatter { ... }

// Factory para seleção
DocumentFormatter formatter = DocumentFormatterFactory.getFormatter(cpf, cnpj, passport);
Document doc = formatter.format(data);
```

**Benefício**: Adicionar novo tipo de documento é apenas uma classe nova + uma entrada no factory, sem mexer no resto do código.

### 2. Immutable Objects
**Padrão**: Todas as entidades (Room, Reservation, Document, Message) são imutáveis.

**Implementação**: Classes com `final` fields, construtores privados, métodos `with*` para criar versões modificadas.

```java
public class Reservation {
    private final long id;
    private final String guestName;
    // ... mais fields finais
    
    public Reservation withStatus(ReservationStatus newStatus) {
        return new Reservation(id, roomNumber, ..., newStatus, ...);
    }
}
```

**Benefício**: Thread-safe, facilita rastreamento de mudanças, evita efeitos colaterais.

### 3. Repository Pattern
**Padrão**: Acesso a dados isolado em classes Repository.

```java
class ReservationRepository {
    public Reservation create(Reservation r) { ... }
    public Optional<Reservation> findById(long id) { ... }
    public void update(Reservation r) { ... }
    public List<Reservation> findByRoomAndDateRange(...) { ... }
}
```

**Benefício**: Fácil de testar, trocar banco de dados no futuro, SQL centralizado.

## Fluxo de Dados

### 1. Criar Reserva
```
POST /reservations (JSON)
    ↓
CreateReservationRequest (desserialização)
    ↓
ReservationService.createReservation()
    - Validar inputs
    - Verificar disponibilidade (ReservationRepository)
    - Calcular preço com sazonalidade (DateUtil)
    - Arredondar valores (NumberFormatter)
    - Criar Reservation
    - Persistir (ReservationRepository.create)
    ↓
ReservationResponse (serialização para JSON)
```

### 2. Confirmar Reserva
```
POST /reservations/{id}/confirmation
    ↓
ReservationService.confirmReservation()
    - Obter reserva
    - Validar status
    - Atualizar para CONFIRMED
    - Gerar documento
    - Enviar email (MessageRepository.create)
    ↓
ReservationResponse (status 200 ou erro 409)
```

### 3. Check-out
```
POST /reservations/{id}/check-out
    ↓
ReservationService.checkOut()
    - Obter reserva
    - Validar status
    - Atualizar para CHECKED_OUT
    - Gerar documento
    ↓
DocumentResponse (com texto formatado)
```

## Validações

### Entrada (AppRoutes)
- Campos obrigatórios presentes
- Formato de data válido (yyyy-MM-dd)
- Rate plan válido

### Lógica (ReservationService)
- Pelo menos 1 hóspede
- Check-out após check-in
- Exatamente 1 tipo de identificação (CPF, CNPJ ou Passaporte)
- Quarto existe
- Número de hóspedes ≤ capacidade do quarto
- Quarto disponível nas datas (sem conflitos)
- Estado da reserva permite operação

### Banco de Dados (Schema)
- NOT NULL em campos obrigatórios
- FOREIGN KEY em room_number
- SERIAL PRIMARY KEY para auto-incremento

## Cálculo de Preços

### Alta Temporada
- 15 de dezembro a 28 de fevereiro (qualquer ano, sem validação de intervalo específico)
- Julho inteiro
- Multiplicador: 1.20 (20% acréscimo)

### Arredondamento
- Modo: HALF_EVEN (meio para par, arredondamento bancário)
- Aplicado em: cada noite, subtotal, desconto, total
- Escala: 2 casas decimais

### Desconto
- STANDARD: 0% desconto
- NON_REFUNDABLE: 15% sobre o subtotal

## Cancelamento

### Política de Reembolso

| Status atual | Plano STANDARD | Plano NON_REFUNDABLE |
|---|---|---|
| PENDING | Sem reembolso (não pagou) | Sem reembolso (não pagou) |
| CONFIRMED | Reembolso total | Sem reembolso (0.00) |

## Relatórios

### Ocupação (CSV)
- Filtra reservas pagas (CONFIRMED, CHECKED_IN, CHECKED_OUT) na data
- Uma linha por quarto ocupado
- Linha "total" com {ocupados}/{total}
- Formato: room,type,reservation,check_in,check_out

### Receita (CSV)
- Filtra reservas que começam no mês
- Calcula: amountPaid - refund
- Uma linha por reserva
- Linha "total" com soma
- Formato: reservation,room,check_in,check_out,status,amount

## Testes

### Unitários
- `NumberFormatterTest`: Validação de arredondamento e formatação
- `DateUtilTest`: Cálculo de alta temporada
- `CpfDocumentFormatterTest`: Formatação de documentos

### Integração
Requerem PostgreSQL. Podem ser adicionados em `src/test/java/com/pousada/mare_alta/integration/`.

## Variáveis de Ambiente

| Var | Obrigatório | Padrão | Exemplo |
|---|---|---|---|
| DB_URL | Sim | - | `jdbc:postgresql://localhost:5432/mare_alta` |
| DB_USER | Sim | - | `postgres` |
| DB_PASSWORD | Sim | - | `minha_senha` |
| PORT | Não | 7070 | `8080` |

## Próximas Melhorias

1. **Testes de Integração**: Usar Testcontainers para PostgreSQL
2. **Autenticação**: JWT para recepção/contador
3. **Transações**: @Transactional ou gerenciamento manual
4. **Logging**: Estruturado com SLF4J
5. **Validação**: Bean Validation (Hibernate Validator)
6. **Documentação API**: OpenAPI/Swagger
7. **Agências**: Implementar novo tipo de documento
