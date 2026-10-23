# Orientações de projeto e design patterns em agentes de programação: da construção à manutenção

*Rascunho de 23/10/2026, sobre o experimento inteiro em `J:\TCC` (de setembro a outubro de 2026). O TCC_V1 é o
resultado final; as fases anteriores aparecem como o caminho que levou a ele. Os marcadores `[CITAR: ...]` indicam
onde falta uma referência; nenhuma foi inventada. Os números vêm dos relatórios e do `DECISOES.md` de cada fase.*

## Resumo

Agentes de programação baseados em Grandes Modelos de Linguagem (LLMs) já escrevem e alteram sistemas inteiros, e
podem ser orientados por arquivos de projeto e *skills*, o que aqui se chama *harness*. Este trabalho investiga como
essas orientações mudam a forma como os agentes aplicam *design patterns*. O estudo passou por quatro fases: a
construção de um *harness* genérico; um experimento confirmatório com pré-registro (11 modelos, o padrão Strategy,
quatro níveis de orientação, 220 execuções); uma pesquisa exploratória com cinco patterns, na construção e na
manutenção; e o estudo final (TCC_V1), com banco de dados, um verificador de qualidade em cinco dimensões calibrado em
casos de resposta conhecida e cinco rodadas repetidas (324 execuções). Os resultados convergem: as
orientações aumentam o uso correto do pattern (no confirmatório, de 67% para 93% das execuções; na manutenção final, de
3 para 11 acertos em 15), quase não criam estrutura desnecessária e não pioram a correção; o efeito é maior nos modelos
que não acertam sozinhos e quando o agente muda um código pronto, e o modelo pesa mais que a orientação.

## 1. Introdução

Agentes de programação, como o Claude Code, recebem um pedido em linguagem natural e escrevem, testam e alteram o
código de um projeto sem intervenção humana [CITAR: trabalhos sobre agentes de programação autônomos]. Esses agentes
podem receber orientações persistentes: um arquivo de projeto (o `CLAUDE.md`), *skills* (instruções que o agente abre
durante a tarefa) e processos (como desenhar antes de codificar, ou um revisor) [CITAR: documentação do Claude Code].
Neste trabalho, *harness* é o conjunto portátil dessas práticas e orientações.

A pergunta é se essas orientações levam o agente a decisões de projeto melhores. *Design patterns* servem bem a essa
pergunta porque têm um gabarito: são soluções conhecidas para variações que crescem [CITAR: Gamma et al., Design
Patterns], e a regra de três diz quando uma variação passa a justificar a estrutura [CITAR: Fowler, Refactoring;
Kerievsky, Refactoring to Patterns]. Também têm um risco simétrico: estrutura onde ela não se justifica, o exagero.

A pergunta geral do trabalho é: **como as orientações de projeto dadas a um agente de programação mudam a forma como
ele aplica design patterns, ao construir um sistema do zero e ao manter um sistema pronto?** Ela se desdobra em seis
perguntas, todas respondidas na Seção 5:

- **P1.** Como o *harness* muda o uso do pattern pedido?
- **P2.** Como ele muda o exagero, a correção e as outras dimensões de qualidade?
- **P3.** Como o efeito varia entre os modelos?
- **P4.** Como o tipo de tarefa (construir ou manter) muda o efeito?
- **P5.** Como cada componente do *harness* (o `CLAUDE.md`, a *skill*, o processo) contribui?
- **P6.** Quão estáveis são os resultados quando a mesma tarefa se repete?

As contribuições são: (i) um experimento com pré-registro sobre o Strategy, com 11 modelos e quatro níveis de
orientação; (ii) uma pesquisa com cinco patterns em duas tarefas (construção e manutenção) sobre o mesmo domínio; (iii)
um verificador de qualidade em cinco dimensões, calibrado em casos de resposta conhecida; e (iv) a medida da
estabilidade dos resultados em cinco rodadas.

O restante do artigo está organizado assim: a Seção 2 descreve como o estudo evoluiu; a Seção 3, o método do estudo
final; a Seção 4, os resultados de cada fase; a Seção 5 responde às perguntas; a Seção 6 traz as limitações; e a Seção
7 conclui.

## 2. A evolução do estudo

O desenho final não foi o inicial. Cada fase respondeu a um problema da anterior, e o registro de cada decisão está no
`DECISOES.md` e nos relatórios de cada pasta.

**Fase 1: o *harness* genérico (até 07/09).** O trabalho começou construindo um *harness* de uso geral, a partir de
um levantamento de 72 repositórios públicos de *harness* [CITAR: os repositórios levantados, se forem citados]. A
definição adotada foi subtrativa: o *harness* é o conjunto de práticas, verificações e permissões que mantém o trabalho
do agente coerente com o pedido, com o código existente e com o resto do sistema. Em 06/09, o objeto do trabalho
passou a ser os princípios de projeto de um *harness*, e não um *harness* específico.

**Fase 2: o experimento confirmatório do Strategy (12/09 a 12/10).** Na orientação de 12/09, o recorte foi fixado:
construir software × design de baixo nível, com e sem *harness*, em mais de um modelo. O enunciado é o de uma loja
online (o *checkout*), escrito na voz do dono, com quatro pontos que pedem o Strategy e um controle negativo, em que
usar o pattern é exagero. Uma bancada (a `construction-v3`) serviu para achar defeitos no enunciado, na correção e na
análise antes de medir: uma suíte de aceitação pela API (provada com mutantes), a classificação do desenho pelo
Semgrep, conferida por leitura humana às cegas, e métricas de qualidade (CK e SonarQube). Uma primeira versão do lote
(o V4) foi descartada antes da análise, por um mal-entendido sobre quais modelos entrariam nas conclusões. O lote que
vale, o V5, foi congelado num pré-registro em 10/10, antes da primeira execução: 11 modelos, quatro níveis (N0 sem
*harness*; N1 um `CLAUDE.md`; N2 o N1 com uma *skill* pública de patterns; N3 o N2 com um processo de desenho e um
revisor), cinco réplicas, 220 execuções.

**Fase 3: cinco patterns, construção e manutenção (14/10 a 19/10).** O V5 mostrou um teto: cinco dos 11 modelos já
acertavam o Strategy sem orientação, e o recorte deixava de fora a manutenção. A pesquisa nova passou a medir cinco
patterns (Strategy, Observer, Factory Method, Decorator, Template Method) num domínio novo, uma pousada, em duas
tarefas: a manutenção (`maintenance-v1`: o agente muda um sistema pronto sem nenhum pattern) e a construção
(`construction-v6`, a loja, e depois `construction-v7`, a pousada). O *framework* passou de Spring Boot para Javalin,
porque o Spring entrega pronto parte do que se mede (eventos, contêiner de objetos, aspectos). A correção passou a ser
feita por uma IA (Claude Opus 5.5) às cegas, calibrada nas soluções de referência de cada pattern. Os modelos ficaram
três (Haiku 4.5, Sonnet 4.5 e Sonnet 5.5), com uma execução por combinação.

**Fase 4: o estudo final, TCC_V1 (19/10 a 23/10).** As duas pesquisas foram reunidas num lugar só, com os mesmos
cinco cenários, a mesma base e os mesmos três níveis (N0, N1, N2). A *skill* pública foi trocada por uma escrita para o
trabalho, porque a pública não separava patterns vizinhos e tinha levado um modelo a abrir a *skill* com uma pergunta
e parar sem programar. Depois de uma rodada sem banco, o estudo passou a ter um PostgreSQL em cada execução e um
verificador de qualidade em cinco dimensões, para preencher a tabela que o trabalho quer responder (construir e manter
× arquitetura, design, testes, banco × com e sem *harness*).

## 3. Método do estudo final (TCC_V1)

### 3.1 Tarefas, cenários e níveis

O sistema é o de reservas da Pousada Maré Alta, em Java 21 com Javalin e os dados num PostgreSQL 18. Na **construção**,
o agente recebe uma pasta vazia e o enunciado do sistema inteiro, na voz do dono; os testes não são pedidos. Na
**manutenção**, recebe o sistema pronto, sem nenhum pattern, com o esquema em migrações e 26 testes, e um pedido de
mudança. Os cinco cenários pedem um pattern cada, sem dizer o nome: cada texto mostra uma variação que já tem vários
casos e diz que ela vai crescer. Os níveis são N0 (só o texto), N1 (um `CLAUDE.md` com princípios, sem nomear
patterns) e N2 (o mesmo `CLAUDE.md` e a *skill* `design-patterns`). Os modelos são o Claude Haiku 4.5, o Claude Sonnet
4.5 e o Claude Sonnet 5.5, todos com o mesmo esforço (*medium*), cada execução num contêiner com um banco só dela.

### 3.2 O verificador de qualidade

Cada projeto foi revisado por um agente (Claude Opus 5.5), às cegas quanto ao modelo e ao nível, com uma *skill* e um
*script* de medição por dimensão: a arquitetura (dependências e ciclos entre pacotes, pelo jdeps); o design de baixo
nível (a classe do pattern: acerto, alternativa equivalente, malfeito ou omissão; e o design do resto do código); os
testes (contagem e cobertura, pelo Maven e pelo JaCoCo); o banco (o esquema, pelo `pg_dump`, e conferências como
dinheiro com duas casas e chave estrangeira com índice); e o comportamento (um roteiro de chamadas à API por cenário,
com os dados depois de reiniciar e duas reservas simultâneas). Cada dimensão recebe **bom**, **com problemas** ou
**ruim**. Antes de valer, o verificador foi calibrado em 26 casos de resposta conhecida (as soluções de referência,
com e sem o pattern, e seis exemplos estragados de propósito): as 52 notas esperadas conferiram.

### 3.3 As rodadas

O piloto teve uma execução por combinação (2 tarefas × 5 cenários × 3 níveis × 3 modelos = 90). O pattern que mais
errou (Decorator) e o que mais acertou (Factory Method) foram repetidos até cinco rodadas, com mais 144 execuções.

## 4. Resultados

### 4.1 O confirmatório do Strategy (V5, 220 execuções)

A Tabela 1 mostra a escada de orientação, com 55 execuções por nível. O "P4 certo" é o ponto mais difícil do
enunciado: cada caso isolado e nenhuma condição sobre o caso no cálculo.

**Tabela 1.** O V5 por nível (11 modelos × 5 réplicas em cada nível).

| medida | N0 | N1 | N2 | N3 |
|---|---|---|---|---|
| P4 certo | 67% | 93% | 93% | 98% |
| exagero no controle negativo | 0% | 2% | 5% | 0% |
| suíte de aceitação inteira (21 casos) | 71% | 84% | 80% | 87% |
| complexidade cognitiva (média) | 33,7 | 22,9 | 23,5 | 22,5 |
| linhas de código (média) | 519 | 584 | 614 | 571 |
| custo médio por execução (US$) | 1,32 | 1,52 | 1,47 | 1,98 |

Das cinco hipóteses principais do pré-registro (N1 contra N0), quatro foram apoiadas: o `CLAUDE.md` isola os casos do
P4, não piora a correção, reduz a complexidade e tem efeito maior nos modelos mais fracos; a do exagero foi contrariada
no sentido de que o *harness* **não muda** o exagero. O efeito apareceu nos seis modelos fora do teto e em nenhum
contra: o Opus 4.7, por exemplo, foi de 0 para 5 acertos em 5, e o Haiku 4.5, de 0 no N0 para 4 no N3. A *skill* (N2
contra N1) ficou inconclusiva; ela não foi aberta espontaneamente em nenhuma das 110 execuções com *skill*.

### 4.2 Cinco patterns, construção e manutenção (fase 3)

Com a *skill* pública e uma execução por combinação, a construção da pousada (`construction-v7`, 45 execuções) teve 6
acertos no N0, 9 no N1 e 8 mais uma alternativa no N2; a manutenção (`maintenance-v1`) teve 1, 3 mais uma alternativa,
e 6 mais uma alternativa nos cenários que pedem o pattern, e 44 contenções em 45 controles negativos. O Factory Method e
o Strategy saíram com mais frequência; o Decorator e o Template Method, só no Sonnet 5.5 e só com orientação. Dois
achados guiaram a fase seguinte: o princípio "o próximo caso num lugar só" foi lido como "num método só" no Decorator;
e o Sonnet 4.5, ao abrir a *skill* passando uma pergunta como argumento, tratou a resposta como a tarefa e entregou a
pasta vazia em duas construções.

### 4.3 O estudo final com banco (TCC_V1, 90 execuções)

A Tabela 2 mostra quantas execuções ficaram boas, em 15 por nível.

**Tabela 2.** Execuções boas por dimensão, tarefa e nível (piloto do TCC_V1).

| dimensão | Construir N0 | N1 | N2 | Manter N0 | N1 | N2 |
|---|---|---|---|---|---|---|
| pattern (acerto ou alternativa) | 9 | 11 | 12 | 3 | 6 | 11 |
| design do resto do código | 3 | 4 | 4 | 1 | 5 | 4 |
| arquitetura | 4 | 0 | 3 | 12 | 13 | 13 |
| testes | 2 | 3 | 2 | 7 | 8 | 4 |
| banco | 0 | 0 | 0 | 12 | 12 | 13 |
| comportamento | 5 | 5 | 5 | 13 | 12 | 9 |

Construindo, nenhuma das 45 execuções usou migrações de esquema, e 15 deixaram o mesmo quarto ser reservado duas vezes
quando dois pedidos chegam juntos; mantendo, as 45 conservaram as migrações e a proteção da base. O Sonnet 5.5 teve o
comportamento bom nas 30 execuções; o Haiku 4.5, construindo, não entregou um sistema funcional em nenhuma das 15.

### 4.4 As cinco rodadas (144 execuções)

**Tabela 3.** Execuções com o pattern certo (acerto ou alternativa), em cinco rodadas.

| pattern | tarefa | N0 | N1 | N2 |
|---|---|---|---|---|
| Factory Method | construir | 12 de 15 | 13 de 15 | 14 de 15 |
| Factory Method | manter | 6 de 15 | 11 de 15 | 14 de 15 |
| Decorator | construir | 4 de 15 | 10 de 15 | 10 de 15 |
| Decorator | manter | 3 de 15 | 5 de 15 | 8 de 15 |

O resultado do piloto (pattern certo ou não) se repetiu em 119 das 144 rodadas novas (83%), e em 21 das 36
combinações as cinco rodadas deram o mesmo resultado.

## 5. Discussão: as respostas às perguntas

**P1. Como o *harness* muda o uso do pattern?** Ele aumenta o uso correto, em todas as fases. No confirmatório, o
ponto difícil do Strategy foi de 67% para 93% das execuções só com o `CLAUDE.md`; no estudo final, na manutenção, de 3
para 6 e 11 acertos em 15; nas cinco rodadas do Factory Method mantido, de 6 para 14 em 15. As orientações agem
justamente sobre o que dizem: juntar a variação num lugar e não repetir passos.

**P2. Como ele muda o exagero, a correção e as outras dimensões?** O exagero quase não aparece (no confirmatório, de 0%
para no máximo 5%; no estudo final, 5 estruturas desnecessárias em 90 execuções da rodada sem banco), e a correção não
piora (apoiada no pré-registro). A complexidade cai com o *harness*, mas as linhas e as classes sobem: o código fica
mais organizado e maior. No estudo final, arquitetura, testes, banco e comportamento quase não mudam entre os níveis: o
*harness* fala de estrutura, e é ali que age.

**P3. Como o efeito varia entre os modelos?** O efeito se concentra nos modelos que não acertam sozinhos. No
confirmatório, cinco dos 11 modelos já estavam no teto no N0, e o ganho veio dos outros seis. No estudo final, o modelo
pesa mais que o *harness*: o Sonnet 5.5 sem orientação supera o Haiku 4.5 com a *skill* em quase todas as dimensões, e
o Haiku não chegou ao Decorator em 30 execuções. O *harness* ajuda mais os modelos intermediários: no Decorator
construído, o Sonnet 4.5 foi de 2 em 5 para 5 em 5.

**P4. Como o tipo de tarefa muda o efeito?** O efeito é maior na manutenção. Diante de um código que já funciona, o
agente sem orientação tende a acrescentar mais uma condição (3 acertos em 15 no N0 do estudo final, contra 9 na
construção), e a orientação desfaz esse hábito. Por outro lado, o ponto de partida pesa mais que a orientação nas
dimensões que a base já resolve: mantendo, o agente conserva a arquitetura, as migrações e as restrições do banco;
construindo, nenhuma execução usou migrações.

**P5. Como cada componente contribui?** O `CLAUDE.md` faz a maior parte do efeito. No confirmatório, a *skill* pública
não acrescentou nada mensurável, porque não foi aberta; o processo (N3) teve efeito no desenho e no exagero. Quando o
agente é mandado abrir a *skill*, ela ajuda nos patterns menos óbvios: no Decorator, a forma literal do pattern só
apareceu com a *skill*, e só no Sonnet 5.5, nas dez rodadas. Ela também tem um risco: aberta com uma pergunta, levou dois
modelos diferentes a responder em texto e parar sem programar.

**P6. Quão estáveis são os resultados?** O resultado de uma execução isolada se confirmou em 83% das repetições, o que
sustenta as tendências, mas não diferenças de uma ou duas execuções. A variação concentra-se no nível sem orientação e
no modelo mais fraco; com orientação, o Sonnet 5.5 repetiu o mesmo resultado nas cinco rodadas de todas as combinações,
e o Sonnet 4.5 ainda variou no Decorator mantido. Alguns erros são constantes: o
Sonnet 5.5, mantendo sem orientação, não acertou o Factory Method em nenhuma das cinco rodadas, e acertou nas dez com
orientação.

## 6. Limitações

- **Uma só família de modelos e um só agente** (Claude e Claude Code): não se separa o efeito do *harness* do efeito do
  par.
- **As orientações foram escritas por quem conhece as tarefas.** No estudo final, a *skill* e o `CLAUDE.md` tendem a
  favorecer os níveis com orientação; no confirmatório, a *skill* era pública.
- **A correção do estudo final é feita por uma IA.** Foi calibrada (52 de 52) e julga com números de ferramentas e
  evidência no código; no confirmatório, a classificação automática foi conferida por um leitor humano (174 de 176
  respostas iguais), mas a releitura prevista não foi feita.
- **Réplicas desiguais:** cinco no confirmatório e nos dois patterns repetidos; uma nos outros três do estudo final.
- **O ambiente** não tinha Docker dentro do contêiner do agente, o que derrubou os testes de integração que dependiam
  dele.

## 7. Conclusão

O trabalho investigou como orientações de projeto mudam a forma como agentes de programação aplicam *design patterns*,
num caminho que foi de um *harness* genérico a um experimento com pré-registro e, por fim, a um estudo com cinco
patterns, duas tarefas, banco de dados e um verificador calibrado. As fases convergem: as orientações aumentam o uso
correto do pattern sem criar estrutura desnecessária e sem piorar a correção; o efeito é maior nos modelos que não
acertam sozinhos e na manutenção de código pronto; e o modelo continua pesando mais que a orientação nas demais
dimensões de qualidade. Como trabalhos futuros, ficam as réplicas dos três cenários restantes, outras famílias de
modelos e de agentes, e um segundo leitor humano para a correção.

## Referências

[CITAR: Gamma, Helm, Johnson e Vlissides, *Design Patterns: Elements of Reusable Object-Oriented Software*]

[CITAR: Fowler, *Refactoring*]

[CITAR: Kerievsky, *Refactoring to Patterns*]

[CITAR: documentação do Claude Code (CLAUDE.md, skills e subagentes)]

[CITAR: trabalhos sobre agentes de programação autônomos e sua avaliação]

[CITAR: a skill pública gof-patterns (grndlvl/software-patterns), usada no confirmatório e na fase 3]

[CITAR: as ferramentas de medição usadas (Semgrep, SonarQube, CK, JaCoCo)]
