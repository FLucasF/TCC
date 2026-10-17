# O serviço com defeitos plantados

`runs/PLANTED-01/` é uma cópia do `runs/pilots/OPUS55-N0-03` (o projeto, o enunciado que ele recebeu, o relatório de
arquitetura e a foto do banco) com defeitos conhecidos, um ou dois por dimensão. Serve para conferir uma correção:
a IA corrige o PLANTED-01 como corrigiria qualquer execução, e a resposta tem de bater com o
[`esperado.md`](esperado.md). A linha do PIT dele está em `pit.csv`, nesta pasta.

## Os defeitos

| dimensão | o defeito | onde | o item que muda |
|---|---|---|---|
| padrões | o brinde do GOLD decidido por um `if` que nomeia o GOLD no calculador; o original pergunta ao nível (`tier.gift(subtotal)`) | `CheckoutCalculator.java:104` | P4-local `espalhado`, P4-selecao `condicional_no_calculo` |
| arquitetura | uma classe de negócio, no pacote do pedido, que guarda um controller e devolve um `ResponseEntity` | `order/OrderReceipts.java` (nova) | A1 `nao_atende` |
| banco | sem o `CHECK (stock >= 0)` | `schema.sql:6` e a foto | B7 `parcial` |
| banco | o total final em `DOUBLE PRECISION` | `schema.sql:30` e a foto | B4 `nao_atende` |
| banco | o item do pedido sem a chave estrangeira para o produto | `schema.sql:42` e a foto | B2 `parcial` |
| banco | o `created_at` do pedido sem fuso (`TIMESTAMP`) | `schema.sql:20` e a foto | B5 `nao_atende` |
| testes | todas as asserções retiradas dos testes, com os números do PIT dos mesmos testes sem asserções (`analysis/pilots/pit-without-assertions.csv`) | `src/test/` e `pit.csv` | T2 `superficiais` |

Os defeitos do banco estão no `schema.sql` **e** na foto (`suite/database-schema.json`), para o código e o banco
dizerem a mesma coisa. O relatório de arquitetura foi gerado de novo com a classe nova. Os testes foram tirados das
asserções pela mesma ferramenta da medida do PIT (`without_assertions`, corrigida em 17/10), que também apaga os
comentários deles.

## O resultado de 17/10/2026

Corrigido pelo Claude Opus 5.5 (effort `high`), uma dimensão por vez: **21 de 21** itens iguais ao esperado. Ele achou
todos os defeitos e não se confundiu com o que o original já tinha. Achou também, sem que fosse plantado, a
`PurchaseHistory` (uma interface com uma implementação só, item E) e o `OrderReceipts` como código sem uso.

Com as réguas do formato escrito (sem lista fechada), a dimensão do banco foi corrigida de novo, sem script: as 6
conclusões do banco iguais ao esperado (`../batches/written-1/`).

## O resultado com a correção alinhada (17/10/2026)

Com a correção alinhada com a da manutenção (as oito dimensões, o comportamento testado pela própria IA, a resposta em
texto com uma linha de conclusão por item), Claude Opus 5.5 (effort `high`) achou **todos os defeitos plantados**, e
nada do que o original já tinha foi confundido com eles. Em três itens, porém, a palavra da conclusão foi mais branda
que a do esperado: A1, B4 e B5 saíram `em parte`, e o esperado é `não segue`. Ele viu o defeito nos três, mas pesou o
resto do item (um único `OrderReceipts`, uma única coluna em `double precision`, uma única coluna sem fuso). Na
primeira tentativa, o Factory saiu sem as linhas de conclusão, porque as réguas ainda mandavam salvar a correção em
`correcao/…` e o F não tinha palavra de conclusão. Depois do conserto, o F saiu `não se aplica` (contêiner, criação
concentrada). O comportamento achou, rodando, o efeito do `created_at` sem fuso: numa segunda instância com a JVM no
fuso de São Paulo, um pagamento com 47 horas foi recusado por prazo vencido. Saída em
`../../../analysis/planted/judge/opus-5-5-high/`.

Depois disso, as réguas ganharam duas regras: no A1, no B4 e no B5, um único ponto errado já é `não segue` (a pergunta
é de sim ou não), e no comportamento um ponto que o enunciado não decide vai para as "Dúvidas" sem mudar a conclusão.
Corrigidas de novo a arquitetura, o banco e o comportamento, as conclusões ficaram **21 de 21 iguais ao esperado**. Duas
mudanças fora do esperado: o A2 passou de `em parte` para `não segue` com os mesmos achados (os dois laços de
comportamento, `order`↔`catalog` e `order`↔`customer`), e o fuso do servidor no C6 foi para as "Dúvidas", porque o
enunciado não diz em que fuso o serviço roda.
