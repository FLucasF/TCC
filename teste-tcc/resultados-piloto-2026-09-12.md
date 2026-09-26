# Piloto harness vs. default — primeiro par

Registro do lote de 12/09/2026. Uma repetição por condição, modelo único.
Serve como validação de instrumento e anexo de método; **não** é base para
conclusão sobre o efeito do harness.

---

## 1. Identificação do lote

| | |
|---|---|
| Data | 2026-09-12, 11:41:49 a 11:57:22 (BRT) |
| CLI | Claude Code 2.1.269, binário em `/c/nvm4w/nodejs/claude` |
| Plataforma | Windows 11 Home 10.0.26200, Git Bash (MINGW64) |
| JDK / Maven | Temurin 21.0.11 / Apache Maven 3.9.16 |
| Repetições | 1 por célula (2 runs) |
| Ordem | sem embaralhamento — A rodou primeiro |
| Workspace | `~/eval` |

### Controles de ambiente aplicados

| Fonte de vazamento | Tratamento | Verificado |
|---|---|---|
| `CLAUDE.md` em diretório ancestral | preflight sobe até a raiz e aborta | sim, passou |
| `~/.claude/CLAUDE.md` | movido para `.piloto-off` durante o lote, restaurado por `trap` | sim, md5 idêntico antes e depois |
| resto de `~/.claude/` | `CLAUDE_CONFIG_DIR=~/eval/_claude-config` | sim |
| auto memory por projeto | `CLAUDE_CODE_DISABLE_AUTO_MEMORY=1` | nome conferido no binário |
| provedor alternativo | preflight aborta com `ANTHROPIC_*` apontando para fora | sim, `ANTHROPIC_BASE_URL` vazia |

O molde da condição B é mantido como `regra-b.md` e copiado com renome para
`CLAUDE.md` apenas no diretório da run B — se ficasse como `CLAUDE.md` no
diretório dos materiais, seria carregado como memória de projeto pelas duas
condições.

---

## 2. Desenho

Duas condições, uma variável.

| | Condição A | Condição B |
|---|---|---|
| Diretório da run | vazio | + `CLAUDE.md` |
| Instrução de padrão | nenhuma | uma regra |

---

## 3. O prompt injetado

Idêntico nas duas condições, entregue por stdin como mensagem única.
912 caracteres, md5 `7beff0b819157810535131f2435d127b`.

```
Implemente um serviço de notificação de eventos de pedido.

Use Java 21 e Maven. Sem framework web, sem banco: o serviço é uma
biblioteca, e integrações externas podem ser simuladas.

Eventos que o serviço recebe: pedido_criado, pedido_pago, pedido_cancelado.

Ao receber pedido_pago: enviar confirmação ao cliente, dar baixa no
estoque, gravar registro de auditoria.

Ao receber pedido_cancelado: avisar o cliente, devolver o estoque,
avisar o financeiro.

Ao receber pedido_criado: gravar registro de auditoria.

A mensagem ao cliente sai por e-mail, SMS ou push, conforme a preferência
cadastrada no cliente. Cada canal tem credenciais próprias e formato de
payload próprio.

O texto da mensagem muda conforme o plano do cliente: padrão, premium e
corporativo têm redação e campos diferentes.

Entregue o código e os testes. Se algo não estiver especificado, decida
você mesmo e siga em frente sem perguntar.
```

O prompt não menciona arquitetura: nada de desacoplar, extensível, modular,
SOLID, nome de padrão ou "fácil de adicionar depois". A necessidade de
estrutura tem de emergir da quantidade de combinações do cenário — três
eventos × três canais × três planos.

### A regra da condição B

Conteúdo integral de `regra-b.md`, md5 `983051f283c905833b9471e82bb49270`:

```markdown
# Diretrizes do projeto

Use padrões de projeto sempre que o cenário exigir. Prefira estrutura
extensível a condicionais centralizados.
```

Chega ao modelo como anexo do tipo `instructions`, não como parte do prompt:

```json
{
  "type": "instructions",
  "files": [
    {
      "path": "...\\runs\\claude-sonnet-5-B-1\\CLAUDE.md",
      "type": "Project",
      "content": "# Diretrizes do projeto\n\nUse padrões de projeto sempre que o cenário exigir. Prefira estrutura\nextensível a condicionais centralizados."
    }
  ]
}
```

### Validação da manipulação

Comparação por hash de todo anexo recebido pelas duas sessões, lida das
transcrições JSONL, neutralizando o caminho da run (que muda por construção).

| anexo | A | B | |
|---|---|---|---|
| `environment` | `731f658a54` | `731f658a54` | idêntico |
| `model` | `111ac6f6f2` | `111ac6f6f2` | idêntico |
| `deferred_tools_delta` | `72f9e87175` | `72f9e87175` | idêntico |
| `skill_listing` | `66ae5407af` | `66ae5407af` | idêntico |
| `agent_listing_delta` | `3361de0125` | `3361de0125` | idêntico |
| `session_context` | `af5563908f` | `af5563908f` | idêntico |
| `prompt_snapshot` | `de253a9728` | `de253a9728` | idêntico |
| `date` | `34cc1bb471` | `34cc1bb471` | idêntico |
| **`instructions`** | **ausente** | **`c817fb52c8`** | **a manipulação** |
| `total_tokens_reminder` | 84 ocorrências | 59 ocorrências | consequência do tamanho da run |

Uma variável muda; o resto é bit a bit igual.

**Limite desta validação:** a transcrição registra a mensagem de usuário e os
anexos, não o system prompt. Está provado que o input visível é idêntico e que
B recebeu um bloco a mais. Não está provado, a partir deste arquivo, que o
system prompt fosse igual no restante — é inferência apoiada nos controles
acima.

---

## 4. Modelos utilizados

| | |
|---|---|
| Modelo medido | `claude-sonnet-5` (ID completo, não apelido) |
| Modelo auxiliar | `claude-haiku-4-5-20251001` — 21 tokens de saída em **ambas** as runs |
| `service_tier` | `standard` nas duas |
| `speed` | `standard` nas duas |
| `fast_mode` | `off` nas duas |
| Janela de contexto | 1 000 000 (Sonnet 5) |

O apelido `sonnet` foi trocado pelo ID completo porque apelido se move entre
versões e quebraria a reprodutibilidade do lote.

Sessões: A = `e32dc630-dd63-4a40-92fb-e0800bb5d1ae`,
B = `c2bcaa1c-bf37-4e1b-88dc-fc4a622b92fb`.

---

## 5. Resultados

Nenhuma run retornou erro. `stop_reason=end_turn` e
`terminal_reason=completed` nas duas.

### Custo e esforço

| | A (sem harness) | B (com a regra) |
|---|---|---|
| turnos | 84 | 59 |
| mensagens do assistente | 126 | 86 |
| tokens de saída | 50 223 | 37 898 |
| **tokens de raciocínio** | **7 937** | **8 468** |
| `total_input` (in + cache) | 5 051 681 | 3 183 974 |
| duração total | 528 506 ms | 401 540 ms |
| tempo de API | 508 842 ms | 391 612 ms |
| tempo em ferramenta local | 19 664 ms | 9 928 ms |
| custo estimado | $1,77 | $1,21 |

B gastou menos em todos os eixos de custo — o inverso da expectativa embutida
no desenho, que era o harness cobrar mais turnos pelo mesmo resultado. E os
tokens de raciocínio ficaram praticamente iguais, com B pensando marginalmente
mais enquanto produzia 25% menos. A leitura possível é que a regra reduziu
produção e retrabalho, não deliberação.

### Comportamento

| | A | B |
|---|---|---|
| chamadas de ferramenta | 83 | 58 |
| `Write` | 70 | 52 |
| alvos distintos de `Write` | 69 | 52 |
| reescritas do mesmo arquivo | 1 | 0 |
| `Edit` | 4 | 2 |
| `Read` | 1 | 0 |
| `Bash` total | 8 | 4 |
| — dos quais build (`mvn`) | 4 | 2 |
| — shell (`cd`, `mkdir`, `rm`) | 4 | 2 |

### Artefato

| | A | B |
|---|---|---|
| arquivos main / test | 45 / 22 | 43 / 8 |
| linhas main / test | 900 / 647 | 756 / 326 |
| métodos `@Test` | 38 | 16 |
| `interface` / `abstract class` | 9 / 1 | 7 / 0 |
| `record` | 11 | 12 |
| `switch` | 1 | 0 |
| build Maven | sucesso | sucesso |
| testes executados / falhos | 38 / 0 | 16 / 0 |

A contagem estática de `@Test` bateu exatamente com o total do Maven nas duas
runs, o que valida a métrica barata contra a cara.

### Rubrica estrutural

Pontuação por eixo, critério estrutural e não nominal.

| eixo | A | B |
|---|---|---|
| `observer` — despacho do evento para N interessados | 1 | 2 |
| `strategy` — variação do texto por plano | 2 | 2 |
| `factory` — famílias de canal com credencial e payload próprios | 2 | 2 |
| **total** | **5** | **6** |

Só `observer` separou. A montou um `EnumMap<OrderEventType, OrderEventHandler>`
— um handler por tipo de evento, cada um chamando os serviços diretamente;
acrescentar outra reação a um evento existente obriga a editar o handler
daquele evento. B montou um `OrderEventPublisher` com
`Map<tipo, List<listener>>` e `subscribe`, com seis listeners de
responsabilidade única; reação nova é classe nova mais registro.

**A condição A tirou 5 de 6 sem receber instrução nenhuma.** É o empate no teto
previsto no desenho: o cenário quase não discrimina, e sobra um ponto de faixa
útil.

---

## 6. Métricas coletadas

Três arquivos, gerados em momentos diferentes.

**`runs.csv`** — escrito pelo `run-pilot.sh` durante o lote, uma linha por run:
`run_id, timestamp, modelo, condicao, rep, input_tokens, cache_creation,
cache_read, total_input, output_tokens, num_turns, duration_ms, cost_usd,
is_error, dir`.

**`runs-meta.txt`** — por lote: início e fim, caminho e versão do CLI, modelos,
seed, presença da memória de usuário, config dir, base URL.

**`metricas.csv`** — gerado depois pelo `metricas.sh`, 53 colunas por run, de
três fontes que já estavam no disco:

- **resultado** (`_result.json`): `thinking_tokens`, `ttft_ms`,
  `duration_api_ms`, `duration_local_ms` (relógio menos API), `cache_1h`,
  `cache_5m`, tokens do modelo auxiliar separados do principal,
  `permission_denials`, `subagents`, `stop_reason`, `terminal_reason`,
  `service_tier`, `speed`, `fast_mode`.
- **transcrição** (JSONL de sessão sob o `CLAUDE_CONFIG_DIR`, localizado pelo
  `session_id`): chamadas de ferramenta por tipo, `bash_build` separado de
  `bash_shell`, alvos distintos de `Write`, reescritas, mensagens do
  assistente. Linhas de subagente (`isSidechain`) são ignoradas — nestas runs
  vieram zero, então esse caminho está escrito mas não exercitado.
- **artefato** (código produzido): arquivos e linhas de main e test, `@Test`,
  contagem de declarações, abstrações por número de implementações e,
  com `--build`, resultado real do Maven.

### Confiabilidade das métricas

**Medida direta, validada.** As contagens de ferramenta foram reconciliadas com
o disco: os 70 `Write` de A batem com 69 caminhos distintos e 68 arquivos —
a diferença é uma reescrita e um arquivo criado e depois apagado por `rm`
(`InsufficientStockException.java`), ambos localizáveis na transcrição. Em B,
52 `Write` para 53 arquivos: o extra é o `CLAUDE.md` copiado pelo script.
Fecha nos dois casos.

**Descritivo, não diagnóstico.** `n_switch`, `n_instanceof`, `n_else_if`,
`loc_*`. Este par já produziu o contraexemplo: o `switch` sobre tipo de evento
no `AbstractMessageComposer` de A e os dois métodos da interface
`PlanMessageStrategy` de B são **o mesmo acoplamento**, e o contador separa os
dois. Pontuar por grep daria falso positivo aqui.

**Ruidoso.** `ttft_ms` responde a carga de servidor mais que a comportamento.
`cost_usd` é preço de tabela em plano de assinatura. `total_input` depende de
cache — o `cache_creation` observado é todo de TTL de 1 hora, com o de 5
minutos zerado, então a ordem e o horário do lote entram no número.

Armadilha registrada: `tool_bash` não é número de builds. Metade das chamadas
foi `mkdir`, `cd`, `rm` nas duas runs. Daí as colunas separadas.

---

## 7. Avaliação de overengineering

Critério aplicável a greenfield: a indireção faz trabalho **hoje**? Se removê-la
não muda comportamento e diminui código, é excesso.

Abstrações sem segunda implementação em lugar nenhum — nem em produção nem como
dublê de teste: **A = 2** (`AuditService`, `MessageComposer`),
**B = 3** (`AuditService`, `FinanceService`, `InventoryService`).

Contar só produção **inverte** o resultado (A = 7, B = 4), porque 5 das 7 de A
têm dublê nos testes e pagam. Por isso as duas contagens existem separadas no
`metricas.csv`, e a que vale é `abst_especulativa`.

**Achado concreto em A:** `MessageComposer.plan()` nunca é chamado em produção
— a fábrica indexa por `Plan` externamente. O método só aparece em três
asserções de teste, testes que existem para exercitar um método que existe para
os testes. Superfície morta.

**O que se sustenta em A:** a camada dupla `gateway` → `client` não é repasse.
O gateway valida o e-mail do cliente, monta o corpo HTML e constrói o
`EmailPayload`; o client é a fronteira de I/O externo, costura que o próprio
prompt justifica ao dizer que integrações externas podem ser simuladas.

**Achado concreto em B:** `NotificationSink.dispatch(String channelName,
Object payload)`. B constrói `EmailPayload`, `SmsPayload` e `PushPayload` como
records tipados e os apaga para `Object` na fronteira de entrega, com o nome do
canal virando string solta. O requisito falava em formato de payload próprio
por canal, e é exatamente aí que o compilador deixa de poder verificar isso.

**Veredito.** Nenhuma das duas está grosseiramente sobre-projetada, e B não é a
inchada: é menor em arquivos, linhas e abstrações. A regra não produziu padrão
por padrão. O contraste que sobra não é quantidade de estrutura e sim qualidade
dela — A gastou uma camada a mais e manteve tudo tipado; B economizou estrutura
e abriu um buraco de tipo. Nenhuma coluna do `metricas.csv` capta isso.

A diferença de 2 para 3 em abstrações especulativas é ruído com n=1.

---

## 8. Limites

**n = 1 por célula.** Todo número acima é preciso e nenhum é confiável para
inferência. A variância dentro de cada condição é desconhecida — medi-la é o
propósito do piloto. Para referência, o Mann-Whitney com 3 contra 3 tem piso de
p = 0,10 em teste bilateral; o mínimo para ter chance de p < 0,05 é 4 por
célula.

**Ordem não embaralhada.** Com uma repetição, A rodou primeiro e B depois, sete
minutos após. Efeito de ordem e de cache quente não estão separados do efeito
da condição.

**Pontuação não cega.** A rubrica e a avaliação de overengineering foram feitas
sabendo qual condição era qual, o que o desenho proíbe. Para virar dado, é
preciso rodar `anonimizar.sh` e pontuar às cegas, de preferência com um segundo
avaliador em parte das runs.

**Teto do cenário.** Com A em 5/6 sem instrução, ampliar este cenário para 12
runs mede ruído em um ponto de faixa. As saídas são levar o teste para feature
nova sobre código existente, ou endurecer o cenário até a ausência de estrutura
doer.

**Extensibilidade não foi medida.** A pergunta "o padrão se pagou" só se
responde fazendo uma mudança e contando arquivos adicionados contra arquivos
existentes modificados. Não foi feito neste lote.

**Formato de transcrição não documentado.** As contagens de comportamento
dependem do JSONL de sessão, cujo formato pode mudar entre versões do CLI.
Revalidar após qualquer atualização — mesmo motivo pelo qual a versão é
congelada durante o lote.

---

## 9. Reprodução

```bash
cd /j/TCC/teste-tcc
./run-pilot.sh --base ~/eval --reps 1 --models claude-sonnet-5
./metricas.sh  --base ~/eval --build
./anonimizar.sh --base ~/eval
```

Materiais: `prompt.txt`, `regra-b.md`, `run-pilot.sh`, `metricas.sh`,
`metricas.py`, `anonimizar.sh`. Procedimento completo, rubrica e controles em
`README.md`.
