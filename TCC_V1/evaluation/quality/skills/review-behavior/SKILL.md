---
name: review-behavior
description: Revisa o comportamento de um projeto Java da revisão de qualidade chamando a API de verdade - um roteiro fixo por cenário (check_01.sh a check_09.sh) com os valores que o texto pede, a parte comum do sistema, os dados depois de reiniciar e duas reservas ao mesmo tempo. Use na coluna "Comportamento" da revisão.
---

# O comportamento

Esta skill é material de trabalho para a revisão em andamento: rode o roteiro do cenário, leia o resultado e escreva
a seção "Comportamento" do `review.md`.

## A medida

```bash
bash .claude/skills/review-behavior/check_NN.sh <projeto ou final>
```

Use o `check_` do número do cenário (`task.md` diz qual). O roteiro empacota o projeto, sobe o serviço contra um
PostgreSQL vazio, chama a API com `curl` e grava `measures/behavior.md`, uma linha por conferência (`OK` ou `FALHOU`,
com o esperado e o que veio), em quatro partes:

- **a parte comum do enunciado:** os quartos, o preço com a temporada e o arredondamento, as recusas e os códigos de
  erro, a confirmação e o e-mail, o check-in e o check-out, os cancelamentos e as devoluções, os dois relatórios;
- **os dados depois de reiniciar:** o serviço sobe de novo sobre o mesmo banco, e as reservas e as mensagens
  continuam lá;
- **o cenário:** os valores e as regras que o texto do cenário pede;
- **duas reservas ao mesmo tempo:** dez rodadas de dois pedidos simultâneos para o mesmo quarto e as mesmas noites;
  em cada uma, só um pode ser criado.

O log do serviço fica em `measures/behavior-service.log`.

## O que olhar

Cada `FALHOU` precisa de uma leitura: é um erro do agente, ou o texto deixava aquilo em aberto? Por exemplo, a
ordem dos e-mails não importa no cenário 03, e o roteiro não depende dela; mas um valor diferente do que o texto dá
(R$ 686,22, R$ 410,00, o ISS de R$ 38,99) é erro. Quando a falha não for clara, chame a API você mesmo (`curl`) com o
serviço de pé (`java -jar` com `DB_URL`, `DB_USER`, `DB_PASSWORD` e `PORT`), e derrube o serviço no fim.

Uma falha na parte comum que vem de uma escolha razoável num ponto que o texto não fechou não pesa como erro; diga
qual foi. Uma conferência que passa sozinha na base (uma recusa, por exemplo) não prova que a regra nova existe.

Esta coluna mede o que o sistema faz pela API, do jeito que o texto descreve, a partir de um banco vazio. Cada defeito
pesa numa coluna só: um defeito do banco (SQL montado com dado de fora, uma migração que quebra um banco que já tinha
dados, um tipo errado) pesa na coluna do banco, e aqui só conta se aparecer numa chamada que o texto descreve. Se você
achar um desses fora do roteiro, conte-o na coluna dele e cite-o aqui sem mudar a nota.

## A nota

- `bom`: tudo passa, ou só falham pontos que o texto deixava em aberto (explicados).
- `com problemas`: a parte comum funciona, mas parte do cenário falha, ou só as reservas simultâneas falham.
- `ruim`: o serviço não sobe, a parte comum quebra, os dados somem ao reiniciar, ou a maior parte do cenário falha.

Na seção, dê os números (quantas passaram, por parte) e o porquê de cada falha que pesou.
