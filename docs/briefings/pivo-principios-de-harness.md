# Pivô de foco — de "o harness" para "os princípios de projeto de um harness"

**Escrito em 2026-09-06.** Três meses até a entrega. Este documento existe para
fechar o escopo, não para registrar decisões técnicas — para isso vale
[`decisoes-do-harness.md`](decisoes-do-harness.md), que continua sendo a fonte.

---

## 1. A ambiguidade que decide o TCC

A ideia foi enunciada como *"um harness generalista que vai guiar o modelo a boas
práticas de programação e a menos erros"*. Há dois trabalhos diferentes ali:

| | Objeto | Estado |
|---|---|---|
| **(a)** | Um harness cujo **conteúdo** são boas práticas de programação | Tentado e abandonado, com razão |
| **(b)** | As **boas práticas de construção** de um harness | Já escrito, não reconhecido como contribuição |

O (a) falha por um motivo já medido neste projeto: o modelo conhece SOLID, e
escrever "aplique SOLID" não muda a saída. O modo de falha observado é o oposto —
ele aplica cedo demais. Duas desistências anteriores (skills de boas práticas,
mecanismos de memória) foram exatamente isto, e o diagnóstico estava certo nas
duas vezes.

**"Boas práticas de programação" e "menos erros" não são o mesmo alvo.** O
segundo é o alvo válido. O caminho até ele não passa por mais texto de prática —
passa pela estratificação por quem garante, que já está construída.

O (b) é o objeto deste TCC.

---

## 2. A pergunta de pesquisa

> Que princípios de projeto governam a construção de um harness que reduz erros de
> um agente de codificação — e quais desses princípios são independentes da
> linguagem do projeto e da capacidade do modelo?

A segunda metade é o que dá defensabilidade. Ela responde à pergunta que a banca
faz e que hoje não tem resposta: *"como você sabe que isso continua valendo no
próximo modelo?"*

A resposta é a classificação. Todo princípio cai em uma de duas classes:

**Classe I — dependente de capacidade.** Existe porque o modelo, hoje, erra
naquilo. Um modelo melhor o dissolve. É o caso da maior parte de um guia de
práticas.

**Classe II — estrutural.** Não é déficit de conhecimento, então capacidade não
fecha:

| Invariante | Por que capacidade não resolve |
|---|---|
| Probabilístico vs. garantido | Um modelo melhor acerta com mais frequência. Frequência não é garantia |
| Não-observável | `Food.normalizeToSearch` invisível ao módulo `patient` é fato do repositório, não do modelo |
| Espaço de ação | Saber que não deve não remove a ferramenta da mão |
| Estado através da sessão | A sessão acaba e o contexto compacta. É arquitetura, não capacidade |

A definição subtrativa já em uso — *"se o modelo já faz sozinho, não entra"* — é
o operador que separa as duas classes. O resíduo da subtração **é** a Classe II.
O critério já existe; falta aplicá-lo com evidência em vez de intuição.

---

## 3. O catálogo

Doze princípios extraídos do que já está registrado. Nível de evidência:
**M** = medido com número · **V** = verificado em fonte ou código vigente · **H** =
verificado em código que existiu e foi removido, hoje no histórico do git · **R** =
raciocínio, ainda sem evidência.

| # | Princípio | Classe | Evidência hoje | O que falta |
|---|---|---|---|---|
| 1 | Definição subtrativa: se o modelo já faz, não entra | — | R + o caso SOLID | Contagem: quantos dos 72 harnesses violam isso |
| 2 | Organizar por quem garante, não por assunto | II | V — duas camadas hoje, três até 06/09 | Quantos dos 72 organizam por assunto |
| 3 | Prática só desce de camada quando a de cima não a expressa | II | V (o caso do timeout, em prosa por falta de linter) | Um caso de descida executado — Semgrep |
| 4 | Teto declarado e verificado como teste | I | **H** (`rules.test.mjs`); hoje o teto é intenção, conferido à mão | Reinstalar o teste, se a camada voltar |
| 5 | Unidade de contagem definida (regra = 1 parágrafo, 1 imperativo) | — | V | — |
| 6 | Manifesto que não conhece a linguagem | II | **H** (`validation.json`) | **Prova em 2ª stack** — ver §4 |
| 7 | Ambiente ausente é `BLOCKED`, nunca `FAIL` | II | **H** (`prerequisites`) | Um caso observado de "conserto" indevido |
| 8 | Trabalho global é da ferramenta; local é do modelo | II | **M** (jscpd: ~30 tokens filtrado vs. 5–20k explorando) | O filtro exigia executor; sem ele o número não se reproduz |
| 9 | O gatilho precisa ser observável pelo agente | II | R, com o contraexemplo circular nomeado | — |
| 10 | Perguntar tem curva de custo | I | R | — |
| 11 | Falhar aberto, e dizer em vez de silenciar | II | **H** (portão desistia em 4, teto do Claude Code é 8) | — |
| 12 | Adiar com o sinal nomeado que traz de volta | — | V (tabela de adiados) | — |

**O princípio 8 é o mais forte que o projeto tem.** É o único com medição de
ponta a ponta, e é o que melhor exemplifica a Classe II: a regra em prosa
*"procure antes de criar"* falha porque o modelo busca pelo nome errado; a
ferramenta acha. Não é questão de o modelo ser melhor.

**Os achados negativos entram como resultado, não como desistência.** "Skills
genéricas de boas práticas não alteram a saída porque o modelo já as segue" e
"mecanismos próprios de memória duplicam o que a plataforma faz — o que falta é
governança, não escrita" são duas linhas de resultado sob o princípio 1. A
evidência da segunda já existe: a nota escrita pelo modelo em 05/09 que mandava
ignorar crítica acadêmica e contradisse o trabalho do dia seguinte.

---

### O que a remoção do executor faz com este catálogo

Em 06/09, depois de escrito o que está acima, a camada de verificação do harness foi
construída e removida no mesmo dia. Cinco princípios — 2, 4, 6, 7 e 11 — tinham como
evidência justamente aquele código, e por isso mudaram de **V** para **H**.

Isso não os torna falsos, e vale ser preciso sobre o porquê: um princípio de projeto
se sustenta pelo que foi observado ao construir, não por o artefato continuar de pé.
O código está no histórico do branch `executor-em-node` e é inspecionável. Para um
trabalho cuja contribuição são princípios, **artefato construído, medido e cortado é
evidência mais forte que artefato mantido por precaução** — porque inclui o custo da
decisão de cortar.

O que a remoção de fato tira é a instanciação vigente. A consequência prática é a
§4: a prova de genericidade na segunda stack agora exige reinstalar a camada antes,
ou ser reescrita como comparação de manifestos e não de execuções.

E ela acrescenta um princípio que não estava na lista, porque só aparece quando se
corta algo:

> **13. A linguagem do executor é derivada dos pré-requisitos que o projeto já
> declara, não escolhida de antemão.** Corolário, e é onde a genericidade termina: o
> manifesto pode ser agnóstico porque não sabe o que é um teste; o executor não pode,
> porque é código e código tem linguagem. A única saída completa é distribuir binário
> em vez de fonte, trocando genericidade por opacidade.

---

## 4. "Generalista" é uma afirmação que precisa de prova

Hoje a genericidade é **de projeto**, não demonstrada: o manifesto roda o comando
declarado e `mvn test`, `pytest`, `go test` são indistinguíveis para ele. Isso é o
mecanismo correto, mas é argumento, não evidência.

A prova é barata: instanciar o harness numa **segunda stack** que não compartilhe
nada com a primeira — um projeto pequeno em Go ou Python, algumas centenas de
linhas, escrito para isso. O que se mede é o custo de adoção:

- quantas linhas de `validation.json` a segunda stack exigiu
- quantos dos 12 princípios sobreviveram sem alteração
- quantos exigiram caso particular — **e cada caso particular é um furo na
  afirmação de genericidade, a ser reportado como tal**

Sem isso, "generalista" é asserção. Com isso, vira o capítulo de validação, e é o
que separa este trabalho dos 72 levantados.

---

## 5. Plano de 12 semanas

| Semanas | O quê | Entrega |
|---|---|---|
| 1–2 | Fechar o catálogo: forma de cada princípio (enunciado, quando se aplica, custo, classe, evidência). Definir o critério de classificação I/II | Codebook |
| 3–5 | Segunda stack. Instanciar, medir custo de adoção, registrar cada caso particular | Capítulo de validação |
| 6–8 | Fechar as lacunas da tabela §3: contagem contra os 72 para os princípios 1 e 2; uma descida de regra via Semgrep para o princípio 3 | Evidência |
| 9–10 | Rodar tarefas reais no NutriPlan registrando falhas, para dar base observada aos princípios ainda marcados **R** | Dados |
| 11–12 | Escrita | Texto |

A folga está nas semanas 9–10: se atrasar, elas encolhem e os princípios **R**
ficam declarados como raciocínio, o que é honesto e não invalida o resto.

---

## 6. O que sai do centro

| Sai | Vira |
|---|---|
| A varredura dos 72 repositórios | Trabalhos relacionados. Trabalho excelente, e não é contribuição — mas alimenta as contagens dos princípios 1 e 2 |
| Decisões de arquitetura do executor | Apêndice, com o código no histórico do branch `executor-em-node` |
| O harness como produto | Instrumento e instanciação dos princípios |
| Ablação regra a regra das 10 + 5 skills | **Cortado.** Escopo demais para 3 meses sozinho, e "não mudou nada" ×12 não sustenta um capítulo |

---

## 7. Riscos

**O catálogo virar lista de opiniões.** É o risco principal. Mitigação: a coluna
de nível de evidência é obrigatória e aparece no texto final. Um princípio **R**
é publicável desde que apareça como **R**.

**A segunda stack consumir mais que três semanas.** Mitigação: o projeto é
escrito para o teste, não encontrado. Poucas centenas de linhas, com suíte que
roda em segundos.

**A classificação I/II ser circular.** Dizer "é estrutural porque capacidade não
resolve" sem critério independente é petição de princípio. Mitigação: o critério
tem que ser enunciado antes de classificar — na semana 1–2, não depois de olhar
os dados.
