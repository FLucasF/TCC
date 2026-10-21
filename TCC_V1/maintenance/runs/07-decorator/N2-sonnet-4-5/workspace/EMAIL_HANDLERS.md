# Regras de processamento de e-mail

O sistema processa cada mensagem através de uma cadeia de handlers antes de enviá-la. Cada handler pode modificar a mensagem, bloqueá-la ou adicionar cópias.

## Handlers atuais

1. **EmailValidator**: valida o endereço. Se inválido (sem @, com múltiplos @, ou sem ponto no domínio), marca como BLOCKED e para a cadeia.
2. **TestModeHandler**: quando `HOTEL_TEST_MODE=true`, redireciona para `testes@pousadamarealta.com.br`, adiciona `[TESTE]` no assunto e registra o destinatário original no corpo.
3. **ArchiveCopyHandler**: gera uma cópia para `arquivo@pousadamarealta.com.br` com assunto iniciando com `[Cópia]`. Desabilitado em modo de teste.

## Como adicionar uma nova regra

### Exemplo: Não enviar de madrugada

```java
final class NightTimeBlocker implements EmailHandler {
    
    private final boolean enabled;
    
    NightTimeBlocker(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean handle(EmailContext context) {
        if (!enabled) {
            return true;
        }
        
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(0, 0)) && now.isBefore(LocalTime.of(6, 0))) {
            context.status = MessageStatus.BLOCKED;
            return false; // para a cadeia
        }
        return true; // continua
    }
}
```

### Exemplo: Limite de mensagens por destinatário

```java
final class RateLimiter implements EmailHandler {
    
    private final Map<String, Integer> messageCount = new ConcurrentHashMap<>();
    private final int maxPerDay;
    
    RateLimiter(int maxPerDay) {
        this.maxPerDay = maxPerDay;
    }
    
    @Override
    public boolean handle(EmailContext context) {
        int count = messageCount.merge(context.to, 1, Integer::sum);
        if (count > maxPerDay) {
            context.status = MessageStatus.BLOCKED;
            return false;
        }
        return true;
    }
}
```

### Registrando o handler

Adicione o novo handler na cadeia em `EmailSender.buildHandlerChain()`:

```java
private static List<EmailHandler> buildHandlerChain(boolean testMode, boolean archiveCopyEnabled) {
    List<EmailHandler> chain = new ArrayList<>();
    chain.add(new EmailValidator());
    chain.add(new NightTimeBlocker(true)); // sempre ativo
    chain.add(new RateLimiter(10)); // máximo 10 por dia
    chain.add(new TestModeHandler(testMode));
    if (!testMode) {
        chain.add(new ArchiveCopyHandler(archiveCopyEnabled));
    }
    return chain;
}
```

## Ordem de execução

A ordem importa. Handlers são executados na ordem em que aparecem na lista:

1. Se um handler retorna `false`, a cadeia para
2. Se retorna `true`, o próximo handler é executado
3. Handlers podem modificar `context.to`, `context.subject`, `context.body`
4. Handlers podem marcar `context.status = MessageStatus.BLOCKED`
5. Handlers podem adicionar mensagens extras via `context.addMessage(...)`

## Configuração por instalação

Para configurações diferentes por instalação, passe parâmetros ao construtor do `EmailSender`:

```java
// Instalação A: cópia de arquivo habilitada
new EmailSender(sentMessages, testMode, true)

// Instalação B: sem cópia de arquivo
new EmailSender(sentMessages, testMode, false)

// Com novos parâmetros:
new EmailSender(sentMessages, testMode, archiveEnabled, blockNightTime, maxMessagesPerDay)
```
