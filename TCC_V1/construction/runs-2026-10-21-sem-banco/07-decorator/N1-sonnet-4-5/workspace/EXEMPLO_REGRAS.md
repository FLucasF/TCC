# Demonstração: Sistema de Regras de Mensagens

## O Problema Original

O cliente pediu três regras que precisam estar "desde o primeiro dia", e já avisou que **virão mais regras**:

1. ✅ Validação de e-mail
2. ✅ Cópia para arquivo
3. ✅ Modo de teste

**E no futuro:**
- Não enviar de madrugada
- Limite de mensagens por dia
- Cada regra pode valer sempre, só no teste, ou só fora dele
- Cada instalação pode ter combinação diferente

## A Solução: Chain of Responsibility

### Como funciona

Cada regra implementa uma interface simples:

```java
public interface MessageRule {
    void apply(MessageRuleContext context);
}
```

O `MessageProcessor` aplica todas as regras em ordem:

```java
public class MessageProcessor {
    private final List<MessageRule> rules;
    
    public MessageRuleContext process(MessageRuleContext context) {
        for (MessageRule rule : rules) {
            rule.apply(context);
        }
        return context;
    }
}
```

### Exemplo 1: Modo Produção

```java
List<MessageRule> rules = new ArrayList<>();
rules.add(new EmailValidationRule());      // Valida formato
rules.add(new ArchiveCopyRule());          // Cria cópia para arquivo

MessageProcessor processor = new MessageProcessor(rules);
```

**Entrada:**
```
Para: hospede@example.com
Assunto: Reserva confirmada
Corpo: Olá, sua reserva está confirmada...
```

**Saída:**
```
Mensagem 1:
  Para: hospede@example.com
  Assunto: Reserva confirmada
  Status: SENT

Mensagem 2 (cópia):
  Para: arquivo@pousadamarealta.com.br
  Assunto: [Cópia] Reserva confirmada
  Status: SENT
```

### Exemplo 2: E-mail Inválido

**Entrada:**
```
Para: hospede.gmail  (sem @ e sem domínio completo)
```

**Saída:**
```
Mensagem:
  Para: hospede.gmail
  Status: BLOCKED
  
(Nenhuma cópia criada - bloqueada antes)
```

### Exemplo 3: Modo de Teste

```java
List<MessageRule> rules = new ArrayList<>();
rules.add(new EmailValidationRule());
rules.add(new TestModeRedirectRule());     // Redireciona em vez de copiar

MessageProcessor processor = new MessageProcessor(rules);
```

**Entrada:**
```
Para: hospede@example.com
Assunto: Reserva confirmada
Corpo: Olá, sua reserva está confirmada...
```

**Saída:**
```
Mensagem:
  Para: testes@pousadamarealta.com.br
  Assunto: [TESTE] Reserva confirmada
  Corpo: Destinatário original: hospede@example.com
         Olá, sua reserva está confirmada...
  Status: SENT

(Sem cópia para arquivo em modo de teste)
```

## Adicionando uma Nova Regra

### Requisito: "Não enviar mensagens de madrugada (22h-6h)"

**1. Criar a regra:**

```java
package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.MessageRuleContext;
import java.time.LocalTime;

public class NoNightTimeRule implements MessageRule {
    
    @Override
    public void apply(MessageRuleContext context) {
        LocalTime now = LocalTime.now();
        
        if (now.isAfter(LocalTime.of(22, 0)) || 
            now.isBefore(LocalTime.of(6, 0))) {
            context.setStatus(MessageStatus.BLOCKED);
        }
    }
}
```

**2. Adicionar no MessageProcessor (App.java):**

```java
private static MessageProcessor createMessageProcessor(boolean testMode) {
    List<MessageRule> rules = new ArrayList<>();
    rules.add(new EmailValidationRule());
    rules.add(new NoNightTimeRule());        // ← Nova regra entra aqui
    
    if (testMode) {
        rules.add(new TestModeRedirectRule());
    } else {
        rules.add(new ArchiveCopyRule());
    }
    
    return new MessageProcessor(rules);
}
```

**Pronto!** Nenhum código existente foi modificado, apenas:
- ✅ Uma nova classe criada
- ✅ Uma linha adicionada na lista de regras

## Regras Condicionais

### Requisito: "Limite de 5 mensagens por hóspede por dia, só em produção"

```java
public class DailyLimitRule implements MessageRule {
    private final Map<String, List<LocalDate>> sentDates = new ConcurrentHashMap<>();
    private final int dailyLimit;
    
    public DailyLimitRule(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }
    
    @Override
    public void apply(MessageRuleContext context) {
        if (context.getStatus() == MessageStatus.BLOCKED) {
            return; // Já bloqueada, não processa
        }
        
        String email = context.getOriginalTo();
        LocalDate today = LocalDate.now();
        
        List<LocalDate> dates = sentDates.computeIfAbsent(email, k -> new ArrayList<>());
        long todayCount = dates.stream()
                .filter(date -> date.equals(today))
                .count();
        
        if (todayCount >= dailyLimit) {
            context.setStatus(MessageStatus.BLOCKED);
        } else {
            dates.add(today);
        }
    }
}
```

**Adicionar só em produção:**

```java
if (testMode) {
    rules.add(new TestModeRedirectRule());
} else {
    rules.add(new DailyLimitRule(5));     // Só em produção
    rules.add(new ArchiveCopyRule());
}
```

## Benefícios da Arquitetura

### ✅ Open/Closed Principle
- **Aberto para extensão**: Novas regras sem modificar existentes
- **Fechado para modificação**: MessageProcessor não muda

### ✅ Single Responsibility
- Cada regra faz uma coisa só
- MessageProcessor só coordena

### ✅ Composição Flexível
- Ordem importa: validação antes de tudo
- Combinações diferentes por ambiente
- Fácil testar cada regra isoladamente

### ✅ Preparado para o Futuro
- Cliente já avisou: "virão mais regras"
- Sistema aceita quantas regras forem necessárias
- Cada instalação pode ter conjunto diferente

## Comparação: Antes e Depois

### ❌ Sem o padrão

```java
public void sendMessage(String to, String subject, String body) {
    // Validação
    if (!isValidEmail(to)) {
        saveBlocked(to, subject, body);
        return;
    }
    
    // Modo de teste
    if (testMode) {
        to = "testes@...";
        subject = "[TESTE] " + subject;
        body = "Original: " + to + "\n" + body;
    }
    
    // Enviar
    save(to, subject, body);
    
    // Cópia (só em produção)
    if (!testMode) {
        save("arquivo@...", "[Cópia] " + subject, body);
    }
    
    // Nova regra = mexer aqui de novo! 😱
}
```

### ✅ Com o padrão

```java
public void sendMessage(String to, String subject, String body) {
    MessageRuleContext context = new MessageRuleContext(channel, to, subject, body);
    messageProcessor.process(context);
    
    save(context); // Regras já aplicadas
    
    // Nova regra = só adicionar na lista! 🎉
}
```

## Testabilidade

Cada regra é testada isoladamente:

```java
@Test
void shouldBlockEmailAfter10PM() {
    NoNightTimeRule rule = new NoNightTimeRule();
    MessageRuleContext context = new MessageRuleContext(...);
    
    // Simula horário de madrugada
    rule.apply(context);
    
    assertThat(context.getStatus()).isEqualTo(MessageStatus.BLOCKED);
}
```

E o processador testa a composição:

```java
@Test
void shouldApplyAllRulesInOrder() {
    MessageProcessor processor = new MessageProcessor(List.of(
        new EmailValidationRule(),
        new ArchiveCopyRule()
    ));
    
    // Testa que as duas regras foram aplicadas
}
```

---

**Conclusão:** O sistema está preparado para crescer sem quebrar. Cada nova regra é um arquivo novo, não uma modificação do código existente. Exatamente como o cliente pediu: "organizar para que o próximo caso entre num lugar só, sem mexer no que já funciona".
