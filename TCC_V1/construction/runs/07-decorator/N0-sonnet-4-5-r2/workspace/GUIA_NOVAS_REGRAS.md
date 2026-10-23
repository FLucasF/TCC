# Guia: Como Adicionar Novas Regras de Mensagens

## Cenário Exemplo: Limite de Envios por Dia

Você quer impedir que o sistema envie mais de 3 e-mails para o mesmo destinatário por dia, para evitar spam.

### Passo 1: Criar a Regra

Crie um novo arquivo em `src/main/java/com/pousadamarealta/messaging/rules/DailyLimitRule.java`:

```java
package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.MessageStatus;
import com.pousadamarealta.domain.repository.MessageRepository;
import com.pousadamarealta.messaging.MessageProcessor;
import com.pousadamarealta.messaging.ProcessingContext;
import com.pousadamarealta.messaging.ProcessingResult;

import java.time.LocalDate;

public class DailyLimitRule implements MessageProcessor {
    
    private final MessageRepository messageRepository;
    private final int maxPerDay;
    
    public DailyLimitRule(MessageRepository messageRepository, int maxPerDay) {
        this.messageRepository = messageRepository;
        this.maxPerDay = maxPerDay;
    }
    
    @Override
    public ProcessingResult process(Message message, ProcessingContext context) {
        // Conta quantas mensagens SENT foram enviadas hoje para este destinatário
        long sentToday = messageRepository.countSentToRecipientOnDate(
            message.getTo(), 
            LocalDate.now()
        );
        
        if (sentToday >= maxPerDay) {
            message.setStatus(MessageStatus.BLOCKED);
            return ProcessingResult.block(message);
        }
        
        return ProcessingResult.continueProcessing(message);
    }
}
```

### Passo 2: Adicionar Método ao Repositório

Em `MessageRepository.java`, adicione:

```java
public interface MessageRepository {
    Message save(Message message);
    List<Message> findAll();
    long countSentToRecipientOnDate(String recipient, LocalDate date); // NOVO
}
```

### Passo 3: Implementar no PostgresMessageRepository

Em `PostgresMessageRepository.java`, adicione:

```java
@Override
public long countSentToRecipientOnDate(String recipient, LocalDate date) {
    String sql = """
        SELECT COUNT(*) 
        FROM messages 
        WHERE recipient = ? 
          AND status = 'SENT'
          AND DATE(created_at) = ?
        """;
    
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setString(1, recipient);
        stmt.setDate(2, Date.valueOf(date));
        
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    } catch (SQLException e) {
        throw new RuntimeException("Failed to count messages", e);
    }
}
```

**Nota**: Você também precisará adicionar uma coluna `created_at` na tabela messages. Altere `DatabaseInitializer.java`:

```java
stmt.execute("""
    CREATE TABLE IF NOT EXISTS messages (
        id SERIAL PRIMARY KEY,
        channel VARCHAR(20) NOT NULL,
        recipient VARCHAR(255) NOT NULL,
        subject TEXT NOT NULL,
        body TEXT NOT NULL,
        status VARCHAR(20) NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
    """);
```

### Passo 4: Registrar a Regra

Em `MessageService.java`, modifique o método `buildProcessors()`:

```java
private List<MessageProcessor> buildProcessors() {
    List<MessageProcessor> processors = new ArrayList<>();
    processors.add(new EmailValidationRule());
    processors.add(new DailyLimitRule(messageRepository, 3)); // NOVA REGRA
    processors.add(new TestModeRule());
    processors.add(new ArchiveCopyRule());
    return processors;
}
```

### Passo 5: Atualizar o Construtor do MessageService

```java
public MessageService(MessageRepository messageRepository, boolean isTestMode) {
    this.messageRepository = messageRepository;
    this.context = new ProcessingContext(isTestMode);
    this.processors = buildProcessors();
}
```

### Passo 6: Testar

```java
@Test
public void testDailyLimitRule() {
    // Criar 3 reservas para o mesmo e-mail
    for (int i = 0; i < 3; i++) {
        createAndConfirmReservation("guest@example.com");
    }
    
    // A 4ª deve ser bloqueada
    int id4 = createReservation("guest@example.com");
    confirmReservation(id4);
    
    List<Message> messages = messageService.getAllMessages();
    
    // Últimas 2 mensagens (original + cópia) devem estar bloqueadas
    Message lastMessage = messages.get(messages.size() - 1);
    assertEquals(MessageStatus.BLOCKED, lastMessage.getStatus());
}
```

## Outros Exemplos de Regras

### 1. Não Enviar de Madrugada

```java
public class NightTimeBlockRule implements MessageProcessor {
    
    private final LocalTime startBlock;
    private final LocalTime endBlock;
    
    public NightTimeBlockRule(LocalTime startBlock, LocalTime endBlock) {
        this.startBlock = startBlock;
        this.endBlock = endBlock;
    }
    
    @Override
    public ProcessingResult process(Message message, ProcessingContext context) {
        LocalTime now = LocalTime.now();
        
        if (now.isAfter(startBlock) && now.isBefore(endBlock)) {
            message.setStatus(MessageStatus.BLOCKED);
            return ProcessingResult.block(message);
        }
        
        return ProcessingResult.continueProcessing(message);
    }
}

// Uso:
processors.add(new NightTimeBlockRule(
    LocalTime.of(0, 0),   // Bloqueia a partir de meia-noite
    LocalTime.of(6, 0)    // Até 6h da manhã
));
```

### 2. CC para Gerente

```java
public class ManagerCCRule implements MessageProcessor {
    
    private final String managerEmail;
    
    public ManagerCCRule(String managerEmail) {
        this.managerEmail = managerEmail;
    }
    
    @Override
    public ProcessingResult process(Message message, ProcessingContext context) {
        Message ccToManager = new Message();
        ccToManager.setTo(managerEmail);
        ccToManager.setSubject("[CC] " + message.getSubject());
        ccToManager.setBody(message.getBody());
        
        return ProcessingResult.continueWithAdditional(message, ccToManager);
    }
}

// Uso:
processors.add(new ManagerCCRule("gerente@pousadamarealta.com.br"));
```

### 3. Blacklist de E-mails

```java
public class BlacklistRule implements MessageProcessor {
    
    private final Set<String> blacklist;
    
    public BlacklistRule(Set<String> blacklist) {
        this.blacklist = blacklist;
    }
    
    @Override
    public ProcessingResult process(Message message, ProcessingContext context) {
        if (blacklist.contains(message.getTo().toLowerCase())) {
            message.setStatus(MessageStatus.BLOCKED);
            return ProcessingResult.block(message);
        }
        
        return ProcessingResult.continueProcessing(message);
    }
}

// Uso:
Set<String> blacklist = Set.of(
    "spam@example.com",
    "fake@example.com"
);
processors.add(new BlacklistRule(blacklist));
```

### 4. Log de Auditoria

```java
public class AuditLogRule implements MessageProcessor {
    
    private final AuditLogger auditLogger;
    
    public AuditLogRule(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }
    
    @Override
    public ProcessingResult process(Message message, ProcessingContext context) {
        auditLogger.log(
            "EMAIL_SENT",
            Map.of(
                "recipient", message.getTo(),
                "subject", message.getSubject(),
                "timestamp", LocalDateTime.now()
            )
        );
        
        return ProcessingResult.continueProcessing(message);
    }
}
```

### 5. Substituição de Variáveis

```java
public class TemplateVariablesRule implements MessageProcessor {
    
    @Override
    public ProcessingResult process(Message message, ProcessingContext context) {
        String body = message.getBody();
        
        // Substituir variáveis no corpo
        body = body.replace("{{nome_pousada}}", "Pousada Maré Alta");
        body = body.replace("{{telefone}}", "(11) 98765-4321");
        body = body.replace("{{endereco}}", "Rua da Praia, 123");
        
        message.setBody(body);
        
        return ProcessingResult.continueProcessing(message);
    }
}
```

## Configuração por Ambiente

Para permitir configurações diferentes por instalação, você pode usar um arquivo de configuração:

### application.yml
```yaml
messaging:
  rules:
    emailValidation:
      enabled: true
    
    dailyLimit:
      enabled: true
      maxPerDay: 3
    
    nightTimeBlock:
      enabled: true
      startHour: 0
      endHour: 6
    
    testMode:
      enabled: ${HOTEL_TEST_MODE:false}
    
    archiveCopy:
      enabled: true
      skipInTestMode: true
```

### Carregar Configuração

```java
public class MessageServiceFactory {
    
    public static MessageService create(
        MessageRepository repository,
        Configuration config
    ) {
        List<MessageProcessor> processors = new ArrayList<>();
        
        if (config.isEnabled("emailValidation")) {
            processors.add(new EmailValidationRule());
        }
        
        if (config.isEnabled("dailyLimit")) {
            int max = config.getInt("dailyLimit.maxPerDay");
            processors.add(new DailyLimitRule(repository, max));
        }
        
        if (config.isEnabled("nightTimeBlock")) {
            int start = config.getInt("nightTimeBlock.startHour");
            int end = config.getInt("nightTimeBlock.endHour");
            processors.add(new NightTimeBlockRule(
                LocalTime.of(start, 0),
                LocalTime.of(end, 0)
            ));
        }
        
        // ... outras regras
        
        return new MessageService(repository, processors);
    }
}
```

## Ordem Importa!

Lembre-se de que a ordem dos processadores é importante:

1. **Validação** primeiro (bloqueia inválidos antes de processar)
2. **Transformações** (modificam a mensagem)
3. **Geradores de cópias** por último (trabalham com mensagem já processada)

Exemplo de ordem recomendada:
```java
processors.add(new EmailValidationRule());        // 1. Validação
processors.add(new BlacklistRule(blacklist));     // 2. Validação
processors.add(new DailyLimitRule(repo, 3));      // 3. Validação
processors.add(new NightTimeBlockRule(...));      // 4. Validação
processors.add(new TestModeRule());               // 5. Transformação
processors.add(new TemplateVariablesRule());      // 6. Transformação
processors.add(new ArchiveCopyRule());            // 7. Gerador de cópias
processors.add(new ManagerCCRule(email));         // 8. Gerador de cópias
processors.add(new AuditLogRule(logger));         // 9. Logging (último)
```

## Dicas

1. **Teste cada regra isoladamente** antes de adicionar à pipeline
2. **Use interfaces** para facilitar testes com mocks
3. **Documente** o comportamento esperado de cada regra
4. **Considere performance**: regras que fazem queries devem ser otimizadas
5. **Pense em observabilidade**: adicione logs para debug
6. **Mantenha regras simples**: uma responsabilidade por regra
7. **Use configuração** para valores que podem mudar

## Conclusão

O sistema foi projetado para ser facilmente extensível. Adicionar novas regras é questão de:
1. Implementar `MessageProcessor`
2. Registrar na lista de processadores
3. Testar

Não é necessário modificar o código existente, apenas adicionar novos componentes. Isso segue o **Open/Closed Principle**: aberto para extensão, fechado para modificação.
