---
name: review-architecture
description: Revisa a arquitetura de um projeto Java da revisão de qualidade - os pacotes, as camadas (HTTP, regras, dados), as dependências entre eles e os ciclos - a partir do jdeps (script measure_dependencies.sh) e da leitura do código. Use na coluna "Arquitetura" da revisão.
---

# A arquitetura

Esta skill é material de trabalho para a revisão em andamento: rode o script, leia o resultado e o código, e escreva
a seção "Arquitetura" do `review.md`.

## A medida

```bash
bash .claude/skills/review-architecture/measure_dependencies.sh <projeto ou final>
```

Grava `measures/dependencies.md`: os pacotes (classes e linhas), as dependências entre eles, quem depende de quem
(quantos), os ciclos (pelo `tsort`), as bibliotecas que cada pacote usa (Javalin, JDBC, Jackson, Flyway, pool) e as
maiores classes. Na manutenção, rode também com `base` (grava `measures/dependencies-base.md`) e compare: o que conta
é o que o agente mudou.

## O que olhar

O sistema é pequeno (cerca de mil linhas), e a pergunta é se as responsabilidades estão separadas o bastante para ele
crescer sem que tudo mexa em tudo. Leia, com os números ao lado:

- **A borda HTTP:** as rotas (o Javalin) só leem a requisição, chamam a regra e escrevem a resposta? Ou a regra de
  negócio (preço, situação da reserva, cancelamento) e o SQL moram dentro dos handlers?
- **As regras:** ficam em classes que não conhecem o Javalin nem o JDBC? Um serviço que recebe um `Context` do
  Javalin, ou que monta SQL, mistura camadas.
- **Os dados:** o acesso ao banco fica isolado (em classes de acesso a dados) ou espalhado pelo código?
- **A montagem:** as peças são criadas e ligadas num lugar só (o `main` ou uma classe de montagem), ou cada classe
  cria as suas dependências por dentro (um `new` do acesso ao banco dentro do serviço, um `static` global)?
- **As dependências:** apontam da borda para as regras e das regras para os dados? Um ciclo entre pacotes é sinal de
  responsabilidade no lugar errado; um ciclo pequeno entre dois pacotes vizinhos pesa menos que vários.
- **O tamanho:** uma classe muito maior que as outras, que faz de tudo, é o lugar onde o próximo pedido vai doer.

Não peça cerimônia: num sistema deste tamanho, não ter interfaces para cada classe, nem camadas de "portas e
adaptadores", não é defeito. Estrutura sem necessidade conta no exagero do design de baixo nível, não aqui.

## A nota

- `bom`: a borda HTTP fina, as regras sem Javalin e sem SQL, o acesso ao banco isolado, a montagem num lugar, sem
  ciclo (ou só um ciclo que já existia na base, na manutenção).
- `com problemas`: uma mistura localizada (um handler com uma regra, um serviço com um pedaço de SQL), um ciclo novo
  entre dois pacotes, ou uma classe grande demais.
- `ruim`: as camadas não existem (regra e SQL dentro das rotas, tudo num pacote ou numa classe), as regras dependem do
  Javalin, ou vários ciclos.

Na seção, dê os números (pacotes, dependências, ciclos) e o porquê com `arquivo:linha`.
