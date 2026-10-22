# Exemplos de Cálculo de Preços - Pousada Maré Alta

## Sazonalidade

A alta temporada inclui:
- **Julho**: Inteiro (1º ao 31º)
- **Dezembro**: A partir do dia 15
- **Janeiro**: Inteiro (1º ao 31º)
- **Fevereiro**: Inteiro (1º ao 28/29º)

Durante a alta temporada, a diária sobe **20%**.

### Exemplo 1: Reserva em Baixa Temporada

**Quarto**: 101 (STANDARD) - R$ 249,90 por noite
**Datas**: 10/03/2027 a 12/03/2027 (2 noites em março)
**Plano**: STANDARD (sem desconto)

```
10/03/2027 (baixa temporada): R$ 249,90
11/03/2027 (baixa temporada): R$ 249,90
─────────────────────────────
Subtotal:                      R$ 499,80
Desconto:                      R$ 0,00
─────────────────────────────
Total:                         R$ 499,80
```

### Exemplo 2: Reserva em Alta Temporada (Julho)

**Quarto**: 101 (STANDARD) - R$ 249,90 por noite
**Datas**: 10/07/2027 a 12/07/2027 (2 noites em julho)
**Plano**: STANDARD

```
10/07/2027 (alta temporada): R$ 249,90 × 1.20 = R$ 299,88
11/07/2027 (alta temporada): R$ 249,90 × 1.20 = R$ 299,88
─────────────────────────────────────────────────
Subtotal:                                       R$ 599,76
Desconto:                                       R$ 0,00
─────────────────────────────────────────────────
Total:                                          R$ 599,76
```

### Exemplo 3: Reserva com Plano NON_REFUNDABLE

**Quarto**: 201 (DELUXE) - R$ 389,90 por noite
**Datas**: 10/03/2027 a 12/03/2027 (2 noites em março)
**Plano**: NON_REFUNDABLE (15% de desconto)

```
10/03/2027 (baixa temporada): R$ 389,90
11/03/2027 (baixa temporada): R$ 389,90
─────────────────────────────
Subtotal:                      R$ 779,80
Desconto (15%):                R$ 116,97
─────────────────────────────
Total:                         R$ 662,83
```

### Exemplo 4: Reserva Cruzando Sazonalidade

**Quarto**: 301 (SUITE) - R$ 579,90 por noite
**Datas**: 14/12/2027 a 16/12/2027 (2 noites)
**Plano**: STANDARD

```
14/12/2027 (baixa temporada): R$ 579,90
15/12/2027 (alta temporada): R$ 579,90 × 1.20 = R$ 695,88
─────────────────────────────────────────────
Subtotal:                                     R$ 1.275,78
Desconto:                                     R$ 0,00
─────────────────────────────────────────────
Total:                                        R$ 1.275,78
```

## Arredondamento "Meio para o Par"

O sistema usa arredondamento HALF_EVEN (meio para o par). Exemplos:

- 1,245 arredonda para **1,24** (par)
- 1,255 arredonda para **1,26** (par)
- 1,235 arredonda para **1,24** (par)

Todos os valores são arredondados neste padrão:
1. Preço de cada noite
2. Soma das noites (subtotal)
3. Desconto
4. Total final

## Planos de Tarifa

### STANDARD
- Sem desconto na diária
- Cancelamento antes do check-in: **reembolso total**
- Cancelamento depois do check-in: não permitido

### NON_REFUNDABLE
- **15% de desconto** na soma das diárias
- Cancelamento antes do check-in: **sem reembolso**
- Cancelamento depois do check-in: não permitido
- Proporciona economia ao cliente em troca da inflexibilidade

## Exemplo 5: Comparação de Planos

**Quarto**: 201 (DELUXE) - R$ 389,90 por noite
**Datas**: 10/07/2027 a 13/07/2027 (3 noites em julho - alta temporada)

### STANDARD:
```
3 × (R$ 389,90 × 1.20) = 3 × R$ 467,88 = R$ 1.403,64
Sem desconto
Total: R$ 1.403,64
```

### NON_REFUNDABLE:
```
3 × (R$ 389,90 × 1.20) = 3 × R$ 467,88 = R$ 1.403,64
Desconto (15%): R$ 210,55
Total: R$ 1.193,09
```

**Economia com NON_REFUNDABLE**: R$ 210,55 (15%)

## Cancelamentos

### Cenário 1: STANDARD cancelado antes de pagar
- Status: PENDING → CANCELLED
- Refund: R$ 0,00 (nada foi pago)
- Receita da pousada: R$ 0,00

### Cenário 2: STANDARD cancelado após confirmação
- Status: CONFIRMED → CANCELLED
- Refund: Valor total pago
- Receita da pousada: R$ 0,00

### Cenário 3: NON_REFUNDABLE cancelado antes de pagar
- Status: PENDING → CANCELLED
- Refund: R$ 0,00 (nada foi pago)
- Receita da pousada: R$ 0,00

### Cenário 4: NON_REFUNDABLE cancelado após confirmação
- Status: CONFIRMED → CANCELLED
- Refund: R$ 0,00 (sem reembolso)
- Receita da pousada: Valor total pago

## Relatório de Receita

O relatório financeiro mostra:
**Receita = Valor Pago - Reembolso**

Exemplo para Julho de 2027:
- Reserva 1 (STANDARD, confirmada, não cancelada): R$ 599,76
- Reserva 2 (NON_REFUNDABLE, confirmada, depois cancelada): R$ 509,80
- Reserva 3 (STANDARD, confirmada, depois cancelada): R$ 0,00 (refundido)

**Total**: R$ 1.109,56
