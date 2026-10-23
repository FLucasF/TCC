# Orientações de projeto e design patterns em agentes de programação: construção e manutenção de um sistema com banco de dados

*Rascunho de 23/10/2026, só sobre o TCC_V1. Os marcadores `[CITAR: ...]` indicam onde falta uma referência; nenhuma
foi inventada.*

## Resumo

Agentes de programação baseados em Grandes Modelos de Linguagem (LLMs) já constroem e alteram sistemas inteiros, mas
pouco se sabe sobre como orientações de projeto mudam a qualidade do código que eles produzem. Este trabalho avalia
três modelos (Claude Haiku 4.5, Claude Sonnet 4.5 e Claude Sonnet 5.5) em duas tarefas, construir um sistema de
reservas do zero e mudar um sistema pronto, cada uma com cinco cenários que pedem um *design pattern* sem nomeá-lo, em
três níveis de orientação: nenhuma, um arquivo de princípios e o mesmo arquivo com uma *skill* de patterns. As 234
execuções foram corrigidas por um verificador automático calibrado (52 de 52 notas certas em casos de resposta
conhecida). As orientações mudam principalmente o uso do pattern, e mais na manutenção (de 3 para 11 acertos em 15);
nas demais dimensões, pesam mais o modelo e o ponto de partida. Em cinco rodadas repetidas, o resultado do piloto se
manteve em 83% das execuções.

## 1. Introdução

Agentes de programação, como o Claude Code, recebem um pedido em linguagem natural e escrevem, testam e alteram o
código de um projeto sem intervenção humana [CITAR: trabalho sobre agentes de programação autônomos]. Esses agentes
podem ser orientados por arquivos de projeto (o `CLAUDE.md`) e por *skills*, conjuntos de instruções que o agente
consulta durante a tarefa [CITAR: documentação do Claude Code sobre CLAUDE.md e skills]. Chama-se aqui *harness* o
conjunto dessas orientações.

O problema é saber se essas orientações levam o agente a decisões de projeto melhores. *Design patterns* são um caso
adequado para essa pergunta: são soluções conhecidas para variações que crescem [CITAR: Gamma et al., Design
Patterns], e a regra de três diz quando uma variação passa a justificar a estrutura [CITAR: Fowler, Refactoring;
Kerievsky, Refactoring to Patterns].

O objetivo deste trabalho é entender **como as orientações de projeto dadas ao agente mudam a qualidade do código que
ele produz ao construir um sistema do zero e ao manter um sistema pronto**. Desdobram-se cinco perguntas:

- **P1.** Como o *harness* muda o uso do pattern e as outras dimensões de qualidade (arquitetura, testes, banco,
  comportamento)?
- **P2.** Como o tipo de tarefa (construir ou manter) muda esse efeito?
- **P3.** Como a qualidade varia entre os modelos, e quanto dessa diferença o *harness* compensa?
- **P4.** Como a dificuldade muda de um pattern para outro?
- **P5.** Quão estável é o resultado quando a mesma tarefa se repete?

As contribuições são: (i) um experimento controlado com duas tarefas, cinco patterns, três níveis de orientação e três
modelos; (ii) um verificador de qualidade em cinco dimensões, que combina ferramentas de medição e julgamento por IA,
calibrado em casos de resposta conhecida; e (iii) a medida da estabilidade dos resultados em cinco rodadas.

## 2. Metodologia

### 2.1 O domínio e as tarefas

O sistema é o de reservas de uma pousada fictícia (a Pousada Maré Alta), em Java 21 com o *framework* Javalin e os
dados num PostgreSQL 18. Há duas tarefas:

- **Construção:** o agente recebe uma pasta vazia e o enunciado do sistema inteiro, escrito na voz do dono da pousada.
- **Manutenção:** o agente recebe o sistema pronto, sem nenhum pattern, com o esquema do banco em migrações e 26
  testes, e um pedido de mudança.

Os cinco cenários pedem, sem dizer o nome, um pattern cada: Strategy (planos de tarifa), Observer (avisos da
confirmação), Factory Method (documento por tipo de hóspede), Decorator (regras de envio de e-mail) e Template Method
(relatórios). Cada texto mostra uma variação que já tem vários casos e diz que ela vai crescer. Os testes não são
pedidos no enunciado da construção, para medir a iniciativa do agente.

### 2.2 Os níveis de orientação e os modelos

- **N0:** só o texto.
- **N1:** o texto e um `CLAUDE.md` com princípios gerais (o próximo caso entra num lugar só; passos repetidos escritos
  uma vez; nada de estrutura que ninguém pediu), sem nomear nenhum pattern.
- **N2:** o mesmo `CLAUDE.md`, com a instrução de usar a *skill* `design-patterns`, escrita para o trabalho (um
  processo de decisão curto e uma referência por pattern, com exemplos fora do domínio da pousada).

Os modelos são o Claude Haiku 4.5, o Claude Sonnet 4.5 e o Claude Sonnet 5.5, com o mesmo nível de esforço
(*medium*), no Claude Code sem interação, cada execução num contêiner com um banco só dela.

### 2.3 O verificador de qualidade

Cada projeto foi corrigido por um agente revisor (Claude Opus 5.5), às cegas quanto ao modelo e ao nível, com uma
*skill* por dimensão e um *script* de medição em cada uma:

| dimensão | medida | julgamento |
|---|---|---|
| arquitetura | dependências entre pacotes e ciclos (jdeps) | camadas separadas, borda HTTP fina, acesso a dados isolado |
| design de baixo nível | leitura do código | a classe do pattern (acerto, alternativa equivalente, malfeito, omissão) e o resto do design |
| testes | testes e cobertura (Maven e JaCoCo) | se conferem as regras pedidas com valores |
| banco | esquema (`pg_dump`) e conferências simples | migrações, tipos, chaves, restrições, SQL com parâmetros |
| comportamento | um roteiro de chamadas à API por cenário | os valores do texto, os dados depois de reiniciar, duas reservas simultâneas |

Cada dimensão recebe a nota **bom**, **com problemas** ou **ruim**. Antes de valer, o verificador foi calibrado em
26 casos de resposta conhecida: as soluções de referência de cada pattern (com e sem o pattern, nas duas tarefas) e
seis exemplos estragados de propósito (arquitetura misturada, testes sem asserção, banco com tipos errados, entre
outros). As 52 notas esperadas conferiram.

### 2.4 As rodadas

O piloto teve uma execução por combinação: 2 tarefas × 5 cenários × 3 níveis × 3 modelos = 90 execuções. Para medir a
estabilidade (P5), o pattern que mais errou (Decorator) e o que mais acertou (Factory Method) foram repetidos até
cinco rodadas, nas duas tarefas, com os três modelos e os três níveis: mais 144 execuções.

## 3. Resultados

### 3.1 O piloto

A Tabela 1 mostra quantas execuções ficaram boas, em 15 por nível (no pattern, acerto ou alternativa equivalente).

**Tabela 1.** Execuções boas por dimensão, tarefa e nível (piloto, 90 execuções).

| dimensão | Construir N0 | N1 | N2 | Manter N0 | N1 | N2 |
|---|---|---|---|---|---|---|
| pattern | 9 | 11 | 12 | 3 | 6 | 11 |
| design do resto do código | 3 | 4 | 4 | 1 | 5 | 4 |
| arquitetura | 4 | 0 | 3 | 12 | 13 | 13 |
| testes | 2 | 3 | 2 | 7 | 8 | 4 |
| banco | 0 | 0 | 0 | 12 | 12 | 13 |
| comportamento | 5 | 5 | 5 | 13 | 12 | 9 |

Por modelo, o Sonnet 5.5 teve o comportamento bom nas 30 execuções e o pattern certo em 25; o Haiku 4.5, construindo,
não entregou um sistema funcional em nenhuma das 15; o Sonnet 4.5 acertou o pattern em 12 das 15 construções, mas 13
delas deixaram reservar o mesmo quarto duas vezes quando dois pedidos chegam juntos.

### 3.2 As cinco rodadas

A Tabela 2 resume as cinco rodadas do Factory Method e do Decorator (45 execuções por pattern e tarefa).

**Tabela 2.** Execuções com o pattern certo (acerto ou alternativa), em cinco rodadas.

| pattern | tarefa | N0 | N1 | N2 | total |
|---|---|---|---|---|---|
| Factory Method | construir | 12 de 15 | 13 de 15 | 14 de 15 | 39 de 45 |
| Factory Method | manter | 6 de 15 | 11 de 15 | 14 de 15 | 31 de 45 |
| Decorator | construir | 4 de 15 | 10 de 15 | 10 de 15 | 24 de 45 |
| Decorator | manter | 3 de 15 | 5 de 15 | 8 de 15 | 16 de 45 |

O resultado do piloto (pattern certo ou não) se repetiu em 119 das 144 rodadas novas (83%), e em 21 das 36
combinações as cinco rodadas deram o mesmo resultado.

## 4. Discussão: as respostas às perguntas

**P1. Como o *harness* muda a qualidade?** Ele muda sobretudo o uso do pattern. Na manutenção, o pattern certo passou
de 3 (N0) para 6 (N1) e 11 (N2) em 15 no piloto, e de 6 para 14 no Factory Method em cinco rodadas. Nas outras
dimensões, a diferença entre os níveis foi pequena e sem direção constante. As orientações falam da variação e dos
passos repetidos, e é ali que agem.

**P2. Como o tipo de tarefa muda o efeito?** O efeito é maior na manutenção. Diante de um sistema que já funciona, o
caminho natural do agente sem orientação é acrescentar mais uma condição ao código existente (3 acertos em 15 no N0); a
orientação desfaz esse hábito. Construindo, os modelos já organizam a variação com mais frequência sem ajuda (9 em 15).
Em contrapartida, manter sai muito melhor nas dimensões que a base resolve: as 45 manutenções conservaram as migrações e
a proteção contra reserva dupla, enquanto nenhuma das 45 construções usou migrações e 15 deixaram passar a reserva
dupla. O ponto de partida pesa mais que a orientação em arquitetura, banco e comportamento.

**P3. Como a qualidade varia entre os modelos?** O modelo pesa mais que o *harness*. O Haiku 4.5 com a *skill* ficou
abaixo do Sonnet 5.5 sem nenhuma orientação em quase todas as dimensões, e nunca chegou ao Decorator (1 alternativa em
30 execuções nas cinco rodadas). O *harness* ajuda mais os modelos intermediários: no Decorator construído, o Sonnet 4.5
passou de 2 em 5 (N0) para 5 em 5 (N1 e N2).

**P4. Como a dificuldade muda entre patterns?** O Factory Method é o mais fácil (70 de 90 nas cinco rodadas) e o
Decorator o mais difícil (40 de 90). No Decorator, a forma literal do pattern (embrulhos empilhados) só apareceu com a
*skill*, e só no Sonnet 5.5, em todas as dez rodadas; sem ela, o mesmo modelo preferiu uma lista de regras, que o
critério aceita como alternativa equivalente.

**P5. Quão estável é o resultado?** O resultado de uma execução isolada se confirmou em 83% das repetições, o que
sustenta as tendências do piloto, mas não as diferenças de uma ou duas execuções. A variação concentra-se no N0 e nos
casos de fronteira: o Sonnet 5.5 sem orientação alternou entre a alternativa e a omissão no Decorator, e o Haiku 4.5
alternou entre acerto, malfeito e omissão no Factory Method. Já alguns erros são constantes: o Sonnet 5.5, mantendo
sem orientação, não acertou o Factory Method em nenhuma das cinco rodadas (a escolha do documento ficou em
condicionais), e acertou nas dez rodadas com orientação.

### 4.1 Limitações

- O verificador é uma IA; foi calibrado (52 de 52) e julga com números de ferramentas e evidência no código, mas a
  nota resume uma leitura.
- O `CLAUDE.md` e a *skill* foram escritos por quem conhece as tarefas e o critério, o que tende a favorecer os níveis
  com orientação.
- Apenas dois patterns foram repetidos; nos outros três, há uma execução por combinação.
- O ambiente não tinha Docker dentro do contêiner do agente, o que fez falhar os testes de integração que dependiam
  dele (só no Sonnet 4.5).
- Os três modelos são de um mesmo fornecedor.

## 5. Conclusão

Este trabalho avaliou como orientações de projeto mudam a qualidade do código de agentes de programação, em duas
tarefas e cinco patterns. As orientações mudam principalmente a decisão de usar o pattern, com efeito maior quando o
agente altera um sistema pronto; nas demais dimensões, o modelo e o ponto de partida pesam mais. Os resultados se
mostraram estáveis em cinco rodadas na maior parte das combinações. Como trabalhos futuros, ficam a repetição dos três
cenários restantes, a comparação com uma *skill* pública e a inclusão de modelos de outros fornecedores.

## Referências

[CITAR: Gamma, Helm, Johnson e Vlissides, *Design Patterns: Elements of Reusable Object-Oriented Software*]

[CITAR: Fowler, *Refactoring*]

[CITAR: Kerievsky, *Refactoring to Patterns*]

[CITAR: documentação do Claude Code (CLAUDE.md e skills)]

[CITAR: trabalho sobre agentes de programação autônomos e sua avaliação]
