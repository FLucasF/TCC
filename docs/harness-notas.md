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

## 20/09/2026 — esqueleto removido, e versões pedidas no enunciado

**Antes:** `experimento/skeleton/` era copiado para o workspace: `pom.xml` com
Spring Boot 4.1.1 e Java 21, mais `CheckoutApplication.java` em
`br.tcc.checkout` e um `application.properties`.

**Agora:** o workspace nasce vazio. O enunciado ganhou a linha "Use Java 21 e
Spring Boot 4.1.1" em "Observações do time técnico".

**O que isso custa, e precisa estar no pré-registro.** Virou pedido, não
garantia. Nas quatro execuções sem esqueleto já coletadas, dois pares
divergiram de base **dentro do mesmo modelo**: MED-06 Haiku escolheu Java 11 no
braço COM e Java 17 no SEM — e o COM não compilou, porque Spring Boot 3.x não
roda em Java 11 — e MED-07 Haiku escolheu 3.1.0 contra 3.1.5. A linha no
enunciado existe para fechar isso, e a taxa de obediência passa a ser um dado:
`fundacao.spring_boot` e `fundacao.java` no `meta.json`.

**A imagem virou `experimento-harness:v3`.** O Dockerfile mudou, e a regra da
bancada é que linha alterada no Dockerfile significa tag nova. `executar.sh`,
`conferir-exemplos.sh` e o README já apontam para a v3; enquanto ela não for
construída, o preflight do `executar.sh` falha com mensagem clara em vez de
rodar na imagem errada.

**O aquecimento do `~/.m2` mudou de casa.** Saiu de `experimento/skeleton/` para
`infra/docker/aquecimento/`, que agora é um projeto Spring Boot próprio e
mínimo. As versões dele têm que acompanhar as do enunciado: se divergirem, o
cache esquenta o que ninguém usa, e só paga download quem **obedecer** — o que
seria vantagem de tempo para quem desobedece. Isso já aconteceu de fato: no
MED-07, com o cache em 4.1.1, o Opus escolheu 4.1.1 e baixou nada, enquanto o
Haiku escolheu 3.1.5 e registrou 46 `Downloaded from` na transcrição.

## 20/09/2026 — web liberada nas duas condições

**Antes:** `--disallowedTools "WebSearch,WebFetch,Agent,Task"`. O D10 bloqueava
web para impedir ajuda externa não controlada, e o §9.4 escolhia entre proxy e
auditoria.

**Agora:** `--disallowedTools "Agent,Task"`. Subagente segue bloqueado, porque
aquilo é controle de troca de modelo, não de acesso à internet.

**O que isso custa, e precisa estar no pré-registro.** Ganha validade externa:
quem usa o Claude Code no dia a dia tem web. Perde controle interno, e o risco
é específico desta tarefa: frete por modalidade, desconto por cupom e ajuste por
forma de pagamento são os três exemplos canônicos com que o padrão Strategy é
ensinado. Com busca liberada, os dois braços podem convergir por terem lido o
mesmo tutorial, e o contraste que o experimento mede encolhe. Somado a isso,
resultado de busca muda de um dia para o outro, o que é variação não controlada
entre repetições, cara com n=3.

Consequência para a auditoria: `auditoria.acesso_web_suspeito` deixa de ser
marca de violação e passa a ser registro descritivo. Para as 24 execuções de
medição, que rodaram com web bloqueada, ela continua sendo prova de isolamento.

## 20/09/2026 — a suíte escondida rodou de verdade pela primeira vez

Com a imagem `v3`, os 60 casos contra as seis execuções do `MED-07`. São
aplicações Java reais, não app de mentira.

| execução | |
|---|---|
| `MED-07-VAZIO-OPUS-COM` | 60/60 |
| `MED-07-VAZIO-OPUS-SEM` | 60/60 |
| `MED-07-VAZIO-SONNET-COM` | 60/60 |
| `MED-07-VAZIO-SONNET-SEM` | 60/60 |
| `MED-07-VAZIO-HAIKU-COM` | **57/60** |
| `MED-07-VAZIO-HAIKU-SEM` | **59/60** |

### O que isso diz sobre o instrumento

Quatro de seis passam tudo. **Correção funcional satura**, que é exatamente a
premissa do experimento: o que separa os modelos aqui não é se a conta fecha, é
como o código está organizado. A suíte escondida é controle, não o desfecho.

Mas ela **não é inútil**: pegou dois defeitos reais que os quatro exemplos do
enunciado não pegavam.

### Defeito 1 — Haiku COM: a parcela sai da base errada

Três casos, um único bug.

| caso | total do pedido | esperado | veio |
|---|---|---|---|
| `exemplo-2` | 425,30 | 75,90 | 81,27 |
| `pag-cartao-4x` | 200,00 | 52,51 | 55,15 |
| `pag-cartao-12x` | 200,00 | 18,90 | 21,43 |

A fórmula Price está escrita **corretamente** no código. Errada é a base: ele
calcula a parcela sobre o total **já com juros**, aplicando a fórmula na própria
saída dela. Confere exato — `price(455,40; 1,99%; 6) = 81,27`,
`price(210,04; 1,99%; 4) = 55,15`, `price(226,80; 1,99%; 12) = 21,43`.

Duas coisas valem registrar:

**Ele erra o `exemplo-2`, que estava no enunciado.** O modelo recebeu o caso
resolvido, com "6× de 75,90" escrito, e entregou 81,27.

**O `totalFinal` está certo.** Só o `valorParcela` diverge. Uma suíte que
comparasse apenas o total aprovaria essa aplicação — que é a regra escrita no
`PENDENTE.md` depois das dez execuções de 19/09, "conferir campo a campo, nunca
só o totalFinal", se provando sozinha num caso que ninguém tinha rodado.

### Defeito 2 — Haiku SEM: o limite do boleto nunca foi implementado

`pag-boleto-100001` devolve HTTP 200 onde deveria devolver 400 com
`FORMA_PAGAMENTO_INDISPONIVEL`. O código entrega o motivo por escrito, em
comentário que o próprio modelo deixou:

```java
// Adiciona frete (precisa saber qual modalidade... mas não temos aqui)
// Vamos pegar do request original que está sendo passado
// Na verdade o erro de boleto só aparece se total > 1000
// Vou verificar na chamada do service
```

O "vou verificar na chamada do service" nunca aconteceu. É pensamento em voz
alta deixado no código de produção, e matéria-prima para a análise qualitativa.

### O oitavo defeito de ferramenta, achado no caminho

`cygpath -w` sobre caminho **relativo** devolve caminho relativo, o Docker
recusa o bind mount com "is not a valid Windows path" e sai com código 125 — e
o `conferir-exemplos.sh` somava o 125 como "125 casos com erro". O exemplo
relativo documentado no `README.md` nunca teria funcionado.

Corrigido em duas frentes, porque o defeito tem duas causas:

1. `CASOS` passa a ser resolvido para caminho absoluto.
2. Os códigos de erro do próprio Docker (125, 126, 127) deixam de ser somados
   como contagem de casos, e viram uma coluna própria no resumo. É a mesma
   família do 66, que já tinha dado esse problema em 19/09: **nem todo código de
   saída é contagem**.

E o stderr do container deixou de ser descartado: vai para
`runs/logs/conferir-<run_id>.err`. Sem isso, este defeito continuaria invisível
— só se via o número 125 sem nenhuma explicação.

### Junto veio uma melhoria

`CASOS` aceita **pasta**, e aí todos os `.json` de dentro rodam contra a mesma
subida da aplicação. Seis grupos num boot, em vez de seis boots:

```bash
CASOS=avaliacao/casos avaliacao/ferramentas/conferir-exemplos.sh <run_id>
```
