# Dados — fase 1: só o que o CLI retorna

Ponto de partida enxuto. Nesta fase **não** se mede nada do código produzido,
nada da transcrição e nada de avaliação. Só o que vem no `_result.json` de cada
run, que o runner já salva inteiro.

O `_dados.md` dentro do projeto apenas apresenta esses campos de forma legível.
A fonte da verdade continua sendo o JSON bruto, arquivado junto.

Base deste levantamento: as 20 runs do lote de 12/09/2026.

---

## 1. As medidas

Nove números. São os que variam entre runs e dizem alguma coisa.

| Campo | O que é |
|---|---|
| `num_turns` | Idas e voltas do agente até terminar. Não muda com cache |
| `usage.output_tokens` | Tudo que o modelo escreveu. Não muda com cache |
| `usage.output_tokens_details.thinking_tokens` | Quanto disso foi raciocínio |
| `usage.input_tokens` | Entrada nova, cobrada a preço cheio |
| `usage.cache_creation_input_tokens` | Entrada gravada no cache nesta run |
| `usage.cache_read_input_tokens` | Entrada lida do cache |
| `total_cost_usd` | Custo da run |
| `duration_ms` | Relógio, do início ao fim |
| `duration_api_ms` | Tempo esperando o modelo. Cerca de 96% do total |

**A única conta a fazer:** `total_input = input_tokens + cache_creation + cache_read`.
É o tamanho real de tudo que o modelo leu, e não muda com o cache ligado ou
desligado — o cache muda o preço desses tokens, não a quantidade.

---

## 2. Como terminou

| Campo | O que é |
|---|---|
| `is_error` | Se a run falhou. Run com erro sai da comparação |
| `stop_reason` | `end_turn` quando o agente decidiu que acabou |
| `terminal_reason` | `completed`, ou erro de API, ou timeout |
| `api_error_status` | Código HTTP quando falha. `429` é estouro da cota de 5 horas |
| `result` | O texto final do agente. Serve como registro, **não como avaliação** — é o agente julgando o próprio trabalho |

---

## 3. Identificação

| Campo | O que é |
|---|---|
| `session_id` | Liga a run à transcrição arquivada |
| `uuid` | Identificador da mensagem final |
| `modelUsage.<modelo>.canonicalModel` | O modelo que de fato atendeu. Protege contra o apelido apontar para outra versão no meio do lote |

---

## 4. Latência — coletado, não usado como sinal

`ttft_ms`, `ttft_stream_ms`, `first_content_frame_ms`, `time_to_request_ms`.

Variam bastante, mas respondem a carga de servidor e fila, não a comportamento
do agente. Ficam no JSON; não entram em comparação.

---

## 5. Constantes nas 20 runs — vão para o `lote.md`

Trinta e quatro campos não mudaram nenhuma vez. Repetir em 30 arquivos é ruído;
uma linha por lote basta. Se algum **variar**, o runner avisa e ele sobe para o
relatório da run.

| Grupo | Campos | Valor observado |
|---|---|---|
| Atendimento | `usage.service_tier`, `usage.speed` | `standard` |
| Modo rápido | `fast_mode_state`, `fast_mode_disabled_reason` | `off`, `sdk_opt_in_required` |
| Modelo | `contextWindow`, `maxOutputTokens`, `provider`, `costBasis` | 1.000.000, 64.000, `firstParty`, `list` |
| Subagentes | os 14 campos de `subagent_stats` | todos 0 |
| Ferramentas de servidor | `web_search_requests`, `web_fetch_requests` | 0 |
| Permissões | `permission_denials` | lista vazia |
| Cache curto | `ephemeral_5m_input_tokens` | 0 — todo o cache foi de 1 hora |
| Estrutura | `type`, `subtype`, `result_index`, `queued_turn_count` | fixos |

**Uma exceção:** `usage.inference_geo` teve **dois valores diferentes** nas 20
runs. Passa a ser registrado por run, não no lote.

---

## 6. Sobre o `modelUsage`

Repete os mesmos números de `usage`, mas indexados pelo modelo. Só vale a pena
quando o CLI aciona um modelo auxiliar: no primeiro par isso aconteceu, com 21
tokens de saída de um Haiku; nas 20 runs do lote, não aconteceu nenhuma vez.

Regra: se `modelUsage` tiver mais de uma chave, os tokens do auxiliar são
registrados à parte, para não poluírem o número do modelo medido.

---

## 7. O que fica fora desta fase

Nada disso é coletado agora:

- métricas do código produzido — arquivos, linhas, testes, abstrações;
- comportamento lido da transcrição — ferramentas usadas, builds, retrabalho;
- cumprimento dos requisitos;
- notas de design, suas ou do juiz.

Todas continuam possíveis depois, porque o workspace, a transcrição e o JSON
ficam arquivados em cada run. Entram quando você decidir, sem precisar rodar
nada de novo.
