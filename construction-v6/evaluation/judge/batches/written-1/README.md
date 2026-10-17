# O primeiro teste do formato escrito (17/10/2026)

A primeira correção com as réguas que pedem feedback escrito, sem lista fechada e sem script: o Claude Opus 5.5
(effort `high`), dentro da imagem do experimento (`experimento-harness:v5`), corrigiu a dimensão do banco do
`PLANTED-01`. O container só via a pasta da execução, o CSV do PIT e as réguas, nos caminhos que o
`rubric/common.md` descreve; não via o `planted/esperado.md` nem o resto do repositório. O pedido foi uma frase só
(`pedido.txt`).

| arquivo | o que é |
|---|---|
| `pedido.txt` | o pedido, como o Lucas o escreveria |
| `PLANTED-01-banco.md` | a correção que ele escreveu (`correcao/banco.md`, na cópia da execução) |
| `PLANTED-01-banco-transcricao.jsonl` | a transcrição |

**Resultado:** os 6 itens do banco que o `planted/esperado.md` lista, todos com a mesma conclusão: B1 (as tabelas têm
chave primária), B2 (falta a chave do item para o produto), B3 (a `notification.order_id` sem índice), B4 (o
`final_total` em `double precision`), B5 (o `created_at` sem fuso) e B7 (o estoque protegido só pelo código). O
feedback explica por que cada ponto importa no enunciado, diz o que fica em dúvida (no B5, se o instante já sai
deslocado sem rodar) e termina com o que mudaria primeiro. Custo: US$ 0,73.

**Falta:** as outras três dimensões (padrões, arquitetura, testes) do mesmo teste, que ficaram para depois que a
janela de 5 horas da assinatura renovar.
