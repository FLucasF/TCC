# Sistema de Regras de Mensagens

## Requisitos Implementados

### 1. Validação de E-mail

**Regra**: Mensagem para endereço inválido não deve ser enviada; aparece como bloqueada.

**Validação**: Um endereço é válido quando:
- Tem exatamente um `@`
- Tem alguma coisa antes do `@`
- Depois do `@`, tem um nome com pelo menos um ponto no meio (ex: `gmail.com`)

**Exemplos**:
- ✅ `ana@gmail.com` - válido
- ❌ `ana@gmail` - inválido (sem ponto após @)
- ❌ `ana.gmail.com` - inválido (sem @)
- ❌ `@gmail.com` - inválido (nada antes do @)

**Implementação**: `EmailValidationRule`

### 2. Cópia para Arquivo

**Regra**: Toda mensagem enviada gera cópia para `arquivo@pousadamarealta.com.br`.

**Comportamento**:
- Assunto: `[Cópia] ` + assunto original
- Corpo: idêntico ao original
- Mensagem bloqueada não gera cópia

**Implementação**: `ArchiveCopyRule`

### 3. Modo de Teste

**Regra**: Em modo de teste, mensagens vão para `testes@pousadamarealta.com.br`.

**Comportamento**:
- Destinatário: sempre `testes@pousadamarealta.com.br`
- Assunto: `[TESTE] ` + assunto original
- Corpo: linha adicional no início: `Destinatário original: <endereço>`
- Em modo de teste, não gera cópia para arquivo
- Validação de e-mail continua funcionando (bloqueio se inválido)

**Configuração**: Variável `HOTEL_TEST_MODE=true`

**Implementação**: `TestModeRule`

## Pipeline de Regras

### Modo Produção (HOTEL_TEST_MODE não definido ou false)

```
Mensagem
  ↓
EmailValidationRule → valida → bloqueia se inválido
  ↓
ArchiveCopyRule → cria cópia para arquivo@...
  ↓
Resultado: [mensagem original, cópia arquivo] ou [mensagem bloqueada]
```

### Modo Teste (HOTEL_TEST_MODE=true)

```
Mensagem
  ↓
EmailValidationRule → valida → bloqueia se inválido
  ↓
TestModeRule → redireciona para testes@...
  ↓
Resultado: [mensagem redirecionada] ou [mensagem bloqueada]
```

## Exemplos Práticos

### Exemplo 1: Produção, e-mail válido

**Entrada**:
```json
{
  "to": "joao@example.com",
  "subject": "Reserva confirmada",
  "body": "Sua reserva está confirmada..."
}
```

**Saída** (2 mensagens):

Mensagem 1:
```json
{
  "to": "joao@example.com",
  "subject": "Reserva confirmada",
  "body": "Sua reserva está confirmada...",
  "status": "SENT"
}
```

Mensagem 2 (cópia):
```json
{
  "to": "arquivo@pousadamarealta.com.br",
  "subject": "[Cópia] Reserva confirmada",
  "body": "Sua reserva está confirmada...",
  "status": "SENT"
}
```

### Exemplo 2: Produção, e-mail inválido

**Entrada**:
```json
{
  "to": "joao.example",
  "subject": "Reserva confirmada",
  "body": "Sua reserva está confirmada..."
}
```

**Saída** (1 mensagem bloqueada):
```json
{
  "to": "joao.example",
  "subject": "Reserva confirmada",
  "body": "Sua reserva está confirmada...",
  "status": "BLOCKED"
}
```

### Exemplo 3: Teste, e-mail válido

**Entrada**:
```json
{
  "to": "joao@example.com",
  "subject": "Reserva confirmada",
  "body": "Sua reserva está confirmada..."
}
```

**Saída** (1 mensagem redirecionada):
```json
{
  "to": "testes@pousadamarealta.com.br",
  "subject": "[TESTE] Reserva confirmada",
  "body": "Destinatário original: joao@example.com\nSua reserva está confirmada...",
  "status": "SENT"
}
```

### Exemplo 4: Teste, e-mail inválido

**Entrada**:
```json
{
  "to": "joao",
  "subject": "Reserva confirmada",
  "body": "Sua reserva está confirmada..."
}
```

**Saída** (1 mensagem bloqueada, mantém destinatário original):
```json
{
  "to": "joao",
  "subject": "Reserva confirmada",
  "body": "Sua reserva está confirmada...",
  "status": "BLOCKED"
}
```

## Extensibilidade Futura

Conforme mencionado no enunciado:

> "Já pediram para não mandar mensagem de madrugada e para ter um limite de mensagens por hóspede por dia, e cada regra dessas pode valer sempre, só no modo de teste ou só fora dele."

### Como adicionar: Limite de mensagens por dia

```java
public class DailyLimitRule implements MessageRule {
    private final MessageRepository repository;
    private final int maxPerDay;

    @Override
    public void apply(MessageRuleContext context) {
        if (context.isBlocked()) return;
        
        String email = context.getOriginalMessage().getTo();
        LocalDate today = LocalDate.now();
        
        long count = repository.countByRecipientAndDate(email, today);
        
        if (count >= maxPerDay) {
            context.block();
            context.getCurrentMessage().setStatus(MessageStatus.BLOCKED);
        }
    }
}
```

### Como adicionar: Não enviar de madrugada

```java
public class NoNightTimeRule implements MessageRule {
    @Override
    public void apply(MessageRuleContext context) {
        if (context.isBlocked()) return;
        
        LocalTime now = LocalTime.now();
        LocalTime nightStart = LocalTime.of(0, 0);
        LocalTime nightEnd = LocalTime.of(6, 0);
        
        if (now.isAfter(nightStart) && now.isBefore(nightEnd)) {
            context.block();
            context.getCurrentMessage().setStatus(MessageStatus.BLOCKED);
        }
    }
}
```

### Registrar as novas regras

```java
public class MessageRulePipelineFactory {
    
    public static MessageRulePipeline create(boolean testMode) {
        List<MessageRule> rules = new ArrayList<>();
        
        rules.add(new EmailValidationRule());
        rules.add(new DailyLimitRule(repository, 10)); // novo
        rules.add(new NoNightTimeRule());              // novo
        
        if (testMode) {
            rules.add(new TestModeRule());
        } else {
            rules.add(new ArchiveCopyRule());
        }
        
        return new MessageRulePipeline(rules);
    }
}
```

## Design: Por que Chain of Responsibility?

1. **Extensibilidade**: Adicionar regra = criar classe, sem mexer nas existentes
2. **Composição**: Combinar regras livremente
3. **Configurabilidade**: Diferentes pipelines para diferentes contextos
4. **Testabilidade**: Cada regra testável isoladamente
5. **Ordem importa**: EmailValidation sempre primeiro (bloqueia inválidos antes de processar)

Esta é a organização que atende ao CLAUDE.md:

> "Para acrescentar o próximo caso parecido com este, quantos lugares do código teriam de mudar? Se forem vários, organize para que o próximo caso entre num lugar só, sem mexer no que já funciona."

**Resposta**: Um lugar só - criar nova classe `MessageRule`.
