# Demonstração do Sistema

Este documento mostra os principais recursos do sistema implementados.

## ✅ Funcionalidades Implementadas

### 1. Gestão de Quartos
- 5 quartos cadastrados (101, 102, 201, 202, 301)
- 3 tipos: STANDARD, DELUXE, SUITE
- Capacidades e tarifas diferentes

### 2. Reservas com Estados
- PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- CANCELLED (pode ser feito antes do check-in)
- Validações completas

### 3. Cálculo de Preços
- Alta temporada (15/dez-28/fev + julho): +20%
- Planos de tarifa:
  - STANDARD: preço normal
  - NON_REFUNDABLE: -15% desconto, sem reembolso
- Arredondamento "meio para o par" (HALF_EVEN)
- Exemplo testado: 2 noites em julho com desconto = R$ 509,80

### 4. Notificações
- Email de confirmação automático
- Comprovante de reserva
- Segunda via do comprovante
- Mensagens armazenadas em memória

### 5. Relatórios (Template Method Pattern)
Todos seguem a mesma estrutura extensível:

**Ocupação** (`/reports/occupancy?date=YYYY-MM-DD`)
```csv
Pousada Maré Alta,occupancy,2027-03-10
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-03-10,2027-03-12
total,1/5
```

**Financeiro** (`/reports/revenue?month=YYYY-MM`)
```csv
Pousada Maré Alta,revenue,2027-03
reservation,room,check_in,check_out,status,amount
1,101,2027-03-10,2027-03-12,CONFIRMED,499.80
total,499.80
```

**Taxa de Turismo** (`/reports/tourism-tax?month=YYYY-MM`)
```csv
Pousada Maré Alta,tourism-tax,2027-03
reservation,room,check_in,check_out,guests,nights,tax
1,101,2027-03-10,2027-03-12,2,2,18.00
total,18.00
```
*Cálculo: R$ 4,50 por hóspede por noite*

**Planos de Tarifa** (`/reports/rate-plans?month=YYYY-MM`)
```csv
Pousada Maré Alta,rate-plans,2027-03
rate_plan,reservations,amount
STANDARD,1,499.80
NON_REFUNDABLE,0,0.00
total,1,499.80
```

## 🎯 Padrão de Design: Template Method

A estrutura de relatórios usa Template Method para garantir que:

1. **Novos relatórios** entram em um só lugar (nova classe que estende `CsvReport`)
2. **Mudanças na estrutura** aplicam automaticamente em todos
   - Exemplo: adicionar linha de data no final requer apenas 1 mudança na classe base

### Estrutura Implementada

```java
abstract class CsvReport {
    public final String generate() {
        // Sequência fixa de passos
        titulo → cabeçalho → dados → total
    }
    
    // Cada relatório implementa apenas suas partes específicas
    protected abstract String reportName();
    protected abstract List<String> dataLines();
    protected abstract String totalLine();
}
```

## 🔒 Validações

- ✅ Quarto não pode ter 2 reservas na mesma noite
- ✅ Capacidade máxima do quarto respeitada
- ✅ Mínimo de 1 hóspede
- ✅ Data de saída após data de entrada
- ✅ Transições de estado controladas
- ✅ Reembolsos conforme plano de tarifa

## 📊 Testes

- 24 testes unitários e de integração
- Cobertura de:
  - Cálculo de preços (alta temporada, descontos)
  - Transições de estado
  - Validações de negócio
  - Estrutura de relatórios
  - Taxa de turismo

Execute: `mvn verify`

## 🚀 Deploy

```bash
# Build
mvn package

# Executar
java -jar target/reservations-1.0-SNAPSHOT.jar

# Ou com porta customizada
PORT=8080 java -jar target/reservations-1.0-SNAPSHOT.jar
```

## 📝 Exemplo Completo

```bash
# 1. Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# 2. Confirmar (envia email automaticamente)
curl -X POST http://localhost:7070/reservations/1/confirmation

# 3. Check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# 4. Check-out (retorna comprovante)
curl -X POST http://localhost:7070/reservations/1/check-out

# 5. Ver mensagens enviadas
curl http://localhost:7070/messages

# 6. Relatórios
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
curl "http://localhost:7070/reports/revenue?month=2027-03"
curl "http://localhost:7070/reports/tourism-tax?month=2027-03"
curl "http://localhost:7070/reports/rate-plans?month=2027-03"
```
