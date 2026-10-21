# Mudanças no Sistema de E-mails

## Resumo

O sistema de e-mails foi expandido com três novas funcionalidades:

1. **Validação de endereços de e-mail**
2. **Cópia automática para arquivo**
3. **Modo de teste**

## Detalhamento

### 1. Validação de E-mail

Mensagens com endereços de e-mail inválidos são bloqueadas e não são enviadas. Um e-mail é considerado válido quando:
- Contém exatamente um `@`
- Tem algum texto antes do `@`
- Tem um domínio após o `@` com pelo menos um ponto (ex: `gmail.com`)

**Exemplos de e-mails inválidos:**
- `ana@gmail` (sem ponto no domínio)
- `ana.gmail.com` (sem @)
- `@gmail.com` (nada antes do @)
- `ana@@gmail.com` (dois @)

**Comportamento:**
- Mensagens bloqueadas aparecem na lista com `status: BLOCKED`
- O endereço original é mantido na mensagem bloqueada
- Mensagens bloqueadas NÃO geram cópia para arquivo

### 2. Cópia para Arquivo

Em modo normal (produção), toda mensagem enviada com sucesso gera uma cópia para `arquivo@pousadamarealta.com.br`.

**Características da cópia:**
- Mesmo texto da mensagem original
- Assunto prefixado com `[Cópia] `
- Aparece na lista de mensagens logo após a original
- Também tem `status: SENT`

### 3. Modo de Teste

Quando a variável de ambiente `HOTEL_TEST_MODE=true` está definida, o sistema entra em modo de teste:

**Comportamento:**
- Mensagens são redirecionadas para `testes@pousadamarealta.com.br`
- Assunto é prefixado com `[TESTE] `
- Primeira linha do corpo é `Destinatário original: {endereço_real}`
- NÃO gera cópia para arquivo
- Validação de e-mail continua funcionando normalmente

**Observação:** Mensagens bloqueadas mantêm o endereço original (não são redirecionadas).

## Campo Status

Todas as mensagens agora incluem um campo `status` na API:
- `SENT`: mensagem enviada com sucesso
- `BLOCKED`: mensagem bloqueada por validação

## API

### GET /messages

Retorna todas as mensagens enviadas, incluindo:
- Mensagens para hóspedes (originais)
- Cópias para arquivo
- Mensagens bloqueadas

Exemplo de resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla!...",
    "status": "SENT"
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "arquivo@pousadamarealta.com.br",
    "subject": "[Cópia] Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla!...",
    "status": "SENT"
  }
]
```

## Arquitetura

As mudanças foram implementadas usando um sistema extensível de regras de e-mail:

- `EmailValidationRule`: valida endereços de e-mail
- `TestModeRule`: redireciona mensagens no modo de teste
- `ArchiveCopyRule`: cria cópias para arquivo

Cada regra pode ser aplicada ou não dependendo da configuração, facilitando a adição de novas regras no futuro (como limites de mensagens por dia ou bloqueio de envio em horários específicos).
