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

# Diário de decisões da bancada

Mudanças que não são do harness, mas afetam a comparabilidade das execuções.

## 19/09/2026 — Maven passa a ser online (revisão de D17)

**Antes:** `settings.xml` com `<offline>true</offline>` na imagem. Dependência
nova era impossível; o build falhava.

**Agora:** Maven online. O agente acrescenta biblioteca se julgar necessário, e
o `meta.json` registra em `dependencias`: hash do `pom.xml` que entrou, hash do
que sobrou, e a lista de acrescentadas e removidas.

**Por quê.** O offline estava carregando três justificativas e só uma se
sustentava. Deriva de versão já é impedida pelo `spring-boot-starter-parent:4.1.1`
do esqueleto, que trava a versão de tudo — as dependências nem declaram versão.
Tempo de download continua fora da medição por causa do `~/.m2` aquecido. Sobrava
"impedir dependência nova", que é real, mas impedir apaga o dado: nunca se
descobre se o modelo obedeceria à instrução.

**O que isso custa, e precisa estar no pré-registro:**

- O arquivo pode envelhecer. Uma app que acrescentou dependência só recompila
  se o artefato ainda existir no repositório.
- A comparação estrutural fica frágil. Anotação que gera código — Lombok é o
  caso óbvio — desloca arquivos, linhas e métodos sem que o design tenha mudado.
  Decidir **antes do lote** se `dependencias.acrescentadas` vira covariável ou
  critério de exclusão. Depois de ver o resultado, não vale.

**O que mudou junto:**

| | antes | depois |
|---|---|---|
| imagem | `experimento-harness:v1` | `experimento-harness:v2` |
| `settings.xml` | `<offline>true</offline>` | removido da imagem |
| build pós-execução | `mvn -o -B verify` | `mvn -B verify` |
| ferramentas de avaliação | `mvn -o` | `mvn` |
| enunciado | "Não adicione bibliotecas novas ao projeto." | linha removida |
| enunciado | "funcionando com `mvn -o verify`" | "funcionando com `mvn verify`" |
| `hash_prompt` | `4e225fb7…44b97d5a` | `c0bba536…d568a955` |

A linha do enunciado foi **removida**, não trocada por permissão explícita. Um
"pode adicionar se precisar" é convite e elevaria a taxa acima do que o modelo
faria por conta própria; silêncio é o que mede "se ele achar necessário".

A `v1` continua no disco com o mesmo digest (`sha256:2bd0792b…`), então as dez
execuções de 19/09 seguem reproduzíveis. Elas usaram o enunciado antigo e o
Maven offline: **não são comparáveis com o que vier depois.**
