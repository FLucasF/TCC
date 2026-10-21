# Cálculo de Preços e Tarifas

## Quartos e Diárias

| Quarto | Tipo | Capacidade | Diária Normal | Diária Alta Temporada |
|--------|------|-----------|----------------|----------------------|
| 101, 102 | STANDARD | 2 pessoas | R$ 249,90 | R$ 299,88 |
| 201, 202 | DELUXE | 3 pessoas | R$ 389,90 | R$ 467,88 |
| 301 | SUITE | 4 pessoas | R$ 579,90 | R$ 695,88 |

## Alta Temporada

**Períodos com 20% de acréscimo:**
- 15 de dezembro até 28 de fevereiro (verão)
- 1º ao 31 de julho (inverno)

## Planos de Tarifa

### STANDARD (Normal)
- Sem desconto
- Cancelamento: reembolso completo do valor pago

### NON_REFUNDABLE (Não Reembolsável)
- Desconto: 15% sobre o subtotal
- Cancelamento: sem reembolso

## Exemplo de Cálculo

### Exemplo 1: Reserva STANDARD em Fevereiro (High Season)
```
Quarto: 201 (DELUXE)
Datas: 10/02/2027 a 12/02/2027
Tipo: STANDARD
Hóspedes: 2

Cálculo:
- Noites: 2 (10-11 e 11-12)
- Diária: 389,90 * 1.20 = R$ 467,88 (arredondado)
- Subtotal: 467,88 * 2 = R$ 935,76
- Desconto: 0%
- Total: R$ 935,76
```

### Exemplo 2: Reserva NON_REFUNDABLE em Julho (High Season)
```
Quarto: 201 (DELUXE)
Datas: 10/07/2027 a 13/07/2027
Tipo: NON_REFUNDABLE
Hóspedes: 2

Cálculo:
- Noites: 3 (10-11, 11-12, 12-13)
- Diária: 389,90 * 1.20 = R$ 467,88
- Subtotal: 467,88 * 3 = R$ 1.403,64
- Desconto: 1.403,64 * 0.15 = R$ 210,55 (arredondado)
- Total: 1.403,64 - 210,55 = R$ 1.193,09
```

### Exemplo 3: Reserva Atravessando Período de Alta Temporada
```
Quarto: 101 (STANDARD)
Datas: 14/12/2026 a 16/12/2026
Tipo: STANDARD
Hóspedes: 1

Cálculo:
- Noite 1 (14-15/12): 249,90 * 1.00 = R$ 249,90 (antes de 15/12, low season)
- Noite 2 (15-16/12): 249,90 * 1.20 = R$ 299,88 (a partir de 15/12, high season)
- Subtotal: 249,90 + 299,88 = R$ 549,78
- Desconto: 0%
- Total: R$ 549,78
```

## Arredondamento

O sistema utiliza o método **"meio para o par"** (banker's rounding) em todas as operações:

### Exemplos de Arredondamento
```
249,90 * 1.20 = 299,88   (1 centavo arredondado)
1.403,64 * 0.15 = 210,546 → 210,55  (3 centavos, arredonda para par)
```

Este método é aplicado em:
1. Cálculo de cada diária em alta temporada
2. Soma das diárias (subtotal)
3. Cálculo do desconto
4. Cálculo do total final

## Datas no Sistema

### Formato de Entrada (API)
- ISO 8601: `2027-07-10` (YYYY-MM-DD)

### Formato de Saída
- **JSON**: `2027-07-10` (YYYY-MM-DD)
- **Relatórios CSV**: `2027-07-10` (YYYY-MM-DD)
- **Comprovante/Email**: `10/07/2027` (DD/MM/YYYY, formatado com vírgula nos valores monetários)

## Noites de Estadia

Uma reserva de 10/07 a 13/07 = **3 noites**:
- Noite 1: 10/07 para 11/07
- Noite 2: 11/07 para 12/07
- Noite 3: 12/07 para 13/07

A data de saída NÃO é contada como noite de estadia.

## Cancelamento e Reembolsos

### Reserva STANDARD
```
Status: PENDING → Sem reembolso (não há pagamento)
Status: CONFIRMED → Reembolso completo do valor pago
Status: CHECKED_IN → Reembolso completo do valor pago
```

### Reserva NON_REFUNDABLE
```
Status: PENDING → Sem reembolso (não há pagamento)
Status: CONFIRMED → Sem reembolso (refund = R$ 0,00)
Status: CHECKED_IN → Sem reembolso (refund = R$ 0,00)
```

## Relatório Financeiro

O "dinheiro que fica com a pousada" = `amountPaid - refund`

### Exemplos
```
Reserva STANDARD confirmada e cancelada:
  amountPaid: 935,76
  refund: 935,76
  amount: 0,00 (sem lucro)

Reserva NON_REFUNDABLE confirmada e cancelada:
  amountPaid: 1.193,09
  refund: 0,00
  amount: 1.193,09 (lucro total mantido)

Reserva STANDARD apenas criada (pendente):
  amountPaid: 0,00
  refund: null
  amount: 0,00 (não aparece no relatório)
```

## API de Preços

A API retorna sempre com 2 casas decimais:

```json
{
  "subtotal": 779.80,
  "discount": 0.00,
  "total": 779.80,
  "amountPaid": 0.00,
  "refund": null
}
```

## Validação

O sistema valida:
- ✅ Data de saída > data de entrada
- ✅ Mínimo 1 hóspede, máximo = capacidade do quarto
- ✅ Datas em formato válido (YYYY-MM-DD)
- ✅ Quarto existente
- ✅ Quarto disponível para as datas (sem sobreposição)
- ✅ Plano de tarifa válido (STANDARD ou NON_REFUNDABLE)
