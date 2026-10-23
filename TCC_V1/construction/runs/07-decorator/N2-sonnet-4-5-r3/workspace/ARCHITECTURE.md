# Arquitetura do Sistema de Mensagens

## Decisão de Design: Chain of Responsibility

O sistema de envio de mensagens foi implementado usando o padrão **Chain of Responsibility**, conforme orientação do `CLAUDE.md`:

> "Antes de escrever cada parte, pergunte: para acrescentar o próximo caso parecido com este, quantos lugares do código teriam de mudar?"

### Problema Original

O sistema precisa processar mensagens com três regras:
1. **Validação de e-mail**: bloqueia mensagens com endereços inválidos
2. **Modo de teste**: redireciona para `testes@pousadamarealta.com.br`
3. **Cópia para arquivo**: duplica para `arquivo@pousadamarealta.com.br`

Além disso, foram mencionadas regras futuras:
- Limite de mensagens por hóspede por dia
- Restrição de horário (não enviar de madrugada)
- Cada regra pode valer sempre, só em teste ou só em produção

### Alternativas Consideradas

#### Opção 1: Método com if/else
```java
void sendMessage(Message msg, boolean testMode) {
    if (!isValidEmail(msg.to)) {
        block(msg);
        return;
    }
    
    if (testMode) {
        msg = redirect(msg, "testes@...");
    }
    
    send(msg);
    
    if (!testMode) {
        send(copy(msg, "arquivo@..."));
    }
}
```

**Problema**: Adicionar a 4ª regra (limite diário) exige modificar este método. A 5ª regra (horário) modifica de novo. Com 10 regras, o método fica ilegível e qualquer mudança afeta código que já funciona.

#### Opção 2: Decorator
```java
DataSink sink = new TestModeDecorator(
    new ArchiveCopyDecorator(
        new EmailValidator(
            new MessageSender()
        )
    )
);
```

**Problema**: Decorators executam **todos** em sequência. Mas a validação de e-mail pode **bloquear** a mensagem, impedindo que as outras regras processem. No Decorator, mesmo bloqueada, a mensagem passaria pelas camadas seguintes.

#### Opção 3: Chain of Responsibility ✓

Todos os handlers executam, mas cada um pode:
- Modificar a mensagem
- Alterar o status (SENT → BLOCKED)
- Handlers posteriores reagem ao status

```java
MessageDispatcher dispatcher = new MessageDispatcher(List.of(
    new EmailValidationRule(),    // pode bloquear
    new TestModeRule(),            // só age se não bloqueada
    new ArchiveCopyRule()          // só age se não bloqueada
));
```

### Implementação

#### 1. Interface da Regra

```java
public interface MessageRule {
    void process(MessageContext context);
}
```

Cada regra recebe o contexto completo e pode modificá-lo.

#### 2. Contexto da Mensagem

```java
public final class MessageContext {
    private OutgoingMessage message;
    private MessageStatus status;
    private final List<OutgoingMessage> additionalMessages;
    
    // getters, setters
}
```

O contexto carrega:
- A mensagem (pode ser modificada)
- O status (SENT ou BLOCKED)
- Mensagens adicionais (como a cópia para arquivo)

#### 3. Dispatcher

```java
public final class MessageDispatcher {
    private final List<MessageRule> rules;
    
    public MessageContext dispatch(OutgoingMessage message) {
        MessageContext context = new MessageContext(message);
        for (MessageRule rule : rules) {
            rule.process(context);
        }
        return context;
    }
}
```

O dispatcher executa todas as regras em ordem. Não há short-circuit: mesmo bloqueada, a mensagem passa por todas as regras (que podem escolher não agir).

#### 4. Regras Concretas

**EmailValidationRule**: Valida o endereço
```java
public void process(MessageContext context) {
    if (!isValidEmail(context.getMessage().to())) {
        context.setStatus(MessageStatus.BLOCKED);
    }
}
```

**TestModeRule**: Redireciona em modo teste
```java
public void process(MessageContext context) {
    if (context.getStatus() == MessageStatus.BLOCKED) {
        return;  // não modifica mensagem bloqueada
    }
    
    OutgoingMessage modified = context.getMessage()
        .withTo("testes@pousadamarealta.com.br")
        .prependToSubject("[TESTE] ")
        .prependToBody("Destinatário original: " + original.to() + "\n");
    
    context.setMessage(modified);
}
```

**ArchiveCopyRule**: Cria cópia
```java
public void process(MessageContext context) {
    if (context.getStatus() == MessageStatus.BLOCKED) {
        return;  // não cria cópia de mensagem bloqueada
    }
    
    OutgoingMessage copy = context.getMessage()
        .withTo("arquivo@pousadamarealta.com.br")
        .prependToSubject("[Cópia] ");
    
    context.addAdditionalMessage(copy);
}
```

### Montagem por Configuração

```java
public MessageService(boolean testMode, boolean archiveCopy) {
    List<MessageRule> rules = new ArrayList<>();
    rules.add(new EmailValidationRule());  // sempre ativo
    
    if (testMode) {
        rules.add(new TestModeRule());
    }
    
    if (archiveCopy && !testMode) {
        rules.add(new ArchiveCopyRule());
    }
    
    this.dispatcher = new MessageDispatcher(rules);
}
```

A combinação de regras é decidida **uma vez**, onde o objeto é criado. As regras não conhecem configuração nem umas às outras.

### Adicionando Novas Regras

Para adicionar "limite de 5 mensagens por hóspede por dia":

1. Criar a classe:
```java
public final class DailyLimitRule implements MessageRule {
    private final MessageRepository repo;
    
    public void process(MessageContext context) {
        if (context.getStatus() == MessageStatus.BLOCKED) return;
        
        String email = context.getMessage().to();
        int count = repo.countTodayByRecipient(email);
        
        if (count >= 5) {
            context.setStatus(MessageStatus.BLOCKED);
        }
    }
}
```

2. Registrar na montagem:
```java
rules.add(new DailyLimitRule(messageRepository));
```

**Nenhuma regra existente foi modificada.** O teste unitário de cada regra continua verde.

### Por que Chain of Responsibility?

1. **Extensibilidade**: adicionar a 10ª regra não mexe nas outras 9
2. **Composição**: cada instalação monta sua combinação (teste vs. produção, com ou sem arquivo)
3. **Testabilidade**: cada regra é testada isoladamente
4. **Reação ao estado**: regras posteriores podem reagir ao que as anteriores fizeram (bloqueio, modificação)

### Comparação com alternativas

| Aspecto | If/Else | Decorator | Chain ✓ |
|---------|---------|-----------|---------|
| Adicionar regra | Modifica método | Adiciona classe | Adiciona classe |
| Bloqueio | Early return | Não suporta bem | Muda status |
| Combinações | Hard-coded | Composição | Lista configurável |
| Testabilidade | Difícil (muitos casos) | Boa | Boa |
| Código que muda | O método | Assembly | Assembly |

### Referência

Este design segue o padrão documentado em `.claude/skills/design-patterns/references/chain-of-responsibility.md`:

> "Pass a request along a sequence of handlers; each one either handles it, or passes it on, or stops it."

No nosso caso, todas as regras executam (não há short-circuit), mas cada uma pode modificar o contexto ou marcar como bloqueada, influenciando as próximas.
