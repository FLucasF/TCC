---
tags: [tcc, experimento, diario]
---

# Diário de bordo

Tudo que aconteceu **fora do previsto**: decisão tomada no meio do caminho,
execução descartada, defeito de ferramenta encontrado.

Exigido pelas §13.3 e §13.4 do plano, e referenciado pela §12.3 e pela §17.
Criado em 21/09/2026; até então o conteúdo morava dentro de
`docs/harness-notas.md`, em duas seções, o que é exatamente o tipo de coisa que
some quando alguém vai procurar.

> [!important] A divisão com o `harness-notas.md`
> Aqui ficam **eventos**, em ordem cronológica. Lá fica o **harness como
> artefato**: as quatro regras, a fonte de cada uma, e as tabelas de versão do
> harness e do enunciado, que são provenência e não acontecimento.

## Formato, durante o lote

Uma entrada por exceção, conforme a §13.4:

```markdown
### 2026-09-22 14:25, LOTE-03-HAIKU-COM
- Situação: cota atingida no turno 14
- Ação: execução descartada; refeita às 19:10, mesma posição
- Impacto: nenhum na configuração
```

> [!warning] Interromper à mão é julgamento seu, e entra no experimento
> Não existe limite automático de tempo nem de turnos. Se um dos braços for
> sistematicamente mais lento e você matar mais execuções dele, estará
> descartando justamente os casos difíceis daquele braço, e a comparação fica
> enviesada a favor dele. Registre **toda** interrupção com o tempo decorrido e
> o motivo, e use o mesmo critério de paciência nos dois braços do par — que
> rodam ao mesmo tempo justamente para isso ser comparável.

---

# Antes do lote


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

## 20/09/2026 — a bancada passou a ser versionada

**Antes:** não era repositório Git. Os cinco defeitos de ferramenta de 19/09
foram corrigidos editando arquivo por cima, sem rede.

**Agora:** `git init`, e `runs/` entra no versionamento **menos** `target/`.
Medido: `target/` são 483 MB dos 499 MB da pasta, e todo o resto são 11 MB —
que contêm os 469 `.java` que a rubrica pontua e as 24 transcrições que provam o
isolamento. O `.gitignore` anterior descartava exatamente isso.

`.gitattributes` com `* -text`: os hashes gravados nos `meta.json` e no
`harness-notas.md` são sha256 dos bytes em disco, e normalização de fim de linha
no checkout quebraria a reprodutibilidade das execuções já coletadas.

**Fora do versionamento, por decisão:** `runs/MED-03-HAIKU-SEM/workspace/`. O
agente dessa execução rodou `git init` no próprio workspace e fez 1 commit. O
repositório aninhado fica intacto no disco, sem edição da run arquivada; o custo
é que os 13 `.java` dela não têm cópia versionada.

## 20/09/2026 — sexto e sétimo defeitos de ferramenta

**O comparador não sabia expressar caso de erro.** Dois defeitos somados: todo
status diferente de 200 era contado como falha, e `igual()` caía no ramo
numérico para string, onde `Number("PEDIDO_INVALIDO")` vira `NaN` e nunca é
igual a nada.

**O que isso impedia:** a precedência dos oito códigos de erro — prioridade 1 do
`rotas-descobertas.md`, cobertura zero até então — era **impossível de testar**.
Um 400 devolvido corretamente era reprovado.

**Corrigido:** o caso ganhou `status_esperado`, opcional e valendo 200, então os
dois arquivos de caso que já existiam seguem válidos sem edição. A resposta
passou a ser lida como texto antes de decodificar, porque corpo vazio ou
não-JSON aparece justamente nas respostas de erro.

**Sétimo, achado ao testar o conserto:** `process.exit()` derruba o processo no
Windows enquanto o undici ainda fecha sockets, e o código de saída vira
`0xC0000409` em vez do número de falhas. Trocado por `process.exitCode`.

**Consequência de processo:** nasceu o `avaliacao/ferramentas/autoteste.mjs`,
com cinco apps de mentira com defeito conhecido. É a resposta ao A4 do
`o-que-falta.md` — "nenhum teste testa o testador" — e passou a ser regra: toda
ferramenta de medida ganha teste próprio, com caso de regressão para cada
defeito real encontrado.

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

## 20/09/2026 — quatro decisões fechadas antes do pré-registro

Todas tomadas **antes** de qualquer pacote do lote ser pontuado.

**P6 — o que conta como Strategy.** A varredura das 24 execuções mostrou que a
pergunta era mais larga do que parecia: não são duas formas, são seis. `enum`
com corpo por constante conta, com 2 em C1/C2/C3 e **1 em C5**, porque variante
nova exige editar o próprio `enum`. Tabela de dados com caso especial por
identidade fica em C1=1 e C3=1. `switch` com a lógica dentro é C1=0. Detalhe na
§14.4a do plano e na `rubrica-strategy.md`.

**H2 — virou não-direcional.** A justificativa escrita ("há skill carregada e
hook de verificação") deixou de existir quando os dois saíram do harness, e a
direção está contradita pelas próprias medições: Opus 874.604 → 507.169 tokens
de entrada (−42%) e Sonnet 2.056.553 → 1.134.373 (−45%), `SEM` contra `COM`.
É n=1 em cada, então a hipótese não assume o contrário: declara o mecanismo
plausível à parte.

**D8 — `effort` passou de `high` para `medium`.** Duas razões, nenhuma olhando
desfecho: a calibração de custo inteira vale para `medium`, e `medium` compra
mais repetições, que é onde o estudo é mais fraco. A distinção entre mudar por
**custo** e mudar por **resultado** ficou escrita na §3.1, porque é ela que
separa isso de escolher a régua depois.

**§13.2 — o paralelismo fica.** A regra proibia o que `par.sh` e `rodada.sh`
fazem de propósito. Numa comparação pareada o que importa é os dois braços
enfrentarem as mesmas condições, e a disputa de CPU é simétrica dentro do par.
O custo vai declarado: duração **entre modelos** fica contaminada, e a medida
reportada é `duracao_api_ms`.

## 20/09/2026 — os instrumentos que faltavam

O `o-que-falta.md` abria com "o desfecho primário não tem instrumento nenhum".
Deixou de ser verdade.

**Rubrica** (`avaliacao/rubrica-strategy.md`): critérios instanciados por ponto,
catálogo das seis formas observadas, e âncoras de código real em C1, C2, C3 e
C5, todas conferidas contra o arquivo da run. Duas lacunas ficaram **registradas
em vez de disfarçadas**: C2=1 não tem âncora, e as três extensões não separam
"parametrizado" de "uma classe por variante".

**Testes de extensão** (`avaliacao/testes-extensao/`): 11 casos, e a contagem do
C5 virou mecânica por `git diff --numstat`.

**Suíte escondida**: de 4 casos para **60**, nos quatro grupos da §14.3. Os
valores não foram digitados — saem do `gerar-casos.mjs` em BigInt, e o gerador
aborta se não reproduzir os quatro exemplos do enunciado e E5/E6. A trava foi
testada adulterando a taxa do cartão: acusou e não escreveu.

**Anonimização** (`anonimizar.mjs`): tira os rastros da condição, normaliza as
datas de modificação, embaralha a ordem antes de atribuir o código cego, e conta
as pistas que o próprio modelo deixou no código. Ensaiado nas seis execuções do
MED-07: zero pistas.

**Detector de acesso externo**: ganhou módulo próprio e 13 casos de teste, e os
24 `meta.json` foram reauditados — duas execuções de Opus deixaram de ser
acusadas, 22 ficaram idênticas.

**Campo `repeticao`**: quarto argumento do `executar.sh`, validado. Com n=3 por
célula, sem ele não dá para dizer qual das três é cada run do lote.

## 20/09/2026 — P7, `valida`, e o pré-registro

**P7 — desobediência às versões pedidas.** Taxa reportada: a execução continua
válida e a desobediência vira dado, em `fundacao.obedeceu_versoes`. Consistente
com a §13.3, que já conta build quebrado como resultado.

**`valida`** deixou de depender da memória. O extrator grava `valida_proposta` e
`motivo_proposta`; o campo continua sendo decisão humana, e divergir da proposta
exige motivo escrito. Estava `null` nas 24 execuções porque ninguém tinha
definido quem marca.

**`docs/pre-registro.md`** fixa os hashes de prompt, harness, Dockerfile,
rubrica, gabarito e dos 11 arquivos de caso, mais o digest da imagem `v3`. E
declara o que **não** está pré-especificado, para não virar descoberta
disfarçada depois.

A tag `v1-congelado` **ainda não foi criada** — decisão adiada em 20/09.

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

## 21/09/2026 — agregador e análise, sem p-valor

**`agregar.mjs`**: 44 colunas, uma linha por execução. Até aqui toda tabela
desta bancada saiu de `node -e` improvisado.

**`analisar.mjs`**: as tabelas da §15 em Markdown — e **sem Mann-Whitney**, que
era o que o C3 pedia. A razão é aritmética: com n=3 por braço existem C(6,3)=20
arranjos possíveis, então o menor p bicaudal alcançável é **0,10**. p<0,05 é
impossível mesmo com separação perfeita, e publicar o número só induziria a ler
um "não significativo" que vem do desenho e não dos dados.

Juntar os três modelos daria 9 por braço e tornaria o teste possível, mas
ignoraria o fator de bloco — e a H3 é exatamente sobre o efeito diferir por
modelo.

No lugar: mediana e faixa, e medida de efeito por pares.

**Os testes funcionais viraram dado.** O comparador emite uma linha por grupo e o
`conferir-exemplos.sh` recolhe em `analise/funcional.csv`. Antes o resultado por
grupo só existia como texto na tela, e a tabela 15.1c não teria de onde sair.

## 21/09/2026 — FUMACA-03: a bancada rodou inteira depois da refatoração

Rodada de fumaça com a configuração nova: imagem `v3`, workspace vazio,
enunciado com a linha das versões, web liberada, `effort medium`. Seis
execuções em paralelo, **607s, zero falhas**, `build_ok` nas seis.

Fora da análise, por ser `FUMACA-`.

### O que se confirmou

**Isolamento**, pelo evento inicial das seis: `WebSearch` e `WebFetch`
**disponíveis**, `Agent` **bloqueado**, `~/.claude` do container vazio, nenhum
`CLAUDE.md` fora do workspace, e o harness presente só nas três `COM`.

**A linha das versões funcionou.** As seis escolheram Spring Boot 4.1.1 e Java
21. Antes dela, sem esqueleto, o `MED-06` e o `MED-07` produziram 3.1.0, 3.1.5,
3.2.0 e 3.3.4, com Java 11, 17 e 21 — e o Haiku chegou a escolher Java 11 sob
Spring Boot 3.x, que nem compila. Travar por texto em vez de por esqueleto se
sustentou.

**A web liberada não mudou o comportamento.** `chamadas_web: 0` nas seis, com as
ferramentas disponíveis. A preocupação de que a busca pudesse fazer os dois
braços convergirem por terem lido o mesmo tutorial de Strategy não se
materializou aqui. É n=1 por célula; vale acompanhar no lote.

### Nono defeito de ferramenta, e este era meu

Duas execuções saíram com `valida_proposta: false`, motivo "outro modelo
observado: `claude-haiku-4-5-20251001`". Isso é o **snapshot datado do mesmo
modelo**: o Haiku reporta o id datado nas mensagens, Opus 5 e Sonnet 5 reportam
o id simples. A comparação estrita teria mandado descartar duas execuções boas —
que é o erro mais caro que este campo pode cometer.

A lógica saiu para `infra/scripts/validade.mjs`, com 14 casos em
`validade.teste.mjs`, incluindo o limite que importa: `claude-opus-5-1` **não** é
`claude-opus-5`, então um `startsWith` ingênuo não serviria. Só o sufixo de data
de oito dígitos é removido.

O `reauditar-web.mjs` virou `reauditar.mjs` e passou a recalcular todos os campos
derivados, não só a auditoria de rede.

### A suíte, e a regra de conferir campo a campo se provando de novo

| execução | |
|---|---|
| Opus COM / SEM | 60/60 |
| Sonnet COM / SEM | 60/60 |
| Haiku COM | **57/60** |
| Haiku SEM | **53/60** |

As sete falhas do **Haiku SEM** têm uma propriedade que vale registrar:
**`totalFinal` está correto em todas as sete**. Uma suíte que comparasse só o
total teria dado 60/60 para essa aplicação.

Quatro delas são o `FRETEGRATIS` **exatamente como o `rotas-descobertas.md`
previa**, no defeito que ele chama de "apresentação": a app zera o campo `frete`
em vez de lançar o desconto. Como o total é `produtos − 0 + 0`, ele fecha certo,
e o resumo contraria o contrato. Era o defeito medido em 4 de 10 execuções em
19/09, reproduzindo e sendo pego.

As outras três são a parcela do cartão com juros, com **os mesmos números** do
`MED-07-VAZIO-HAIKU-COM`: 81,27 · 55,15 · 21,43. O mesmo bug reproduzindo em
outra execução e no outro braço.

O **Haiku COM** falha nos mesmos três casos de cartão, mas com valores
diferentes — 454,92 em vez de 455,40 — e ali o `totalFinal` também diverge. É
uma terceira variante do mesmo ponto, o P3, que é o ponto difícil por desenho.

