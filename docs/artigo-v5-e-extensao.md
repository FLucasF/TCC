# Orientações de projeto e design patterns em agentes de programação: um estudo com pré-registro e sua extensão à manutenção

*Rascunho de 23/10/2026: o V5 como estudo principal e o TCC_V1 como extensão. Os marcadores `[CITAR: ...]` indicam
onde falta uma referência; nenhuma foi inventada. Os números vêm dos relatórios e do `DECISOES.md` de cada fase.*

## Resumo

Agentes de programação baseados em Grandes Modelos de Linguagem (LLMs) podem ser orientados por arquivos de projeto e
*skills*, o que aqui se chama *harness*. Este trabalho investiga como essas orientações mudam a forma como os agentes
aplicam *design patterns*. No estudo principal, com pré-registro, 11 modelos da família Claude construíram o mesmo
serviço de *checkout* em quatro níveis de orientação, com cinco réplicas (220 execuções). Um arquivo de princípios
levou o ponto mais difícil do padrão Strategy de 67% para 93% das execuções corretas, sem aumentar o exagero, sem
piorar a correção e com menos complexidade; o efeito veio dos seis modelos que não acertavam sozinhos. Uma extensão
exploratória, com cinco patterns, construção e manutenção, banco de dados e um verificador de qualidade calibrado (324
execuções), confirmou o resultado e mostrou que ele é mais forte quando o agente muda um código pronto (de 3 para 11
acertos em 15).

## 1. Introdução

Agentes de programação, como o Claude Code, recebem um pedido em linguagem natural e escrevem, testam e alteram o
código de um projeto sem intervenção humana [CITAR: trabalhos sobre agentes de programação autônomos]. Eles podem
receber orientações persistentes: um arquivo de projeto (o `CLAUDE.md`), *skills* que o agente abre durante a tarefa e
processos, como desenhar antes de codificar ou passar por um revisor [CITAR: documentação do Claude Code]. Neste
trabalho, *harness* é o conjunto portátil dessas orientações. O tema nasceu da construção de um *harness* de uso geral,
a partir de um levantamento de 72 repositórios públicos, que levou à pergunta de quais princípios de projeto um
*harness* deve transmitir.

A pergunta é se essas orientações levam o agente a decisões de projeto melhores. *Design patterns* servem bem a ela
porque têm um gabarito: são soluções conhecidas para variações que crescem [CITAR: Gamma et al., Design Patterns], e a
regra de três diz quando uma variação passa a justificar a estrutura [CITAR: Fowler, Refactoring; Kerievsky,
Refactoring to Patterns]. Também têm um risco simétrico: estrutura onde ela não se justifica, o exagero.

A pergunta geral é: **como as orientações de projeto dadas a um agente de programação mudam a forma como ele aplica
design patterns?** Ela se divide em dois grupos de perguntas:

- **Do estudo principal, com pré-registro:**
  - **P1.** Como o *harness* muda o uso do pattern pedido?
  - **P2.** Como ele muda o exagero e a correção?
  - **P3.** Como o efeito varia entre os modelos?
  - **P4.** Como cada componente do *harness* (o `CLAUDE.md`, a *skill*, o processo) contribui?
- **Da extensão, exploratórias:**
  - **P5.** Como o tipo de tarefa (construir ou manter) muda o efeito, em cinco patterns?
  - **P6.** Como as outras dimensões de qualidade (arquitetura, testes, banco, comportamento) respondem?
  - **P7.** Quão estáveis são os resultados quando a mesma tarefa se repete?

As contribuições são: (i) um experimento com pré-registro sobre o Strategy, com 11 modelos e quatro níveis de
orientação; (ii) uma extensão a cinco patterns, na construção e na manutenção, com banco de dados; (iii) um verificador
de qualidade em cinco dimensões, calibrado em casos de resposta conhecida; e (iv) a medida da estabilidade dos
resultados em cinco rodadas.

O restante do artigo está organizado assim: a Seção 2 apresenta os trabalhos relacionados; a Seção 3, o estudo
principal; a Seção 4, a extensão; a Seção 5 responde às perguntas; a Seção 6 traz as limitações; e a Seção 7 conclui.

## 2. Trabalhos relacionados

[CITAR: avaliações de código gerado por LLMs, como benchmarks de tarefas de programação e de correção funcional]

[CITAR: estudos sobre a qualidade de projeto e a manutenibilidade do código gerado por LLMs]

[CITAR: estudos sobre agentes de programação em repositórios reais e sobre o efeito de instruções e contexto
persistente no desempenho deles]

[CITAR: trabalhos sobre a detecção e a aplicação de design patterns, de forma automática ou com LLMs]

*O que este trabalho faz de diferente (a confirmar com as fontes):* mede o efeito de orientações persistentes, e não
do enunciado, sobre a decisão de aplicar um pattern que o texto pede sem nomear, com um controle negativo para o
exagero, com pré-registro e em muitos modelos; e estende a medida à manutenção de código pronto.

## 3. Estudo principal: o Strategy com pré-registro

### 3.1 Método

**A tarefa.** O agente recebe uma pasta vazia e o enunciado de um serviço de *checkout* de uma loja online, escrito na
voz do dono, que entende o básico de programação. O texto descreve as regras do negócio e, num anexo, o contrato da API
em linguagem simples, sem nenhuma palavra de arquitetura. Ele esconde quatro pontos que pedem o Strategy (frete, cupom,
forma de pagamento e o nível do clube de fidelidade, o mais difícil, chamado P4, em que um caso afeta três regras) e um
controle negativo (o seguro por região, em que só a porcentagem muda e o pattern seria exagero, chamado P5).

**Os níveis.** A escada acumula: **N0** sem *harness*; **N1** um `CLAUDE.md` com princípios gerais, sem nomear
patterns; **N2** o N1 com uma *skill* pública de patterns [CITAR: gof-patterns]; **N3** o N2 com um processo (um
desenho curto antes do código e um subagente revisor do mesmo modelo).

**Os modelos e as réplicas.** Os 11 modelos Claude disponíveis no Claude Code em 10/10/2026, menos um: Haiku 4.5 e 5.5,
Sonnet 4.5, 4.6, 5 e 5.5, Opus 4.6, 4.7, 4.8, 5 e 5.5. Cada modelo rodou os quatro níveis ao mesmo tempo, num quarteto
(para que horário e carga do servidor fossem iguais dentro do par), cinco vezes, em ordem sorteada: 220 execuções e 55
pares em cada comparação entre níveis vizinhos. O Claude Code rodou como vem, com esforço *medium*, num contêiner.

**As medidas.** A **correção** é uma suíte de aceitação pela API, com 21 casos (12 contas e 9 recusas), provada com
versões erradas de propósito (mutantes). O **desenho** é classificado pelo Semgrep (cada caso isolado; nenhuma condição
sobre o caso no cálculo), e a classificação foi conferida por um leitor humano, às cegas, em 44 pacotes. A
**qualidade** vem de métricas (complexidade cognitiva, linhas, classes, duplicação), e o **custo**, dos tokens e do
tempo.

**As regras de decisão.** Cada hipótese tem uma regra escrita antes de rodar: direcional (a maioria dos modelos fora do
teto melhora, e nenhum piora), de não-inferioridade (o saldo de pares piores não passa de 7 em 55) ou de alteração
(sem direção). O "teto" é o modelo que já acerta nas cinco réplicas sem *harness*: ali, a orientação só pode empatar ou
piorar.

### 3.2 Como o estudo foi protegido

Antes de medir, uma bancada com 62 execuções serviu para achar defeitos no enunciado, na suíte e na análise. Uma
primeira versão do lote (o V4) foi descartada antes de qualquer análise, porque o pré-registro dela separava os modelos
em dois lotes por um mal-entendido sobre quais entrariam nas conclusões; reescrevê-lo depois falsearia a data do
registro. O pré-registro do V5 foi congelado em 10/10, antes da primeira execução, com o *hash* de cada arquivo do
instrumento (122 arquivos) gravado no repositório. Uma análise completa de ensaio, feita sobre o V4, achou um defeito
real no Semgrep (projetos numa subpasta não eram lidos), corrigido antes do congelamento.

### 3.3 Resultados

**Tabela 1.** O estudo principal por nível (55 execuções em cada).

| medida | N0 | N1 | N2 | N3 |
|---|---|---|---|---|
| P4 certo | 67% | 93% | 93% | 98% |
| exagero no controle negativo (P5) | 0% | 2% | 5% | 0% |
| suíte de aceitação inteira (21 casos) | 71% | 84% | 80% | 87% |
| complexidade cognitiva (média) | 33,7 | 22,9 | 23,5 | 22,5 |
| linhas de código (média) | 519 | 584 | 614 | 571 |
| custo médio por execução (US$) | 1,32 | 1,52 | 1,47 | 1,98 |

A conferência humana concordou com o Semgrep em 174 de 176 respostas, e as quatro perguntas de desenho valeram para o
lote. Das cinco hipóteses principais (N1 contra N0), quatro foram apoiadas: o `CLAUDE.md` isola os casos do P4, não
piora a correção (saldo de -3 pares, dentro do limite de 7), reduz a complexidade e tem efeito maior nos modelos mais
fracos. A do exagero foi contrariada no sentido de que o *harness* não muda o exagero (1 par diferente em 55).

O efeito veio dos modelos fora do teto. Cinco modelos (Sonnet 5.5, Opus 4.6, 4.8, 5 e 5.5) acertavam o P4 nas cinco
réplicas sem orientação. Nos outros seis, o N1 teve 14 pares melhores e nenhum pior; o Opus 4.7 foi de 0 para 5 acertos
em 5, e o Haiku 4.5, o mais fraco, de 0 no N0 para 1 no N1 e 4 no N3. Na tendência de N0 a N3, o acerto do P4 subiu
(teste de Page, p = 0,007) e a complexidade caiu (p < 0,001). A *skill* (N2 contra N1) ficou inconclusiva: ela não foi
aberta espontaneamente em nenhuma das 110 execuções que a tinham. O processo (N3 contra N2) teve efeito apoiado no
desenho e no exagero. O lote custou US$ 346 em equivalente de API.

## 4. Extensão: cinco patterns, construção e manutenção

### 4.1 Por que estender, e o caminho até o desenho

O estudo principal deixou três pontos abertos: o teto (cinco dos 11 modelos não tinham o que melhorar), um pattern só e
a manutenção, que estava fora do recorte. Uma fase intermediária mediu cinco patterns (Strategy, Observer, Factory
Method, Decorator, Template Method) num domínio novo, uma pousada, na manutenção e na construção, com uma execução por
combinação e a correção feita por uma IA calibrada. Ela trouxe três mudanças para o desenho final. O *framework* passou
de Spring Boot para Javalin, porque o Spring entrega pronto parte do que se mede (eventos, contêiner de objetos,
aspectos). O princípio "o próximo caso num lugar só" foi lido como "num método só" no Decorator, e o `CLAUDE.md` ganhou
duas precisões. E a *skill* pública, quando o agente a abria passando uma pergunta como argumento, levou um modelo a
responder em texto e entregar a pasta vazia; ela foi trocada por uma *skill* escrita para o trabalho, com um processo de
decisão curto, uma referência por pattern e exemplos fora do domínio da pousada.

### 4.2 Método

O sistema é o de reservas da Pousada Maré Alta, em Java 21 com Javalin e os dados num PostgreSQL 18. Na **construção**,
o agente recebe uma pasta vazia e o enunciado do sistema inteiro, na voz do dono; os testes não são pedidos. Na
**manutenção**, recebe o sistema pronto, sem nenhum pattern, com o esquema em migrações e 26 testes, e um pedido de
mudança. Cada um dos cinco cenários pede um pattern sem dizer o nome. Os níveis são N0, N1 (o `CLAUDE.md`) e N2 (o
`CLAUDE.md` e a *skill*); os modelos, Claude Haiku 4.5, Sonnet 4.5 e Sonnet 5.5, com esforço *medium*, cada execução
com um banco só dela.

Cada projeto foi revisado por um agente (Claude Opus 5.5), às cegas, com uma *skill* e um *script* de medição por
dimensão: arquitetura (dependências e ciclos entre pacotes, pelo jdeps), design de baixo nível (a classe do pattern:
acerto, alternativa equivalente, malfeito ou omissão), testes (contagem e cobertura, pelo JaCoCo), banco (o esquema,
pelo `pg_dump`, e conferências simples) e comportamento (um roteiro de chamadas à API por cenário). Cada dimensão
recebe **bom**, **com problemas** ou **ruim**. Antes de valer, o verificador foi calibrado em 26 casos de resposta
conhecida (as soluções de referência, com e sem o pattern, e seis exemplos estragados de propósito): as 52 notas
esperadas conferiram. O piloto teve uma execução por combinação (90); o pattern que mais errou (Decorator) e o que mais
acertou (Factory Method) foram repetidos até cinco rodadas (mais 144).

### 4.3 Resultados

**Tabela 2.** Execuções boas por dimensão, tarefa e nível (piloto, 15 por nível; no pattern, acerto ou alternativa).

| dimensão | Construir N0 | N1 | N2 | Manter N0 | N1 | N2 |
|---|---|---|---|---|---|---|
| pattern | 9 | 11 | 12 | 3 | 6 | 11 |
| design do resto do código | 3 | 4 | 4 | 1 | 5 | 4 |
| arquitetura | 4 | 0 | 3 | 12 | 13 | 13 |
| testes | 2 | 3 | 2 | 7 | 8 | 4 |
| banco | 0 | 0 | 0 | 12 | 12 | 13 |
| comportamento | 5 | 5 | 5 | 13 | 12 | 9 |

**Tabela 3.** Execuções com o pattern certo em cinco rodadas.

| pattern | tarefa | N0 | N1 | N2 |
|---|---|---|---|---|
| Factory Method | construir | 12 de 15 | 13 de 15 | 14 de 15 |
| Factory Method | manter | 6 de 15 | 11 de 15 | 14 de 15 |
| Decorator | construir | 4 de 15 | 10 de 15 | 10 de 15 |
| Decorator | manter | 3 de 15 | 5 de 15 | 8 de 15 |

Construindo, nenhuma das 45 execuções do piloto usou migrações de esquema, e 15 deixaram o mesmo quarto ser reservado
duas vezes quando dois pedidos chegam juntos; mantendo, as 45 conservaram as migrações e a proteção da base. O Sonnet
5.5 teve o comportamento bom nas 30 execuções do piloto; o Haiku 4.5, construindo, não entregou um sistema funcional em
nenhuma das 15. Nas cinco rodadas, o resultado do piloto (pattern certo ou não) se repetiu em 119 das 144 rodadas novas
(83%).

## 5. Discussão

Cada resposta diz de onde vem a evidência: do estudo principal, com pré-registro, ou da extensão, exploratória.

**P1. Como o *harness* muda o uso do pattern?** *(com pré-registro, e confirmado na extensão)* Ele aumenta o uso
correto. No estudo principal, o ponto difícil do Strategy foi de 67% para 93% só com o `CLAUDE.md`. Na extensão, a
mesma direção aparece nos cinco patterns e nas duas tarefas: na manutenção, de 3 para 6 e 11 acertos em 15; nas cinco
rodadas do Factory Method mantido, de 6 para 14 em 15.

**P2. Como ele muda o exagero e a correção?** *(com pré-registro)* O exagero não muda: ficou entre 0% e 5% em todos os
níveis, e a extensão teve 5 estruturas desnecessárias em 90 execuções da rodada sem banco. A correção não piora, e a
complexidade cai; em troca, as linhas e as classes sobem. O código fica mais organizado e maior.

**P3. Como o efeito varia entre os modelos?** *(com pré-registro)* Ele se concentra nos modelos que não acertam
sozinhos: nenhum dos seis modelos fora do teto piorou, e o maior ganho foi de um modelo forte no resto, o Opus 4.7, o
que mostra que "fora do teto" não é o mesmo que "mais fraco". Na extensão, o modelo pesa mais que a orientação: o
Sonnet 5.5 sem orientação supera o Haiku 4.5 com a *skill* em quase todas as dimensões.

**P4. Como cada componente contribui?** *(com pré-registro, e matizado na extensão)* O `CLAUDE.md` faz a maior parte do
efeito. A *skill* pública não acrescentou nada mensurável, porque não foi aberta; o processo (N3) melhorou o desenho e
o exagero. Na extensão, com a instrução de usar a *skill*, ela ajudou nos patterns menos óbvios (a forma literal do
Decorator só apareceu com ela, e só no Sonnet 5.5, nas dez rodadas), e mostrou um risco: aberta com uma pergunta, levou
dois modelos a responder em texto e parar sem programar.

**P5. Como o tipo de tarefa muda o efeito?** *(exploratória)* O efeito é maior na manutenção. Diante de um código que
já funciona, o agente sem orientação tende a acrescentar mais uma condição (3 acertos em 15 no N0, contra 9 na
construção), e a orientação desfaz esse hábito.

**P6. Como as outras dimensões respondem?** *(exploratória)* Quase não mudam entre os níveis. Nelas, pesam o modelo e o
ponto de partida: mantendo, o agente conserva a arquitetura, as migrações e as restrições do banco que a base já tem;
construindo, nenhuma execução usou migrações. O *harness* fala de estrutura, e é ali que age.

**P7. Quão estáveis são os resultados?** *(exploratória)* O resultado de uma execução isolada se confirmou em 83% das
repetições. A variação concentra-se no nível sem orientação e no modelo mais fraco; com orientação, o Sonnet 5.5
repetiu o mesmo resultado em todas as combinações. Alguns erros são constantes: o Sonnet 5.5, mantendo sem orientação,
não acertou o Factory Method em nenhuma das cinco rodadas, e acertou nas dez com orientação.

**A trajetória do estudo.** O desenho mudou várias vezes, e cada mudança tem data e motivo registrados antes de ver os
resultados que ela afetaria. O lote descartado (o V4) foi descartado antes de qualquer análise; as mudanças da extensão
vieram de problemas de método (o teto, o *framework* que entregava o pattern pronto, a *skill* que travava o agente), e
não da direção dos resultados. E a conclusão principal, de que a orientação aumenta o pattern certo sem exagero, se
manteve em todas as fases, com enunciados, correções e modelos diferentes.

## 6. Limitações

**Do estudo principal.**
- Uma só família de modelos e um só agente: não se separa o efeito do *harness* do efeito do par Claude e Claude Code.
- Um só leitor humano conferiu o Semgrep, e a releitura prevista no pré-registro não foi feita (desvio registrado).
- O enunciado traz o contrato da API, o que dá realismo menor em troca de uma correção igual para todos.

**Da extensão.**
- As orientações (o `CLAUDE.md` e a *skill*) foram escritas por quem conhece as tarefas e o critério, o que tende a
  favorecer os níveis com orientação.
- A correção é feita por uma IA; foi calibrada (52 de 52), mas a nota resume uma leitura.
- Cinco rodadas só nos dois patterns repetidos; uma nos outros três.
- O ambiente não tinha Docker dentro do contêiner do agente, o que derrubou os testes de integração que dependiam dele.

## 7. Conclusão

O estudo principal, com pré-registro, mostra que um arquivo de princípios aumenta o uso correto do padrão Strategy
(de 67% para 93%), sem aumentar o exagero, sem piorar a correção e com menos complexidade, e que o efeito vem dos
modelos que não acertam sozinhos. A extensão mostra que o resultado vale para outros patterns, que é mais forte quando
o agente muda um código pronto, e que nas outras dimensões de qualidade o modelo e o ponto de partida pesam mais que a
orientação. Como trabalhos futuros, ficam as réplicas dos três cenários restantes, outras famílias de modelos e de
agentes, e um segundo leitor humano.

## Referências

[CITAR: Gamma, Helm, Johnson e Vlissides, *Design Patterns: Elements of Reusable Object-Oriented Software*]

[CITAR: Fowler, *Refactoring*]

[CITAR: Kerievsky, *Refactoring to Patterns*]

[CITAR: documentação do Claude Code (CLAUDE.md, skills e subagentes)]

[CITAR: a skill pública gof-patterns (grndlvl/software-patterns)]

[CITAR: as ferramentas de medição (Semgrep, SonarQube, CK, JaCoCo)]

[CITAR: os trabalhos relacionados da Seção 2]
