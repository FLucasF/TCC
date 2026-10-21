# Sistema de Mensagens - Pousada Maré Alta

## Visão Geral

O sistema de mensagens implementa três camadas de proteção:
1. **Validação de email** - Rejeita endereços inválidos
2. **Cópia para arquivo** - Mantém registros de todas as comunicações
3. **Modo de teste** - Protege clientes reais durante testes

## Validação de Email

Um email é considerado **válido** quando:
- Contém exatamente um caractere `@`
- Tem pelo menos um caractere antes do `@`
- Tem um domínio após o `@` com pelo menos um ponto (ex: `exemplo.com`)

### Exemplos

**Válidos:**
- `usuario@exemplo.com` ✓
- `nome.sobrenome@empresa.com.br` ✓
- `teste@sub.dominio.org` ✓

**Inválidos:**
- `usuario@gmail` ✗ (sem ponto no domínio)
- `usuario.gmail.com` ✗ (sem @)
- `@exemplo.com` ✗ (sem conteúdo antes de @)
- `usuario@@exemplo.com` ✗ (dois @)
- `usuario@.exemplo.com` ✗ (ponto no início do domínio)

## Modo Normal (Produção)

Quando `HOTEL_TEST_MODE` não está definido ou é `false`:

### Email Válido
```
Para: joao@example.com
Assunto: Reserva confirmada - Pousada Maré Alta
Status: SENT
Corpo: [comprovante + mensagem]

Para: arquivo@pousadamarealta.com.br (cópia automática)
Assunto: [Cópia] Reserva confirmada - Pousada Maré Alta
Status: SENT
Corpo: [mesmo conteúdo]
```

### Email Inválido
```
Para: invalido@gmail (email original)
Assunto: Reserva confirmada - Pousada Maré Alta
Status: BLOCKED
```

**Nota:** Mensagens bloqueadas não geram cópia para arquivo.

## Modo Teste

Quando `HOTEL_TEST_MODE=true`:

### Email Válido
```
Para: testes@pousadamarealta.com.br (redirecionado)
Assunto: [TESTE] Reserva confirmada - Pousada Maré Alta
Status: SENT
Corpo: 
  Destinatário original: joao@example.com
  
  [comprovante + mensagem original]
```

**Características:**
- Email redirecionado para o servidor de testes
- Assunto prefixado com `[TESTE] `
- Primeira linha mostra o destinatário original
- Sem cópia para arquivo

### Email Inválido
```
Para: invalido@gmail (email original mantido)
Assunto: Reserva confirmada - Pousada Maré Alta
Status: BLOCKED
```

**Nota:** Mesmo em modo teste, emails inválidos são bloqueados com o endereço original.

## Exemplo de Fluxo Completo

### Modo Normal

1. Hóspede cria reserva com `joao@example.com`
   ```
   POST /reservations
   → Reserva criada com status PENDING
   ```

2. Hóspede confirma pagamento
   ```
   POST /reservations/1/confirmation
   → Status muda para CONFIRMED
   → Email enviado para joao@example.com
   → Cópia enviada para arquivo@pousadamarealta.com.br
   → GET /messages retorna 2 mensagens SENT
   ```

### Modo Teste

1. Hóspede cria reserva com `joao@example.com`
   ```
   HOTEL_TEST_MODE=true
   POST /reservations
   → Reserva criada com status PENDING
   ```

2. Hóspede confirma pagamento
   ```
   POST /reservations/1/confirmation
   → Status muda para CONFIRMED
   → Email REDIRECIONADO para testes@pousadamarealta.com.br
   → Sem cópia para arquivo
   → GET /messages retorna 1 mensagem SENT
   ```

## API - GET /messages

Retorna um array de todas as mensagens criadas pelo sistema.

### Estrutura da Mensagem

```json
{
  "id": 1,
  "channel": "EMAIL",
  "to": "joao@example.com",
  "subject": "Reserva confirmada - Pousada Maré Alta",
  "body": "Olá, João Silva!\n\nSua reserva na Pousada Maré Alta está confirmada...",
  "status": "SENT"
}
```

### Campos

- `id`: Identificador único da mensagem
- `channel`: Sempre "EMAIL" por enquanto
- `to`: Destinatário (pode ser redirecionado em modo teste, mantém original se bloqueado)
- `subject`: Assunto da mensagem
- `body`: Corpo completo da mensagem
- `status`: "SENT" (enviada) ou "BLOCKED" (bloqueada)

## Considerações Futuras

O sistema foi projetado para ser extensível. Possíveis extensões:

1. **Limite de mensagens por hóspede por dia**
   - Pode ser configurado por ambiente (sempre, só em teste, etc)

2. **Restrição de horário**
   - Não enviar emails entre 22h e 6h
   - Configurável por ambiente

3. **Diferentes endereços de arquivo**
   - Diferentes instalações podem querer diferentes emails de arquivo
   - Ou desabilitar totalmente a cópia

4. **Persistência**
   - Atualmente em memória
   - Pode migrar para banco de dados

5. **Envio real de email**
   - Atualmente simula o envio
   - Pode integrar com SMTP

## Testando Localmente

### Modo Normal
```bash
java -jar target/reservas-1.0.0.jar
```

### Modo Teste
```bash
HOTEL_TEST_MODE=true java -jar target/reservas-1.0.0.jar
```

### Verificar Mensagens
```bash
curl http://localhost:7070/messages | jq '.'
```

### Filtrar por Status
```bash
# Apenas SENT
curl http://localhost:7070/messages | jq '.[] | select(.status == "SENT")'

# Apenas BLOCKED
curl http://localhost:7070/messages | jq '.[] | select(.status == "BLOCKED")'
```
