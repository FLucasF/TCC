# TCC_V1: resultados da construção e da manutenção com patterns

21 de outubro de 2026. Resultados das 90 execuções do `TCC_V1` e da correção feita por IA.

## 1. O que foi testado

**A pergunta.** Quando o texto que um agente de programação recebe precisa de um design pattern (sem dizer o nome),
o agente aplica, e aplica certo? E as orientações do projeto (o harness) mudam isso?

**As duas pesquisas,** com o mesmo domínio (o sistema de reservas da Pousada Maré Alta) e a mesma pilha (Java 21 e
Javalin, sem Spring):

- **Construção:** o agente recebe uma pasta vazia e o enunciado do sistema inteiro, na voz do dono da pousada, e
  constrói do zero.
- **Manutenção:** o agente recebe o sistema pronto, escrito sem nenhum pattern, e um pedido de mudança.

**Os 5 cenários**, cada um pedindo um pattern sem nomeá-lo:

| cenário | o que o dono pede | pattern esperado |
|---|---|---|
| 01 | cinco planos de tarifa, cada um com preço, regra e devolução próprios, e mais planos chegando | Strategy |
| 03 | avisar a governança e o financeiro quando a reserva é confirmada ou cancelada, e mais gente querendo saber | Observer |
| 05 | um documento diferente por tipo de hóspede (CPF, empresa, estrangeiro), nos três lugares onde ele sai | Factory Method |
| 07 | três regras em volta do envio de e-mails, combinadas conforme o modo de teste e a instalação, e mais regras anunciadas | Decorator |
| 09 | quatro relatórios com os mesmos passos (título, cabeçalho, linhas, total), que vão mudar juntos, e mais relatórios anunciados | Template Method |

**Os 3 braços:**

- **N0:** só o texto;
- **N1:** o texto e um `CLAUDE.md` com princípios gerais, sem nomear pattern (o próximo caso entra num lugar só, sem
  condição a mais num método que já existe; passos repetidos escritos uma vez só; nada de estrutura para o que
  ninguém pediu);
- **N2:** o mesmo `CLAUDE.md` e a skill `design-patterns` (um processo de decisão curto, uma tabela que separa os
  patterns parecidos e o "quando não usar"), com a linha "use a skill `design-patterns`".

**Os 3 modelos:** Claude Haiku 4.5, Claude Sonnet 4.5 e Claude Sonnet 5.5, effort `medium`, uma execução por
combinação: 2 pesquisas × 5 cenários × 3 braços × 3 modelos = **90 execuções**.

## 2. Como a correção foi feita

A correção é do Claude Opus 5.5, às cegas (não sabe o modelo nem o braço), com critérios escritos por pattern. Antes
de usar, o avaliador foi testado em soluções de resposta conhecida (o pattern feito certo e a versão sem pattern de
cada cenário) e acertou 10 de 10 nas duas pesquisas. Nesta rodada, por causa do horário, o Opus rodou com effort
`medium` e julgou o comportamento pela leitura do código, sem subir o serviço (os 90 projetos compilaram e passaram
nos próprios testes).

As palavras da correção: **acerto** (o pattern aplicado direito, em todos os casos), **alternativa** (outra estrutura
que os critérios aceitam, como uma corrente de regras no lugar do Decorator), **malfeito** (a estrutura existe, mas
convive com o jeito sem ela) e **omissão** (a variação ficou em `if`, `switch` ou cópia).

## 3. Os resultados

Legenda: ✅ acerto · 🔷 alternativa · ⚠️ malfeito · ❌ omissão.

Em cada célula: o resultado e, em poucas palavras, o que o agente fez.

### Construção

| cenário | modelo | N0 | N1 | N2 |
|---|---|---|---|---|
| **01 Strategy** | Haiku 4.5 | ❌ três `switch` sobre o plano | ✅ classe abstrata, uma subclasse por plano | ✅ interface, uma classe por plano |
| | Sonnet 4.5 | ❌ `if`/`switch` sobre o plano | ✅ interface, uma classe por plano, num mapa | ✅ interface, uma classe por plano, num `EnumMap` |
| | Sonnet 5.5 | ✅ interface, uma classe por plano, num mapa | ✅ classe abstrata, uma subclasse por plano | ✅ interface, uma classe por plano, num mapa |
| **03 Observer** | Haiku 4.5 | ✅ eventos e um ouvinte por interessado | ✅ um ouvinte por interessado | ⚠️ ouvintes, mas o hóspede ficou de fora |
| | Sonnet 4.5 | ✅ barramento de eventos, um ouvinte por interessado | ⚠️ ouvintes, mas um notificador central monta todos os e-mails | ✅ um ouvinte por interessado, lista vinda de fora |
| | Sonnet 5.5 | ✅ ouvintes registrados no `main` | ✅ ouvintes vindos de fora | ✅ Observer completo |
| **05 Factory Method** | Haiku 4.5 | ✅ fábrica concentrada | ✅ fábrica concentrada | ✅ fábrica com uma classe por documento |
| | Sonnet 4.5 | ✅ fábrica concentrada | ⚠️ documento por tipo, mas o título escolhido por `instanceof` fora | ⚠️ fábrica, mas o título escolhido por `instanceof` fora |
| | Sonnet 5.5 | ✅ método polimórfico no tipo de hóspede | ✅ método polimórfico no tipo de hóspede | ✅ método polimórfico no tipo de hóspede |
| **07 Decorator** | Haiku 4.5 | ❌ três `if` dentro do envio | ❌ três `if` dentro do envio | ⚠️ corrente de regras, mas sempre as três, em qualquer modo |
| | Sonnet 4.5 | 🔷 lista de regras, modo escolhido na montagem | 🔷 lista de regras, modo escolhido na montagem | 🔷 lista de regras, modo escolhido na montagem |
| | Sonnet 5.5 | 🔷 lista de regras com escopo por instalação | 🔷 lista de regras, modo escolhido na montagem | ✅ embrulhos empilhados conforme o modo |
| **09 Template Method** | Haiku 4.5 | ❌ quatro relatórios inteiros, título copiado | ❌ quatro relatórios inteiros, título copiado | ❌ quatro relatórios inteiros, título copiado |
| | Sonnet 4.5 | ⚠️ só um utilitário comum; passos repetidos | ✅ classe abstrata com o método-modelo | ✅ classe abstrata com o método-modelo `final` |
| | Sonnet 5.5 | 🔷 escritor de relatório comum | 🔷 escritor de relatório comum | ✅ classe abstrata com o método-modelo `final` |

### Manutenção

| cenário | modelo | N0 | N1 | N2 |
|---|---|---|---|---|
| **01 Strategy** | Haiku 4.5 | ❌ os `switch` da base cresceram | ❌ três `switch` sobre o plano | ❌ `switch` da base ampliados |
| | Sonnet 4.5 | ❌ `if`/`switch` da base ampliados | ✅ interface, uma classe por plano; `switch` removidos | ✅ interface, uma classe por plano, num mapa |
| | Sonnet 5.5 | ✅ o `enum` virou estratégia | ✅ o `enum` virou estratégia | ✅ interface, uma classe por plano |
| **03 Observer** | Haiku 4.5 | ⚠️ ouvintes, mas o hóspede ficou de fora | ⚠️ ouvintes criados dentro do próprio serviço | ⚠️ ouvintes, mas o hóspede ficou de fora |
| | Sonnet 4.5 | ⚠️ ouvintes, mas o hóspede ficou de fora | ✅ um ouvinte por interessado, registrados na montagem | ✅ um ouvinte por interessado, lista vinda de fora |
| | Sonnet 5.5 | ✅ um ouvinte por interessado, o hóspede incluído | ⚠️ ouvintes, mas o hóspede ficou de fora | ✅ um ouvinte por interessado, lista vinda de fora |
| **05 Factory Method** | Haiku 4.5 | ❌ `if/else` dentro do comprovante | ✅ escolha concentrada num ponto só | ✅ escolha concentrada num ponto só |
| | Sonnet 4.5 | ✅ escolha concentrada no comprovante | ✅ escolha concentrada num método | ✅ fábrica com um gerador por documento |
| | Sonnet 5.5 | ✅ fábrica concentrada | ✅ fábrica concentrada | ✅ método polimórfico no tipo de hóspede |
| **07 Decorator** | Haiku 4.5 | ❌ três `if` dentro do envio | ❌ três `if` dentro do envio | ❌ três `if` dentro do envio |
| | Sonnet 4.5 | ⚠️ lista de regras, mas fixa dentro do envio | ⚠️ lista de regras, mas as regras olham o modo | ⚠️ lista de regras, mas o envio decide o modo |
| | Sonnet 5.5 | 🔷 lista de regras com escopo, montada fora | 🔷 lista de regras, modo declarado fora | ✅ embrulhos empilhados por modo e instalação |
| **09 Template Method** | Haiku 4.5 | ❌ quatro relatórios inteiros, título copiado | ❌ quatro relatórios inteiros, título copiado | ❌ quatro relatórios inteiros, título copiado |
| | Sonnet 4.5 | ⚠️ classe base sem método-modelo | ⚠️ classe comum só junta o texto; passos repetidos | ✅ classe abstrata com o método-modelo `final` |
| | Sonnet 5.5 | 🔷 escritor de relatório comum | ✅ classe abstrata com o método-modelo `final` | ✅ classe abstrata com o método-modelo `final` |

### Os totais (acerto + alternativa)

**Por braço,** em 15 execuções cada:

| braço | construção | manutenção |
|---|---|---|
| N0 | 10 | 6 |
| N1 | 11 | 8 |
| N2 | 11 | 10 |

**Por modelo,** em 15 execuções de cada pesquisa:

| modelo | construção | manutenção | comportamento (pela leitura) |
|---|---|---|---|
| Sonnet 5.5 | 15 | 14 | certo nas 30 |
| Sonnet 4.5 | 10 | 8 | em parte nas 15 da construção; certo em 10 das 15 da manutenção |
| Haiku 4.5 | 7 | 2 | em parte nas 15 da construção; certo em 6 das 15 da manutenção |

**Por pattern,** nas duas pesquisas (18 execuções cada): Factory Method 15, Strategy 12, Observer 11, Template Method 9
e Decorator 9 (contando as alternativas).

**A skill.** Foi aberta em 25 das 30 execuções do N2: em 14 das 15 da construção e em 11 das 15 da manutenção (o Haiku,
na manutenção, só em 1 de 5). Nenhuma execução parou nem ficou sem entregar: as 90 compilaram e passaram nos próprios
testes.

**Exagero** (estrutura sem necessidade fora da parte pedida): 5 em 90, sendo 4 na construção (dois no N2 do Haiku, um
no N2 do Sonnet 4.5, um no N1 do Sonnet 5.5) e 1 na manutenção.

## 4. O que os resultados dizem

1. **O modelo pesa mais que o harness.** O Sonnet 5.5 acerta 29 de 30, com o comportamento certo em todas. O Sonnet
   4.5 acerta 18 de 30. O Haiku acerta 9 de 30, e só 2 de 15 na manutenção.
2. **O harness soma, e soma mais na manutenção.** Na manutenção, os acertos vão de 6 (N0) para 8 (N1) e 10 (N2). Na
   construção, de 10 para 11 e 11: construindo do zero, os modelos já acertam bastante sem ajuda.
3. **Mudar código pronto é mais difícil que construir.** Sem harness, 10 de 15 acertos construindo e 6 de 15 mudando.
   Diante de um sistema que já funciona, o caminho natural do modelo é acrescentar mais uma condição.
4. **Onde o harness mais aparece é no Sonnet 4.5.** Na manutenção, ele passa de omissão ou malfeito no N0 para
   acerto no N1 ou no N2 no Strategy, no Observer e no Template Method.
5. **O Haiku não chega ao Decorator nem ao Template Method** em nenhum braço das duas pesquisas, e quase não abre a
   skill quando muda código pronto (1 em 5).
6. **O Sonnet 5.5 só chega ao Decorator literal com a skill.** Sem ela, faz a corrente de regras (a alternativa); no
   N2, nas duas pesquisas, faz o Decorator.
7. **Quase ninguém exagera:** 5 estruturas sem necessidade em 90 execuções, 3 delas no braço da skill.
8. **O Factory Method e o Strategy são os mais fáceis; o Decorator e o Template Method, os mais difíceis.**

## 5. Os limites

- **Uma execução por combinação.** As tendências (o modelo pesa mais; o harness ajuda mais na manutenção) são as
  conclusões; diferenças de 1 ou 2 execuções entre braços podem ser acaso.
- **A correção desta rodada foi mais leve que a calibrada:** effort `medium` e o comportamento julgado pela leitura
  do código, sem chamar a API. O "em parte" do Haiku e do Sonnet 4.5 na construção precisa ser conferido com o
  serviço no ar.
- **A skill e o `CLAUDE.md` foram escritos por quem conhece as tarefas,** o que tende a favorecer os braços com
  harness; isso está declarado no `TCC_V1/skills/README.md`.

## 6. Custo e onde estão os dados

- As 90 execuções custaram US$ 73,06 em equivalente de API (Sonnet 5.5 12,15; Haiku 19,27; Sonnet 4.5 41,64). A
  correção custou US$ 31,07.
- Os projetos: `TCC_V1/<construction|maintenance>/runs/<cenário>/<braço>-<modelo>/workspace/`.
- A correção de cada projeto, com a explicação de como foi implementado e por que o julgamento:
  `TCC_V1/evaluation/ai/<pesquisa>/<pattern>/runs/<cenário>-<braço>/opus-5-5-medium/<código>/answer.md` (o
  `key.json` ao lado diz qual código é qual modelo).

## 7. Cada execução

Uma linha por execução: o resultado e o que o agente fez, o comportamento (pela leitura do código), o exagero, se
abriu a skill (no N2) e o tempo. A correção completa de cada uma está em `TCC_V1/evaluation/ai/`.

### Construção

**01 Strategy**

- Haiku 4.5 N0: ❌ três `switch` sobre o plano (comportamento em parte, 7 min)
- Haiku 4.5 N1: ✅ classe abstrata, uma subclasse por plano (comportamento em parte, 8 min)
- Haiku 4.5 N2: ✅ interface, uma classe por plano (comportamento em parte, abriu a skill, 11 min)
- Sonnet 4.5 N0: ❌ `if`/`switch` sobre o plano (comportamento em parte, 8 min)
- Sonnet 4.5 N1: ✅ interface, uma classe por plano, num mapa (comportamento em parte, 11 min)
- Sonnet 4.5 N2: ✅ interface, uma classe por plano, num `EnumMap` (comportamento em parte, abriu a skill, 10 min)
- Sonnet 5.5 N0: ✅ interface, uma classe por plano, num mapa (comportamento segue, 4 min)
- Sonnet 5.5 N1: ✅ classe abstrata, uma subclasse por plano (comportamento segue, 4 min)
- Sonnet 5.5 N2: ✅ interface, uma classe por plano, num mapa (comportamento segue, abriu a skill, 3 min)

**03 Observer**

- Haiku 4.5 N0: ✅ eventos e um ouvinte por interessado (comportamento em parte, 10 min)
- Haiku 4.5 N1: ✅ um ouvinte por interessado (comportamento em parte, 6 min)
- Haiku 4.5 N2: ⚠️ ouvintes, mas o hóspede ficou de fora (comportamento em parte, abriu a skill, 9 min)
- Sonnet 4.5 N0: ✅ barramento de eventos, um ouvinte por interessado (comportamento em parte, 10 min)
- Sonnet 4.5 N1: ⚠️ ouvintes, mas um notificador central monta todos os e-mails (comportamento em parte, 11 min)
- Sonnet 4.5 N2: ✅ um ouvinte por interessado, lista vinda de fora (comportamento em parte, **com exagero**, abriu a skill, 15 min)
- Sonnet 5.5 N0: ✅ ouvintes registrados no `main` (comportamento segue, 4 min)
- Sonnet 5.5 N1: ✅ ouvintes vindos de fora (comportamento segue, **com exagero**, 5 min)
- Sonnet 5.5 N2: ✅ Observer completo (comportamento segue, abriu a skill, 5 min)

**05 Factory Method**

- Haiku 4.5 N0: ✅ fábrica concentrada (comportamento em parte, 8 min)
- Haiku 4.5 N1: ✅ fábrica concentrada (comportamento em parte, 6 min)
- Haiku 4.5 N2: ✅ fábrica com uma classe por documento (comportamento em parte, **com exagero**, abriu a skill, 18 min)
- Sonnet 4.5 N0: ✅ fábrica concentrada (comportamento em parte, 10 min)
- Sonnet 4.5 N1: ⚠️ documento por tipo, mas o título escolhido por `instanceof` fora (comportamento em parte, 13 min)
- Sonnet 4.5 N2: ⚠️ fábrica, mas o título escolhido por `instanceof` fora (comportamento em parte, abriu a skill, 10 min)
- Sonnet 5.5 N0: ✅ método polimórfico no tipo de hóspede (comportamento segue, 4 min)
- Sonnet 5.5 N1: ✅ método polimórfico no tipo de hóspede (comportamento segue, 3 min)
- Sonnet 5.5 N2: ✅ método polimórfico no tipo de hóspede (comportamento segue, abriu a skill, 5 min)

**07 Decorator**

- Haiku 4.5 N0: ❌ três `if` dentro do envio (comportamento em parte, 8 min)
- Haiku 4.5 N1: ❌ três `if` dentro do envio (comportamento em parte, 10 min)
- Haiku 4.5 N2: ⚠️ corrente de regras, mas sempre as três, em qualquer modo (comportamento em parte, **com exagero**, abriu a skill, 8 min)
- Sonnet 4.5 N0: 🔷 lista de regras, modo escolhido na montagem (comportamento em parte, 9 min)
- Sonnet 4.5 N1: 🔷 lista de regras, modo escolhido na montagem (comportamento em parte, 11 min)
- Sonnet 4.5 N2: 🔷 lista de regras, modo escolhido na montagem (comportamento em parte, abriu a skill, 11 min)
- Sonnet 5.5 N0: 🔷 lista de regras com escopo por instalação (comportamento segue, 5 min)
- Sonnet 5.5 N1: 🔷 lista de regras, modo escolhido na montagem (comportamento segue, 6 min)
- Sonnet 5.5 N2: ✅ embrulhos empilhados conforme o modo (comportamento segue, abriu a skill, 4 min)

**09 Template Method**

- Haiku 4.5 N0: ❌ quatro relatórios inteiros, título copiado (comportamento em parte, 8 min)
- Haiku 4.5 N1: ❌ quatro relatórios inteiros, título copiado (comportamento em parte, 10 min)
- Haiku 4.5 N2: ❌ quatro relatórios inteiros, título copiado (comportamento em parte, não abriu a skill, 8 min)
- Sonnet 4.5 N0: ⚠️ só um utilitário comum; passos repetidos (comportamento em parte, 10 min)
- Sonnet 4.5 N1: ✅ classe abstrata com o método-modelo (comportamento em parte, 13 min)
- Sonnet 4.5 N2: ✅ classe abstrata com o método-modelo `final` (comportamento em parte, abriu a skill, 13 min)
- Sonnet 5.5 N0: 🔷 escritor de relatório comum (comportamento segue, 5 min)
- Sonnet 5.5 N1: 🔷 escritor de relatório comum (comportamento segue, 4 min)
- Sonnet 5.5 N2: ✅ classe abstrata com o método-modelo `final` (comportamento segue, abriu a skill, 3 min)

### Manutenção

**01 Strategy**

- Haiku 4.5 N0: ❌ os `switch` da base cresceram (comportamento segue, 7 min)
- Haiku 4.5 N1: ❌ três `switch` sobre o plano (comportamento segue, 7 min)
- Haiku 4.5 N2: ❌ `switch` da base ampliados (comportamento segue, não abriu a skill, 7 min)
- Sonnet 4.5 N0: ❌ `if`/`switch` da base ampliados (comportamento segue, 9 min)
- Sonnet 4.5 N1: ✅ interface, uma classe por plano; `switch` removidos (comportamento segue, 9 min)
- Sonnet 4.5 N2: ✅ interface, uma classe por plano, num mapa (comportamento segue, abriu a skill, 11 min)
- Sonnet 5.5 N0: ✅ o `enum` virou estratégia (comportamento segue, 3 min)
- Sonnet 5.5 N1: ✅ o `enum` virou estratégia (comportamento segue, 3 min)
- Sonnet 5.5 N2: ✅ interface, uma classe por plano (comportamento segue, abriu a skill, 2 min)

**03 Observer**

- Haiku 4.5 N0: ⚠️ ouvintes, mas o hóspede ficou de fora (comportamento segue, 8 min)
- Haiku 4.5 N1: ⚠️ ouvintes criados dentro do próprio serviço (comportamento segue, 6 min)
- Haiku 4.5 N2: ⚠️ ouvintes, mas o hóspede ficou de fora (comportamento segue, abriu a skill, 5 min)
- Sonnet 4.5 N0: ⚠️ ouvintes, mas o hóspede ficou de fora (comportamento segue, 8 min)
- Sonnet 4.5 N1: ✅ um ouvinte por interessado, registrados na montagem (comportamento segue, 11 min)
- Sonnet 4.5 N2: ✅ um ouvinte por interessado, lista vinda de fora (comportamento segue, abriu a skill, 8 min)
- Sonnet 5.5 N0: ✅ um ouvinte por interessado, o hóspede incluído (comportamento segue, 2 min)
- Sonnet 5.5 N1: ⚠️ ouvintes, mas o hóspede ficou de fora (comportamento segue, 2 min)
- Sonnet 5.5 N2: ✅ um ouvinte por interessado, lista vinda de fora (comportamento segue, abriu a skill, 2 min)

**05 Factory Method**

- Haiku 4.5 N0: ❌ `if/else` dentro do comprovante (comportamento em parte, **com exagero**, 9 min)
- Haiku 4.5 N1: ✅ escolha concentrada num ponto só (comportamento em parte, 6 min)
- Haiku 4.5 N2: ✅ escolha concentrada num ponto só (comportamento em parte, não abriu a skill, 9 min)
- Sonnet 4.5 N0: ✅ escolha concentrada no comprovante (comportamento em parte, 9 min)
- Sonnet 4.5 N1: ✅ escolha concentrada num método (comportamento segue, 7 min)
- Sonnet 4.5 N2: ✅ fábrica com um gerador por documento (comportamento em parte, abriu a skill, 12 min)
- Sonnet 5.5 N0: ✅ fábrica concentrada (comportamento segue, 2 min)
- Sonnet 5.5 N1: ✅ fábrica concentrada (comportamento segue, 3 min)
- Sonnet 5.5 N2: ✅ método polimórfico no tipo de hóspede (comportamento segue, abriu a skill, 3 min)

**07 Decorator**

- Haiku 4.5 N0: ❌ três `if` dentro do envio (comportamento em parte, 8 min)
- Haiku 4.5 N1: ❌ três `if` dentro do envio (comportamento em parte, 9 min)
- Haiku 4.5 N2: ❌ três `if` dentro do envio (comportamento em parte, não abriu a skill, 5 min)
- Sonnet 4.5 N0: ⚠️ lista de regras, mas fixa dentro do envio (comportamento em parte, 7 min)
- Sonnet 4.5 N1: ⚠️ lista de regras, mas as regras olham o modo (comportamento em parte, 10 min)
- Sonnet 4.5 N2: ⚠️ lista de regras, mas o envio decide o modo (comportamento em parte, abriu a skill, 8 min)
- Sonnet 5.5 N0: 🔷 lista de regras com escopo, montada fora (comportamento segue, 3 min)
- Sonnet 5.5 N1: 🔷 lista de regras, modo declarado fora (comportamento segue, 2 min)
- Sonnet 5.5 N2: ✅ embrulhos empilhados por modo e instalação (comportamento segue, abriu a skill, 2 min)

**09 Template Method**

- Haiku 4.5 N0: ❌ quatro relatórios inteiros, título copiado (comportamento em parte, 6 min)
- Haiku 4.5 N1: ❌ quatro relatórios inteiros, título copiado (comportamento em parte, 6 min)
- Haiku 4.5 N2: ❌ quatro relatórios inteiros, título copiado (comportamento em parte, não abriu a skill, 3 min)
- Sonnet 4.5 N0: ⚠️ classe base sem método-modelo (comportamento segue, 5 min)
- Sonnet 4.5 N1: ⚠️ classe comum só junta o texto; passos repetidos (comportamento segue, 5 min)
- Sonnet 4.5 N2: ✅ classe abstrata com o método-modelo `final` (comportamento segue, abriu a skill, 6 min)
- Sonnet 5.5 N0: 🔷 escritor de relatório comum (comportamento segue, 2 min)
- Sonnet 5.5 N1: ✅ classe abstrata com o método-modelo `final` (comportamento segue, 13 min)
- Sonnet 5.5 N2: ✅ classe abstrata com o método-modelo `final` (comportamento segue, abriu a skill, 3 min)
