# Decisões de Design - Alinhamento com CLAUDE.md

Este documento explica como as orientações do CLAUDE.md foram aplicadas na construção do sistema.

## 1. "Antes de escrever cada parte, pergunte: quantos lugares do código teriam de mudar?"

### Aplicado em: Sistema de Validação de Email

**Problema**: O sistema original mencionava três regras diferentes para emails (validade, cópia para arquivo, teste). Cada mensagem poderia futuramente ter outras regras.

**Solução**: Centralizamos toda a lógica de validação e transformação de mensagens em uma única classe `MessageService`, que:
- Valida o email
- Aplica redirecionamento em modo teste
- Cria cópia para arquivo
- Define o status (SENT ou BLOCKED)

**Impacto**: Adicionar uma nova regra de email (ex: limite de mensagens por hora) requer mudança **apenas** em `MessageService`, não em vários lugares.

---

### Aplicado em: Cálculo de Preços

**Problema**: O cálculo envolve múltiplos passos (base, alta temporada, desconto, arredondamento). Cada um precisa ser reaplicado de forma consistente.

**Solução**: Concentramos toda a lógica em `PriceCalculator` com métodos independentes:
- `isHighSeason()` - verifica temporada
- `calculateNightlyPrice()` - aplica multiplicador
- `calculateDiscount()` - desconto por plano
- `roundPrice()` - arredondamento consistente

**Impacto**: Se a temporada alta mudar (ex: agora inclui semana de Páscoa), muda-se **apenas** `isHighSeason()`.

---

## 2. "Quando vários casos repetem a mesma sequência de passos, escreva a sequência uma vez só"

### Aplicado em: Formatação de Valores

**Problema**: CPF, data e dinheiro aparecem em múltiplos contextos (comprovante, relatório, email).

**Solução**: Criamos classes específicas:
- `CpfFormatter.format()` → "111.222.333-44"
- `DateFormatter.format()` → "10/03/2027"
- `MoneyFormatter.format()` → "R$ 779,80"

**Antes (incorreto)**:
```java
// Em DocumentService
String cpf = cpf.substring(0,3) + "." + ...;

// Em ReportService
String cpf = cpf.substring(0,3) + "." + ...;  // Repetido!
```

**Depois (correto)**:
```java
// Em DocumentService
String cpf = CpfFormatter.format(reservation.guestCpf);

// Em ReportService  
String cpf = CpfFormatter.format(reservation.guestCpf);
```

---

### Aplicado em: Transições de Estado

**Problema**: Check-in só é válido se status == CONFIRMED, check-out só se == CHECKED_IN, etc.

**Solução**: Centralizamos as transições em `ReservationService`:
```java
public static Reservation checkIn(long reservationId) {
    Reservation reservation = ReservationRepository.findById(reservationId);
    if (reservation.status != ReservationStatus.CONFIRMED) {
        throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
    }
    ReservationRepository.updateStatus(reservationId, ReservationStatus.CHECKED_IN);
    return ReservationRepository.findById(reservationId);
}
```

**Impacto**: Se adicionarmos nova regra (ex: só pode check-in após as 14h), muda-se **apenas** esse método.

---

## 3. "Faça essa organização só onde o enunciado mostra que a variação existe e vai continuar"

### Aplicado em: Enumerações de Tipo

**Não criamos** abstrações genéricas:
- ❌ Não temos interface `PaymentPlan`, apenas `RatePlan` com valores específicos
- ❌ Não temos classe genérica `Status`, apenas `ReservationStatus` e `MessageStatus`
- ❌ Não temos `Formatter` abstrato, classes específicas para cada tipo

**Razão**: O enunciado mostra que há apenas 2 planos de tarifa, 4 states de reserva, etc. Adicionar abstração sem necessidade viola o princípio.

---

### Aplicado em: Quartos Hardcoded

**Decisão**: Os 5 quartos (101, 102, 201, 202, 301) estão inseridos no `Database.initialize()` como INSERT direto.

```java
INSERT INTO rooms (number, type, capacity, nightly_rate) 
VALUES (101, 'STANDARD', 2, 249.90) ON CONFLICT DO NOTHING
```

**Razão**: O enunciado diz "Os quartos quase nunca mudam". Não criamos interface para "tipos de quarto" genéricos.

**Se futuramente**: Mover para arquivo de configuração ou UI, muda-se apenas `Database.initialize()`.

---

## 4. Organização das Camadas

### model/ - Apenas dados
Classes imutáveis que carregam dados:
- `Room`, `Reservation`, `Message`, `Document`
- `RoomType`, `RatePlan`, `ReservationStatus`, `MessageStatus`

Nenhuma lógica de negócio aqui.

---

### db/ - Apenas persistência
Cada repositório oferece operações CRUD:
- `RoomRepository.findAll()`, `findByNumber()`
- `ReservationRepository.create()`, `findById()`, `updateStatus()`
- `MessageRepository.save()`, `findAll()`

Nenhuma regra de negócio (ex: validação de transição de estado).

---

### service/ - Apenas lógica de negócio
Classes que orquestram as operações:
- `ReservationService` - controla transições, valida regras
- `MessageService` - aplica regras de email
- `DocumentService` - formata comprovante
- `ReportService` - gera relatórios

Não manipulam banco de dados diretamente, usam repositórios.

---

### util/ - Apenas utilitários
Funções puras sem estado:
- `PriceCalculator` - cálculos de preço
- `EmailValidator` - valida formato
- `DateFormatter`, `MoneyFormatter`, `CpfFormatter` - formatação

---

### api/ - Apenas estrutura de resposta
`ErrorResponse` - formato padrão de erro da API.

---

## 5. Exemplo: Adicionar Nova Regra de Email

### Cenário
"Não enviar emails entre 22h e 6h"

### Implementação
Modificar **apenas** `MessageService.sendMessage()`:

```java
public static Message sendMessage(String to, String subject, String body) throws Exception {
    // Validação existente
    if (!EmailValidator.isValid(to)) {
        // ...
    }
    
    // NOVA REGRA
    LocalTime now = LocalTime.now();
    if (now.isAfter(LocalTime.of(22, 0)) || now.isBefore(LocalTime.of(6, 0))) {
        // Enfileirar para depois
        return MessageRepository.save("EMAIL", to, subject, body, MessageStatus.BLOCKED);
    }
    
    // Resto continua igual
}
```

**Nenhuma outra classe precisa mudar.**

---

## 6. Exemplo: Adicionar Nova Rota na API

### Cenário
"GET /reservations/{id}/summary - resumo simples da reserva"

### Implementação
Adicionar **apenas** em `Application.java`:

```java
app.get("/reservations/:id/summary", ctx -> {
    long id = Long.parseLong(ctx.pathParam("id"));
    Reservation reservation = ReservationRepository.findById(id);
    if (reservation == null) {
        ctx.status(404);
        ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", "..."));
        return;
    }
    Map<String, Object> summary = Map.of(
        "id", reservation.id,
        "status", reservation.status.name(),
        "total", reservation.total.doubleValue()
    );
    ctx.json(summary);
});
```

**Nenhuma lógica de negócio, repositório ou modelo precisa mudar.**

---

## Conclusão

O design segue o princípio central do CLAUDE.md: **organizar o código de forma que variações futuras entrem em um único lugar, sem mexer no que já funciona**.

Isso foi conseguido através de:
1. Separação clara de responsabilidades (camadas)
2. Centralização de lógica correlata (MessageService, PriceCalculator)
3. Evitar abstrações prematuras
4. Usar enumerações para variações discretas conhecidas
