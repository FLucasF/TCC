# Detalhes da Implementação - Pousada Maré Alta

## Arquitetura

O sistema segue uma arquitetura em camadas com separação clara de responsabilidades:

```
┌─────────────────────────────────────────┐
│         API REST (Javalin)              │
│    ReservationAPI.java                  │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│      Serviço de Negócio                 │
│   ReservationService.java               │
└──────────────────┬──────────────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
┌───────▼────────┐  ┌────────▼─────────┐
│  Domínio       │  │  Persistência    │
│  (entidades +  │  │  Database.java   │
│   estratégias) │  │                  │
└────────────────┘  └──────────────────┘
```

## Padrão de Design: Strategy

O padrão Strategy foi aplicado aos planos de tarifa, permitindo adicionar novos planos sem modificar o código existente.

### Interface RatePlan

```java
public interface RatePlan {
    ValidationError validate(...);  // Valida se pode reservar
    PricingResult calculatePrice(...);  // Calcula preço final
    double getRefundPercentage();  // % de reembolso ao cancelar
}
```

### Implementações

1. **StandardRatePlan**: Tarifa normal, sem desconto
2. **NonRefundableRatePlan**: 15% desconto, 0% reembolso
3. **CorporateRatePlan**: Desconto por empresa (12%, 18%, 25%), 100% reembolso
4. **WeekendPackageRatePlan**: Preço fixo (sex→dom), 50% reembolso
5. **LongStayRatePlan**: 7+ noites com 10% desconto, 100% reembolso

### Adicionando Novo Plano

Para adicionar um novo plano, basta:

1. Criar classe `NovoRatePlan implements RatePlan`
2. Implementar os 3 métodos da interface
3. Registrar em `RatePlanFactory.create(RatePlanType)`
4. Adicionar entrada em enum `RatePlanType`

## Cálculo de Preços

### Arredondamento (Banker's Rounding)

Usa `BigDecimal.setScale(2, RoundingMode.HALF_EVEN)` para o arredondamento "meio para o par":
- 249,895 → 249,90 (arredonda para cima, para par)
- 249,885 → 249,88 (arredonda para cima, para par)

### Alta Temporada

- **Julho**: mês inteiro
- **Dezembro-Fevereiro**: 15/12 a 28/02
- Acréscimo: 20% sobre a diária

### Sequência de Cálculo

Para cada noite:
```
preço_noite = diária_base
if (alta_temporada)
    preço_noite = arredondar(preço_noite × 1,20)

subtotal = arredondar(soma de preços_noite)

if (tem_desconto)
    desconto = arredondar(subtotal × percentual_desconto)
else
    desconto = 0

total = arredondar(subtotal - desconto)
```

## Banco de Dados

### Tabelas

#### rooms
```sql
CREATE TABLE rooms (
    number INT PRIMARY KEY,
    type VARCHAR(20) NOT NULL
);
```

Dados iniciais:
- 101, 102: STANDARD
- 201, 202: DELUXE
- 301: SUITE

#### reservations
```sql
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    room_number INT NOT NULL,
    guest_name VARCHAR(255),
    guest_email VARCHAR(255),
    guest_cpf VARCHAR(20),
    check_in DATE,
    check_out DATE,
    guests INT,
    rate_plan VARCHAR(50),
    company_code VARCHAR(50),
    nights INT,
    subtotal NUMERIC(10,2),
    discount NUMERIC(10,2),
    total NUMERIC(10,2),
    status VARCHAR(20),
    amount_paid NUMERIC(10,2),
    refund NUMERIC(10,2),
    created_at TIMESTAMP
);
```

#### messages
```sql
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    channel VARCHAR(50),
    "to" VARCHAR(255),
    subject VARCHAR(500),
    body TEXT,
    created_at TIMESTAMP
);
```

## Fluxo de Estados da Reserva

```
┌────────┐
│PENDING │  ← Criação
└─────┬──┘
      │
      │ confirmReservation()
      ▼
┌───────────┐
│CONFIRMED  │  ← Pagamento recebido, email enviado
└─────┬─────┘
      │
      │ checkIn()
      ▼
┌──────────┐
│CHECKED_IN│
└─────┬────┘
      │
      │ checkOut()
      ▼
┌───────────┐
│CHECKED_OUT│  ← Final
└────────────┘

Cancelamento (antes do check-in):
PENDING/CONFIRMED --cancelReservation()--> CANCELLED
```

## Validações

### Por Plano

1. **STANDARD/NON_REFUNDABLE/CORPORATE/LONG_STAY**:
   - Mínimo 1 hóspede
   - Hóspedes ≤ capacidade do quarto
   - Check-out > check-in
   - Mínimo 1 noite

2. **CORPORATE** (adicional):
   - Código de empresa obrigatório
   - Código deve estar em TECNOSUL/AGROVALE/FARMAVIDA

3. **WEEKEND_PACKAGE** (adicional):
   - Check-in deve ser sexta
   - Check-out deve ser domingo (imediatamente após)

4. **LONG_STAY** (adicional):
   - Mínimo 7 noites

### Por Operação

1. **Confirmação**: apenas de PENDING
2. **Check-in**: apenas de CONFIRMED
3. **Check-out**: apenas de CHECKED_IN
4. **Cancelamento**: apenas de PENDING ou CONFIRMED

## Relatórios

### Ocupação (CSV)

```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

Filtra: status IN (CONFIRMED, CHECKED_IN, CHECKED_OUT)
Critério: check_in ≤ data AND check_out > data

### Receita (CSV)

```csv
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
total,,,,,599.76
```

Filtra: amount_paid > 0 AND check_in no mês
Cálculo: amount = amount_paid - (refund ou 0)

## Comprovante

### Formato

```
Comprovante de reserva nº 7
Hóspede: Carla Dias (CPF 111.222.333-44)
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Desconto: R$ 93,58
Total: R$ 686,22
```

- Números formatados com `Money.formatBr()`: R$ 1.234,56
- Datas formatadas com `DateUtils.formatBr()`: 10/03/2027
- Noites no singular/plural: "1 diária" ou "2 diárias"

### Email de Confirmação

Subject: "Reserva confirmada - Pousada Maré Alta"

```
Olá, <guestName>!

Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

<comprovante>

A entrada é a partir das 14h, e a saída, até as 12h.
Até breve!
```

## Testes

### Cobertura

- `DateUtilsTest`: testes de cálculo de datas e alta temporada
- `MoneyTest`: testes de arredondamento banker's rounding

### Execução

```bash
mvn test      # Testes unitários apenas
mvn verify    # Testes + build + verificações
```

## Performance e Escalabilidade

### Índices (Recomendados)

```sql
CREATE INDEX idx_reservations_room_dates ON reservations(room_number, check_in, check_out);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_check_in ON reservations(check_in);
```

### Limites

- Sem limite de requisições por segundo
- Cada requisição criar conexão com o banco (melhorar com pool)
- Relatórios sem paginação (OK para pequeno volume)

## Segurança

### Validações

- Sempre valida entrada JSON
- Valida domínios de enumerações
- Valida intervalos de datas e quantidade de hóspedes

### Sem Implementação

- Autenticação/autorização (recepcão vs site vs contador)
- Criptografia de CPF no banco
- Rate limiting
- Sanitização de entrada (não há injeção SQL com prepared statements)

## Extensões Futuras

### Novos Planos

1. **SENIOR_DISCOUNT**: Desconto para melhor idade
   - Validação: idade ≥ 60
   - Desconto: 25%

2. **EARLY_BOOKING**: Antecipação
   - Validação: 60+ dias antes
   - Desconto: 20%

3. **BLACK_FRIDAY**: Datas específicas
   - Validação: check-in em datas pré-definidas
   - Desconto: 35%

### Integrações

- Enviar emails realmente (SMTP)
- SMS confirmação de check-in
- Notificações push
- Integração com gateway de pagamento
- Sincronização com Google Calendar
- Integração com VRBO/Airbnb
