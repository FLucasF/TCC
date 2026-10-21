---
name: review-tests
description: Revisa os testes que o próprio agente escreveu num projeto Java da revisão de qualidade - quantos rodam e passam, a cobertura de linhas e de ramos (JaCoCo pela linha de comando, script measure_tests.sh) e, pela leitura, se eles conferem as regras pedidas com valores. Use na coluna "Testes" da revisão.
---

# Os testes

Esta skill é material de trabalho para a revisão em andamento: rode o script, leia o resultado e os testes, e escreva
a seção "Testes" do `review.md`.

## A medida

```bash
bash .claude/skills/review-tests/measure_tests.sh <projeto ou final>
```

Roda o `mvn verify` do projeto com o agente do JaCoCo, sem mexer no `pom.xml`, contra um banco vazio só dele, e
grava `measures/tests.md`: os testes que rodaram, falharam e foram pulados, os métodos de teste escritos no código, a
cobertura de linhas e de ramos (no total e por pacote), os testes que falharam e os tipos de teste (pela API, contra
o banco, com mocks). O log inteiro fica em `measures/tests-maven.log`. Na manutenção, rode também com `base` (grava
`measures/tests-base.md`) e compare.

## O que olhar

Os números dizem quanto código os testes executam; não dizem se eles conferem alguma coisa. Por isso leia os testes:

- **As regras pedidas estão testadas, com valores?** O preço com a temporada e o arredondamento, os planos, o
  cancelamento e a devolução, as situações da reserva, as recusas (os códigos de erro) e, sobretudo, a parte nova do
  cenário. Um teste que só confere `assertNotNull`, ou só o status 200, executa o código sem conferir a regra.
- **Faça a pergunta da mutação** em duas ou três regras da parte nova: se alguém trocasse 0.12 por 0.21, ou `>` por
  `>=`, ou apagasse uma linha, algum teste falharia? Diga quais regras ficariam sem rede.
- **Os testes passam?** Falhas e erros contam; um teste desligado (`@Disabled`) também. Veja no log se a falha é do
  código ou do ambiente (por exemplo, um teste que precisa de Docker ou de um banco que não é o de `DB_URL`).
- **Em que nível:** só testes de unidade com mocks não veem o banco nem a API; testes só pela API não isolam a regra.
  Uma mistura é o esperado.
- **Na construção,** o texto não pede testes: o que se mede é a iniciativa e a qualidade do que o agente escreveu. **Na
  manutenção,** a base tinha 26 testes: conte se o agente testou o que mudou, se manteve os da base passando e se
  apagou ou enfraqueceu algum.

## A nota

- `bom`: os testes passam, conferem com valores as regras pedidas (inclusive a parte nova do cenário) e cobrem a maior
  parte do código das regras.
- `com problemas`: os testes existem e passam, mas deixam parte das regras novas sem conferir, têm asserções fracas em
  parte, ou alguns falham.
- `ruim`: não há testes (ou quase), muitos falham, não compilam, ou não conferem nada de verdade.

Na seção, dê os números e o porquê com `arquivo:linha` (as regras testadas e as que ficaram sem teste).
