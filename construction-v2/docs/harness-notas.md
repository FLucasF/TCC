# Notas do harness

> [!danger] Este arquivo nunca pode entrar em `harness/`
> `scripts/executar.sh` faz `cp -r harness/. workspace/`: **tudo** que estiver
> na pasta vai para dentro do workspace do agente. Um arquivo de anotação que
> cite o domínio ou nomeie o padrão entrega o gabarito.
>
> Em `harness/` só pode existir o que é tratamento. Hoje: `CLAUDE.md`.

## Estado

| | |
|---|---|
| `harness/CLAUDE.md` | escrito — 4 regras, 14 linhas |
| hook de compilação | adiado. Ver abaixo |
| skill | **descartada.** Ver abaixo |

Hash da árvore em 19/09/2026:
`05596ed4a47961daae1134c117e186a3bfb01c93836fb068b63c5abd2937ad00`

## Fonte de cada regra

A documentação do Claude Code dá respaldo à **engenharia** do harness — onde a
regra mora, como carrega, como se verifica. Ela não dá respaldo ao **texto** de
uma regra de projeto. Esse vem da literatura de padrões. Vale separar as duas
coisas na monografia.

| Regra | Fonte |
|---|---|
| levantar o que muda de caso para caso e o que é igual | Gamma et al., *Design Patterns* (1994) — encapsular o conceito que varia |
| o comportamento de cada caso num lugar só dele; selecionar não é sequência de condições | Fowler, *Refactoring* — Replace Conditional with Polymorphism; cheiro Repeated Switches |
| dar todo o contexto de que os casos precisam | Fowler, *Refactoring* — Introduce Parameter Object; cheiro Data Clumps |
| tratar assim apenas o que o enunciado descreve como variando | YAGNI; Fowler, *Refactoring* — cheiro Speculative Generality |

Decisões de forma, com fonte na documentação oficial:

- **Tamanho.** *"target under 200 lines per CLAUDE.md file. Longer files consume
  more context and reduce adherence"* — `code.claude.com/docs/en/memory`. O
  arquivo tem 14 linhas. Citar como orientação do fornecedor, nunca como
  resultado empírico: a Anthropic não publica dado, e o único estudo que testou
  tamanho não achou efeito entre 25 e 500 linhas.
- **Ênfase numa linha só.** *"add emphasis such as 'IMPORTANT' to that line
  alone. If you emphasize many lines, none of them stands out"* —
  `code.claude.com/docs/en/best-practices`.
- **O harness é conselho, não garantia.** *"Unlike CLAUDE.md instructions which
  are advisory, hooks are deterministic and guarantee the action happens"* —
  mesma página. Sem hook, a taxa de cumprimento faz parte do que se mede.

## Por que não tem skill

A ativação é escolha do modelo. Medição independente sobre ~1.000 prompts dá
recall de 46% a 67%, variando de 0,16 a 0,92 por skill. Ninguém mediu isso em
`claude -p`.

O problema decisivo não é a taxa. O remédio documentado para a skill disparar é
encher a `description` de palavras-chave que apareçam no prompt — que aqui
seriam exatamente os nomes do domínio. **Descrição genérica não dispara;
descrição eficaz é gabarito.** Não há meio-termo, então a skill sai.

Consequência a registrar: isso muda D2 no `plano.md`, que previa três camadas.

## O hook, quando for a hora

`PostToolUse` casando `Edit|Write` em `*.java`, rodando
`mvn -o -q -DskipTests compile` — compilação, não `verify`, para o custo não
dominar a run.

Três coisas documentadas que precisam de cuidado:

1. O hook deve gravar o próprio veredito em `/workspace/.harness/gate.jsonl`
   com horário, comando e código de saída, e o `extrair-meta.mjs` recolhe. Sem
   isso não há como provar que ele rodou.
2. Nenhum hook roda em SIGTERM, que é o que o `docker stop` manda. Toda run que
   você interromper na mão perde o veredito.
3. Um Stop hook é sobreposto em silêncio após 8 bloqueios seguidos, e nada na
   stream distingue "passou" de "o teto forçou o fim". O teto é configurável por
   `CLAUDE_CODE_STOP_HOOK_BLOCK_CAP`, e o script deve ler `stop_hook_active`.

Custo por edição não tem número publicado. A FUMACA-01 teve 23 `Write`+`Edit`;
no pior caso são 23 compilações. Como a H2 é sobre custo, medir antes de adotar.

## Regras de contaminação

- Nada que nomeie o domínio.
- Nada que enuncie o critério da rubrica de forma operacional.
- Nada que acrescente requisito funcional que o prompt não pede.
- Nada de arquitetura, testes ou banco — fura o recorte.
- Nada de bloco `permissions`: sob `-p` em pasta não confiada ele é ignorado, e
  mantê-lo no hash sugere um tratamento que não existe.

Antes de congelar: rodar a checagem de palavras proibidas e regravar o hash.

## Observação sobre a condição SEM

O `meta.json` da FUMACA-01, que rodou sem harness e com `~/.claude` vazio,
registra **18 skills embutidas, 51 slash commands e 5 agents** disponíveis —
entre elas `code-review`, `simplify`, `verify` e `debug`.

O braço `SEM` não é um ambiente vazio. O contraste honesto é "com o harness do
autor" contra "Claude Code como vem de fábrica". A redação de D1 no `plano.md`,
que diz "sem skills", precisa ser corrigida.

## Diário de versões do harness

| versão | hash | quando | o que mudou | runs produzidas |
|---|---|---|---|---|
| v1 | `05596ed4…2937ad00` | 19/09/2026 | quatro regras iniciais | FUMACA-01, MED-01-*-COM, MED-02-HAIKU-COM |
| v2 | `56057792…ea24b882` | 19/09/2026 | terceira regra trocada | MED-03-HAIKU-COM em diante |

**v1, regra 3:** "Dê a esse lugar todo o contexto de que os casos precisam, não
apenas o mínimo de que o primeiro deles precisa."

**v2, regra 3:** "Antes de escrever a primeira implementação, decida a
assinatura olhando todos os casos. Ela precisa atender o caso mais exigente,
não o primeiro."

Substituída, não acrescentada: as duas dizem a mesma coisa, e manter as duas
seria duas formulações da mesma regra. O arquivo continua com quatro regras.

**Motivo, medido nas runs de 19/09.** A dificuldade do Haiku não é entender o
pedido, é retrabalho por erro de compilação. Ele quebrou o build em cerca de
metade das tentativas (4 a 6 falhas por execução, quase todas de compilação),
contra zero do Opus com harness. O orquestrador foi reescrito 5 a 6 vezes, e as
implementações de pagamento 3 a 4.

Padrão: escreve as implementações antes de fechar o contrato entre elas,
descobre que não encaixam, volta. Foi o que produziu o `setFreteCalculado` com
`instanceof` na MED-01 — assinatura fixada no primeiro caso, remendo no quarto.

A v1 descrevia o resultado; a v2 descreve quando decidir. Para o Opus é inócua
(16 turnos, 1 build, 0 falhas — ele já faz isso); para o Haiku ataca a causa.

Custo do texto: o harness inteiro pesa de 0,6% a 1,1% da entrada total, contra
diferenças de comportamento de 42% a 82% entre os braços. Trocar uma regra não
move o custo de texto de forma perceptível.

---

# Diário de versões do enunciado

O nome do arquivo mudou; o hash é a identidade. Runs antigas só são
rastreáveis por esta tabela.

| hash | quando | arquivo | o que era | runs produzidas |
|---|---|---|---|---|
| `4e225fb7…44b97d5a` | até 19/09/2026 | `prompt.md` | com esqueleto, Maven offline, "não adicione bibliotecas novas" | FUMACA-01/02, MED-01 a MED-05 |
| `c0bba536…d568a955` | 19/09/2026 | `prompt.md` | com esqueleto, Maven online | nenhuma |
| `4edd89f9…8477c06` | 19/09/2026 | `prompt-sem-esqueleto.md` | pasta vazia | MED-06, MED-07 |
| `53db3424…841bfd124` | 20/09/2026 | `prompt.md` | pasta vazia + "Use Java 21 e Spring Boot 4.1.1" | nenhuma ainda |

O `prompt.md` com esqueleto (`c0bba536`) foi **apagado** em 20/09/2026, e o
`prompt-sem-esqueleto.md` tomou o nome `prompt.md`. As duas famílias com
esqueleto continuam recuperáveis pelo git.

---

## Onde ficam os eventos

Este arquivo guarda o **harness como artefato**: as quatro regras, a fonte de
cada uma, por que a skill saiu, o que o hook faria, e as tabelas de versão do
harness e do enunciado.

O que **aconteceu** — decisão tomada fora do previsto, execução descartada,
defeito de ferramenta encontrado — vai para `docs/diario-de-bordo.md`, que é o
que a §13.3 e a §13.4 do plano exigem. Os quatro registros datados que moravam
aqui foram para lá em 21/09/2026.

