# Arquitetura do Sistema

## Visão Geral

O sistema foi organizado seguindo as orientações do CLAUDE.md, com foco em facilitar a adição de novos casos sem modificar código existente.

## Estrutura de Pacotes

```
com.pousada.maralta/
├── domain/              # Modelos de domínio e regras de negócio
│   ├── Room
│   ├── Reservation
│   ├── Message
│   ├── GuestIdentification (sealed interface)
│   │   ├── CpfIdentification
│   │   ├── CnpjIdentification
│   │   └── PassportIdentification
│   └── enums (RoomType, ReservationStatus, RatePlan)
├── service/            # Lógica de aplicação
│   ├── ReservationService
│   ├── PricingService
│   ├── DocumentGenerator
│   ├── EmailService
│   └── ReportService
├── repository/         # Acesso a dados
│   ├── RoomRepository
│   ├── ReservationRepository
│   ├── MessageRepository
│   └── implementações JDBC
├── controller/         # API REST
│   ├── ApiController
│   └── dto/
└── infrastructure/     # Configuração
    └── DatabaseConfig
```

## Padrão de Design: Visitor

A principal variação do sistema é o tipo de identificação do hóspede, que determina qual documento será gerado:

- **CPF** → Comprovante em português
- **CNPJ** → Fatura com ISS
- **Passaporte** → Booking receipt em inglês
- **(Futuro) Agência** → Documento específico para agências

### Como funciona

1. **Sealed Interface**: `GuestIdentification` é uma sealed interface que lista todos os tipos possíveis
   
2. **Visitor Pattern**: `GuestIdentificationVisitor<T>` permite processar cada tipo de forma diferente

3. **Geração de Documentos**: `DocumentGenerator` usa dois visitors:
   - Um para construir o objeto `ReservationDocument`
   - Outro para gerar o texto do documento

### Adicionando um novo tipo de documento

Para adicionar suporte a agências de turismo:

1. Criar `AgencyIdentification implements GuestIdentification`:
```java
public record AgencyIdentification(String agencyCode, String agencyName) 
    implements GuestIdentification {
    @Override
    public <T> T accept(GuestIdentificationVisitor<T> visitor) {
        return visitor.visitAgency(this);
    }
}
```

2. Adicionar método em `GuestIdentificationVisitor`:
```java
T visitAgency(AgencyIdentification agency);
```

3. Implementar a geração do documento nos visitors de `DocumentGenerator`

4. Adicionar suporte no `ApiController.parseIdentification()`

**Nenhum código existente precisa ser modificado** - apenas estendido.

## Cálculo de Preços

O `PricingService` aplica as regras:

1. **Preço base**: vem do `RoomType`
2. **Alta temporada**: multiplica por 1.20 se a data for:
   - 15/12 até fim de fevereiro
   - Julho inteiro
3. **Subtotal**: soma das noites (cada uma arredondada)
4. **Desconto**: 15% se NON_REFUNDABLE, 0% se STANDARD
5. **Total**: subtotal - desconto

Cada passo usa `RoundingMode.HALF_EVEN` (arredondamento "meio para o par").

## Fluxo de Estados da Reserva

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
    ↓          ↓
  CANCELLED  CANCELLED
```

- **confirm()**: paga o total
- **cancel()**: 
  - STANDARD: devolve o que foi pago
  - NON_REFUNDABLE: não devolve nada
- **checkIn()** e **checkOut()**: mudam apenas o estado

## Persistência

- **PostgreSQL 18**: banco principal
- **HikariCP**: pool de conexões
- **JDBC puro**: sem ORM
- **Migrations**: executadas no `DatabaseConfig.initialize()`

As tabelas são criadas automaticamente na primeira execução.

## API REST

Javalin 6.7.0 com Jackson para JSON:

- Datas no formato ISO (yyyy-MM-dd)
- Valores monetários como números com 2 casas (599.76)
- Erros com código e mensagem

### Tratamento de Erros

| Código | Situação |
|--------|----------|
| 400 INVALID_REQUEST | Campo faltando, JSON inválido, formato de data errado, etc. |
| 404 ROOM_NOT_FOUND | Quarto não existe |
| 404 RESERVATION_NOT_FOUND | Reserva não existe |
| 409 ROOM_UNAVAILABLE | Quarto já reservado nas datas |
| 409 OPERATION_NOT_ALLOWED | Operação não permitida no estado atual |
| 422 CAPACITY_EXCEEDED | Mais hóspedes do que a capacidade |

## Testes

- `PricingServiceTest`: valida cálculos de preço
- `ReservationFlowTest`: valida fluxo completo de reserva

Para rodar:
```bash
mvn test
mvn verify
```

## Configuração

Variáveis de ambiente:

- `DB_URL`: jdbc:postgresql://host:port/database
- `DB_USER`: usuário do banco
- `DB_PASSWORD`: senha do banco
- `PORT`: porta do servidor (padrão 7070)

## Build

```bash
mvn package
```

Gera `target/maralta-1.0.0.jar` executável:

```bash
java -jar target/maralta-1.0.0.jar
```
