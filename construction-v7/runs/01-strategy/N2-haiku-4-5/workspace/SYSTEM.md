# Sistema de Reservas - Pousada Maré Alta

## Construção e Execução

### Build
```bash
mvn clean verify
```

Gera o arquivo `/workspace/target/app.jar`

### Execução
```bash
java -jar target/app.jar
```

O sistema escuta na porta definida pela variável de ambiente `PORT`, ou na porta `7070` se não especificada.

## Estrutura

### Quartos
- **101, 102**: STANDARD (2 pessoas, R$ 249,90)
- **201, 202**: DELUXE (3 pessoas, R$ 389,90)
- **301**: SUITE (4 pessoas, R$ 579,90)

### Planos de Tarifa

1. **STANDARD**: Sem desconto, reembolso 100%
2. **NON_REFUNDABLE**: 15% desconto, reembolso 0%
3. **CORPORATE**: Desconto por empresa, reembolso 100%
   - TECNOSUL: 12%
   - AGROVALE: 18%
   - FARMAVIDA: 25%
4. **WEEKEND_PACKAGE**: Pacote sexta-domingo (2 noites)
   - Standard: R$ 520,00
   - Deluxe: R$ 820,00
   - Suite: R$ 1.200,00
   - Reembolso: 50%
5. **LONG_STAY**: 7+ noites, 10% desconto, reembolso 100%

### Alta Temporada
- Julho (mês inteiro)
- Dezembro 15 - Fevereiro 28
- Acréscimo: +20% sobre a diária

### Arredondamento
Todas as operações monetárias usam arredondamento "meio para o par" (HALF_EVEN), com 2 casas decimais.

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Obtém os dados da reserva
- `POST /reservations/{id}/confirmation` - Confirma (requer amountPaid)
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out (retorna recibo)
- `POST /reservations/{id}/cancellation` - Cancela
- `GET /reservations/{id}/document` - Segunda via do recibo

### Mensagens
- `GET /messages` - Lista de e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Ocupação de um dia (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Faturamento de um mês (CSV)

## Exemplos Testados

### CORPORATE - Tecnosul (201, 10/03-12/03)
- Diárias: R$ 779,80
- Desconto (12%): R$ 93,58
- Total: R$ 686,22 ✓

### WEEKEND_PACKAGE - Deluxe (201, Sexta 12/03 - Domingo 14/03)
- Preço: R$ 820,00
- Reembolso (50%): R$ 410,00 ✓

### LONG_STAY - Standard (101, 01/03-08/03)
- Diárias: R$ 1.749,30
- Desconto (10%): R$ 174,93
- Total: R$ 1.574,37 ✓

## Design Extensível

Os planos de tarifa implementam a interface `RatePlan`, permitindo adicionar novos planos sem modificar o código existente:

```java
public interface RatePlan {
    String getName();
    ValidationResult validate(...);
    PricingResult calculatePrice(...);
    RefundResult calculateRefund(...);
}
```

Novos planos são registrados em `RatePlanFactory` e estão imediatamente disponíveis na API.

## Dados

Todos os dados são armazenados em memória. No reinício do sistema, as reservas são perdidas (conforme solicitado).
