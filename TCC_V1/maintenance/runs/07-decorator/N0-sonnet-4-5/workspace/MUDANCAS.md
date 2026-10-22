# Mudanças no Sistema de E-mails

## Resumo

Implementadas três novas funcionalidades no sistema de envio de e-mails da Pousada Maré Alta:

### 1. Validação de E-mail

Mensagens para endereços inválidos são bloqueadas e aparecem na lista com status `BLOCKED`.

**Critérios de validação:**
- Deve ter exatamente um `@`
- Deve ter conteúdo antes do `@`
- Deve ter conteúdo depois do `@` com pelo menos um ponto (ex: `ana@gmail.com`)

**Exemplos:**
- ✅ Válido: `ana@gmail.com`, `user@example.com.br`
- ❌ Inválido: `ana@gmail`, `ana.gmail.com`, `@gmail.com`, `ana@@gmail.com`

### 2. Cópia para Arquivo

Toda mensagem enviada (não bloqueada, em modo normal) gera uma cópia automática para `arquivo@pousadamarealta.com.br`.

**Características:**
- Assunto: `[Cópia] ` + assunto original
- Corpo: mesmo da mensagem original
- Aparece na lista de mensagens logo após a original
- **Não é gerada em modo de teste**

### 3. Modo de Teste

Quando `HOTEL_TEST_MODE=true`, mensagens são redirecionadas para evitar envio para hóspedes reais.

**Comportamento:**
- Destinatário: `testes@pousadamarealta.com.br`
- Assunto: `[TESTE] ` + assunto original
- Corpo: `Destinatário original: [e-mail]` + corpo original
- Não gera cópia para arquivo
- Endereços inválidos continuam bloqueados (mantém endereço original)

## Mudanças Técnicas

### Banco de Dados

- **Nova migração:** `V2__add_message_status.sql`
- **Campo adicionado:** `status` (TEXT) na tabela `messages`

### Novas Classes

1. **MessageStatus** (enum): `SENT` ou `BLOCKED`
2. **EmailValidator**: valida formato de e-mail
3. **OutgoingMessage**: representa mensagem antes de ser persistida
4. **MessageRule** (interface): define regras de processamento
5. **EmailValidationRule**: regra de validação de e-mail
6. **TestModeRule**: regra de modo de teste
7. **ArchiveCopyRule**: regra de cópia para arquivo

### Classes Modificadas

- **Message**: adicionado campo `status`
- **SentMessages**: atualizado para incluir status
- **EmailSender**: refatorado para aplicar regras
- **Hotel**: configuração das regras ao inicializar

### API

**GET /messages** agora retorna:
```json
{
  "id": 1,
  "channel": "EMAIL",
  "to": "ana@example.com",
  "subject": "Assunto",
  "body": "Corpo da mensagem",
  "status": "SENT"
}
```

## Testes

Adicionados novos testes:

- **EmailValidatorTest**: validação de e-mail
- **MessageRulesTest**: todas as regras isoladas e combinadas
- **ProductionModeTest**: comportamento em modo normal (não teste)

**Total de testes:** 37 (todos passando)

## Como Usar

### Modo Normal (Produção)

```bash
java -jar target/hotel.jar
```

Comportamento:
- E-mails válidos: enviados + cópia para arquivo
- E-mails inválidos: bloqueados

### Modo de Teste

```bash
HOTEL_TEST_MODE=true java -jar target/hotel.jar
```

Comportamento:
- E-mails válidos: redirecionados para `testes@pousadamarealta.com.br`
- E-mails inválidos: bloqueados
- Sem cópias para arquivo
