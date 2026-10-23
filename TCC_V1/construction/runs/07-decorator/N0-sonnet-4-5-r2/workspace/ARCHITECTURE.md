# Arquitetura do Sistema de Mensagens

## Visão Geral

O sistema de mensagens da Pousada Maré Alta foi projetado com extensibilidade em mente. A arquitetura permite adicionar novas regras de processamento de mensagens sem modificar o código existente.

## Design Pattern: Chain of Responsibility

O sistema utiliza o padrão Chain of Responsibility com uma pipeline de processadores de mensagens.

### Componentes Principais

#### 1. MessageProcessor (Interface)
```java
public interface MessageProcessor {
    ProcessingResult process(Message message, ProcessingContext context);
}
```

Cada regra de processamento implementa esta interface.

#### 2. ProcessingContext
Carrega informações de contexto para os processadores:
- `isTestMode()`: Indica se o sistema está em modo de teste

#### 3. ProcessingResult
Resultado do processamento de uma mensagem:
- `getMessage()`: A mensagem processada (possivelmente modificada)
- `getAdditionalMessages()`: Lista de mensagens adicionais geradas (ex: cópias)
- `shouldContinue()`: Se a cadeia deve continuar processando

#### 4. MessageService
Orquestra a pipeline de processamento:

```java
public void send(Message message) {
    // Para cada processador na cadeia
    // Processa a mensagem e coleta resultados
    // Salva todas as mensagens finais no repositório
}
```

### Regras Implementadas

#### 1. EmailValidationRule
- **Objetivo**: Validar formato de e-mail
- **Comportamento**: 
  - Se inválido: marca como BLOCKED e interrompe a cadeia
  - Se válido: continua o processamento
- **Validação**: `algo@dominio.com` com pelo menos um ponto no domínio

#### 2. TestModeRule
- **Objetivo**: Redirecionar e-mails em modo de teste
- **Comportamento**:
  - Se `context.isTestMode()`:
    - Redireciona para `testes@pousadamarealta.com.br`
    - Adiciona prefixo `[TESTE]` no assunto
    - Adiciona linha `Destinatário original: ...` no corpo
  - Senão: não faz nada
- **Ordem**: Executa ANTES da ArchiveCopyRule

#### 3. ArchiveCopyRule
- **Objetivo**: Criar cópia para arquivo
- **Comportamento**:
  - Se NÃO estiver em modo de teste:
    - Cria mensagem adicional para `arquivo@pousadamarealta.com.br`
    - Adiciona prefixo `[Cópia]` no assunto
    - Mantém mesmo corpo
  - Se em modo de teste: não gera cópia

### Fluxo de Processamento

```
Mensagem Original
    ↓
EmailValidationRule
    ├─ INVÁLIDO → BLOCKED (fim da cadeia)
    └─ VÁLIDO → continua
        ↓
TestModeRule
    ├─ Modo Teste → modifica destinatário/assunto/corpo
    └─ Modo Normal → não modifica
        ↓
ArchiveCopyRule
    ├─ Modo Teste → não gera cópia
    └─ Modo Normal → gera mensagem adicional (cópia)
        ↓
Todas as mensagens salvas no repositório
```

### Ordem de Execução

A ordem é crítica:
1. **EmailValidationRule** - PRIMEIRO (bloqueia inválidos antes de qualquer processamento)
2. **TestModeRule** - SEGUNDO (modifica antes da cópia)
3. **ArchiveCopyRule** - TERCEIRO (gera cópia da mensagem já modificada)

### Como Adicionar Novas Regras

#### Exemplo 1: Não enviar de madrugada

```java
public class NightTimeBlockRule implements MessageProcessor {
    @Override
    public ProcessingResult process(Message message, ProcessingContext context) {
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(0, 0)) && now.isBefore(LocalTime.of(6, 0))) {
            message.setStatus(MessageStatus.BLOCKED);
            return ProcessingResult.block(message);
        }
        return ProcessingResult.continueProcessing(message);
    }
}
```

#### Exemplo 2: Limite de mensagens por destinatário

```java
public class RateLimitRule implements MessageProcessor {
    private final MessageRepository repository;
    private final int maxPerDay = 5;
    
    @Override
    public ProcessingResult process(Message message, ProcessingContext context) {
        long count = repository.countByRecipientToday(message.getTo());
        if (count >= maxPerDay) {
            message.setStatus(MessageStatus.BLOCKED);
            return ProcessingResult.block(message);
        }
        return ProcessingResult.continueProcessing(message);
    }
}
```

#### Registrando a Nova Regra

No `MessageService.buildProcessors()`:

```java
private List<MessageProcessor> buildProcessors() {
    List<MessageProcessor> processors = new ArrayList<>();
    processors.add(new EmailValidationRule());
    processors.add(new NightTimeBlockRule());      // Nova regra
    processors.add(new TestModeRule());
    processors.add(new ArchiveCopyRule());
    return processors;
}
```

### Configuração Dinâmica (Futuro)

Para permitir configuração por instalação:

```java
// configuration.json
{
  "rules": [
    { "type": "EmailValidation", "enabled": true },
    { "type": "NightTimeBlock", "enabled": true, "hours": [0, 6] },
    { "type": "TestMode", "enabled": true },
    { "type": "ArchiveCopy", "enabled": true },
    { "type": "RateLimit", "enabled": false, "maxPerDay": 5 }
  ]
}
```

### Benefícios da Arquitetura

1. **Extensibilidade**: Novas regras sem modificar código existente
2. **Testabilidade**: Cada regra pode ser testada isoladamente
3. **Manutenibilidade**: Regras são independentes e fáceis de entender
4. **Configurabilidade**: Fácil adicionar configuração por instalação
5. **Composição**: Regras podem ser combinadas de diferentes formas

### Pontos de Atenção

1. **Ordem importa**: EmailValidation deve ser primeiro
2. **Imutabilidade**: Processadores modificam a mensagem, não criam cópias
3. **Interrupção**: `block()` interrompe a cadeia imediatamente
4. **Mensagens adicionais**: Só entram na cadeia se o processador retornar na próxima iteração

### Estado das Mensagens

- **SENT**: Processada com sucesso, pronta para envio
- **BLOCKED**: Bloqueada por alguma regra (endereço inválido, limite excedido, horário inadequado, etc.)

As mensagens bloqueadas aparecem na lista para que a recepção possa tomar ação (ex: ligar para o hóspede).
