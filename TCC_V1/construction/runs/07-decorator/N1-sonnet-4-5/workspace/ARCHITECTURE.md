# Arquitetura do Sistema de Reservas

## Visão Geral

O sistema foi desenvolvido seguindo os princípios descritos em `CLAUDE.md`:

> "para acrescentar o próximo caso parecido com este, quantos lugares do código teriam de mudar?"

A resposta para o sistema de mensagens é: **apenas um lugar** - basta criar a nova regra e registrá-la.

## Sistema de Mensagens (Chain of Responsibility)

### Design

O sistema de mensagens usa o padrão Chain of Responsibility para permitir que novas regras sejam adicionadas sem modificar o código existente:

```
PendingMessage → [MessageRule 1] → [MessageRule 2] → [MessageRule 3] → ProcessedMessage(s)
```

### Componentes

1. **PendingMessage**: Mensagem antes do processamento (imutável)
2. **ProcessedMessage**: Mensagem após processamento (mutável pelas regras)
3. **MessageRule**: Interface que cada regra implementa
4. **MessageProcessor**: Orquestra a aplicação das regras

### Regras Implementadas

#### 1. EmailValidationRule
- **Responsabilidade**: Validar formato do email
- **Ação**: Se inválido, marca como `BLOCKED`
- **Critério**: Email deve ter um @ com texto antes e depois, e pelo menos um ponto depois do @

#### 2. TestModeRule (condicional)
- **Responsabilidade**: Redirecionar emails em modo de teste
- **Ação**: Muda destinatário, adiciona prefixo no assunto e linha no corpo
- **Quando**: Apenas se `HOTEL_TEST_MODE=true`
- **Observação**: Não afeta mensagens já bloqueadas

#### 3. ArchiveCopyRule (condicional)
- **Responsabilidade**: Criar cópia para arquivo
- **Ação**: Adiciona uma segunda mensagem com prefixo no assunto
- **Quando**: Apenas se NÃO estiver em modo de teste
- **Observação**: Não cria cópia de mensagens bloqueadas

### Ordem de Execução

A ordem das regras é importante:

```java
rules.add(new EmailValidationRule());        // 1º - valida
if (testMode) {
    rules.add(new TestModeRule());           // 2º - redireciona
} else {
    rules.add(new ArchiveCopyRule());        // 2º - cria cópia
}
```

## Adicionando Novas Regras

### Exemplo 1: Limite de Horário

```java
public class BusinessHoursRule implements MessageRule {
    @Override
    public void apply(ProcessedMessage message) {
        if (message.getStatus() == MessageStatus.BLOCKED) {
            return; // Não processa mensagens já bloqueadas
        }
        
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(8, 0)) || now.isAfter(LocalTime.of(22, 0))) {
            message.setStatus(MessageStatus.BLOCKED);
        }
    }
}
```

### Exemplo 2: Limite Diário por Hóspede

```java
public class DailyLimitRule implements MessageRule {
    private final MessageRepository messageRepository;
    private final int maxMessagesPerDay;
    
    public DailyLimitRule(MessageRepository messageRepository, int maxMessagesPerDay) {
        this.messageRepository = messageRepository;
        this.maxMessagesPerDay = maxMessagesPerDay;
    }
    
    @Override
    public void apply(ProcessedMessage message) {
        if (message.getStatus() == MessageStatus.BLOCKED) {
            return;
        }
        
        LocalDate today = LocalDate.now();
        int count = messageRepository.countByRecipientAndDate(message.getTo(), today);
        
        if (count >= maxMessagesPerDay) {
            message.setStatus(MessageStatus.BLOCKED);
        }
    }
}
```

### Exemplo 3: Formatação de Assinatura

```java
public class SignatureRule implements MessageRule {
    private final String signature;
    
    public SignatureRule(String signature) {
        this.signature = signature;
    }
    
    @Override
    public void apply(ProcessedMessage message) {
        if (message.getStatus() != MessageStatus.SENT) {
            return;
        }
        
        message.setBody(message.getBody() + "\n\n" + signature);
    }
}
```

### Registrando as Regras

Em `App.java`:

```java
private static MessageProcessor createMessageProcessor() {
    List<MessageRule> rules = new ArrayList<>();
    
    // Validações (sempre executam)
    rules.add(new EmailValidationRule());
    rules.add(new BusinessHoursRule());
    rules.add(new DailyLimitRule(messageRepository, 10));
    
    // Transformações
    boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
    if (testMode) {
        rules.add(new TestModeRule());
    } else {
        rules.add(new ArchiveCopyRule());
    }
    
    // Formatações (sempre executam)
    rules.add(new SignatureRule("--\nPousada Maré Alta\nwww.pousadamarealta.com.br"));
    
    return new MessageProcessor(rules);
}
```

## Padrão Repository

Os repositories encapsulam todo o acesso ao banco de dados:

- **RoomRepository**: Operações com quartos
- **ReservationRepository**: Operações com reservas
- **MessageRepository**: Operações com mensagens

## Padrão Service

Os services contêm a lógica de negócio:

- **ReservationService**: Lógica de reservas, validações, cálculos
- **MessageService**: Orquestração do envio de mensagens
- **ReportService**: Geração de relatórios

## Cálculo de Preços

O cálculo de preços está encapsulado na classe `Reservation`:

```java
private BigDecimal calculateSubtotal(BigDecimal nightlyRate) {
    BigDecimal sum = BigDecimal.ZERO;
    LocalDate current = checkIn;
    while (current.isBefore(checkOut)) {
        BigDecimal nightPrice = nightlyRate;
        if (isHighSeason(current)) {
            nightPrice = nightPrice.multiply(new BigDecimal("1.20"));
        }
        nightPrice = round(nightPrice);  // Arredonda cada noite
        sum = sum.add(nightPrice);
        current = current.plusDays(1);
    }
    return round(sum);  // Arredonda o subtotal
}
```

O arredondamento usa `RoundingMode.HALF_EVEN` (banker's rounding) conforme especificado.

## Estados da Reserva

```
PENDING
   ↓ confirm()
CONFIRMED
   ↓ checkIn()
CHECKED_IN
   ↓ checkOut()
CHECKED_OUT

PENDING ou CONFIRMED pode ir para CANCELLED via cancel()
```

Cada transição valida o estado atual e lança `IllegalStateException` se inválida.

## Validações

As validações estão distribuídas estrategicamente:

- **Controllers**: Validações de formato (datas, campos obrigatórios)
- **Services**: Validações de negócio (capacidade, disponibilidade)
- **Domain**: Validações de estado (transições de status)

## Extensibilidade

O sistema foi projetado para facilitar a adição de:

1. **Novas regras de mensagens**: Implementar `MessageRule`
2. **Novos tipos de quartos**: Adicionar enum em `RoomType` e popular no banco
3. **Novos planos de tarifa**: Adicionar enum em `RatePlan`
4. **Novos relatórios**: Adicionar método em `ReportService` e endpoint no controller

Tudo isso sem modificar código existente, apenas adicionando novos casos nos lugares preparados para recebê-los.
