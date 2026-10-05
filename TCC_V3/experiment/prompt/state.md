Oi! Tenho uma loja online de roupas e acessórios e preciso do serviço que acompanha cada pedido depois que o cliente finaliza a compra. Não sou programador, então vou explicar como o negócio funciona e o que eu preciso que aconteça. A pasta está vazia, então é montar tudo do começo.

## Como um pedido anda

Todo pedido começa **aguardando pagamento**. Depois disso ele vai passando por etapas, e em cada etapa só algumas coisas podem acontecer com ele. A equipe (ou o próprio site) pede uma ação, e o serviço confere se aquela ação vale naquele momento.

As situações de um pedido são estas:

| Situação | O que quer dizer |
|---|---|
| `AGUARDANDO_PAGAMENTO` | o cliente finalizou, mas o pagamento ainda não caiu |
| `PAGO` | o pagamento caiu |
| `EM_SEPARACAO` | o estoque está separando os produtos |
| `ENVIADO` | o pedido saiu com a transportadora |
| `ENTREGUE` | o pedido chegou ao cliente |
| `CANCELADO` | o pedido foi cancelado |
| `DEVOLVIDO` | o cliente devolveu o pedido depois de receber |

E as ações são estas: `PAGAR`, `SEPARAR`, `ENVIAR`, `ENTREGAR`, `CANCELAR` e `DEVOLVER`.

## O que cada ação faz

**Aguardando pagamento.** `PAGAR` leva para `PAGO`. `CANCELAR` leva para `CANCELADO`, e não há nada para devolver ao cliente, porque ele ainda não pagou.

**Pago.** `SEPARAR` leva para `EM_SEPARACAO`. `CANCELAR` leva para `CANCELADO` e o cliente recebe de volta o valor total do pedido.

**Em separação.** `ENVIAR` leva para `ENVIADO`. `CANCELAR` ainda é possível, mas dá mais trabalho: leva para `CANCELADO`, o cliente recebe de volta o valor total **menos R$ 15,00** de taxa de separação (se o pedido for menor que a taxa, o reembolso é zero, nunca negativo), e os produtos que já tinham sido separados **voltam para o estoque na hora**.

**Enviado.** Só `ENTREGAR`, que leva para `ENTREGUE`. Depois que o pedido saiu não dá mais para cancelar: se o cliente não quiser, ele devolve depois que receber.

**Entregue.** Só `DEVOLVER`, que leva para `DEVOLVIDO`. O cliente recebe de volta **só o valor dos produtos**, o frete não volta. E a gente precisa buscar o pacote na casa dele, então a devolução **agenda uma coleta**. Os produtos só voltam para o estoque quando a coleta chega, não na hora.

**Cancelado e devolvido.** Acabou: nenhuma ação vale mais.

Qualquer ação que não esteja descrita acima para a situação atual não pode acontecer, e o pedido fica como estava.

O processo muda de vez em quando. Já aconteceu de a logística pedir uma etapa nova no meio do caminho, e cada etapa tem as suas regras do que pode e do que não pode.

## O que o cliente vê

No site, cada situação aparece para o cliente com um texto:

| Situação | Texto para o cliente |
|---|---|
| `AGUARDANDO_PAGAMENTO` | Aguardando pagamento |
| `PAGO` | Pagamento confirmado |
| `EM_SEPARACAO` | Separando seus produtos |
| `ENVIADO` | A caminho |
| `ENTREGUE` | Entregue |
| `CANCELADO` | Cancelado |
| `DEVOLVIDO` | Devolvido |

É só o texto que muda de uma situação para outra, não tem regra nenhuma por trás.

## Observações do financeiro

- O valor total do pedido é o valor dos produtos mais o frete.
- Todo valor em dinheiro tem 2 casas decimais. As contas daqui são só somas e subtrações, então não precisa arredondar nada.
- O reembolso de um pedido que não foi cancelado nem devolvido é zero.

---

## Anexo: combinado com o desenvolvedor do site

O site vai chamar o serviço assim. Por favor, siga exatamente estes nomes e formatos. Não use banco de dados: os pedidos podem ficar guardados na memória enquanto o serviço estiver rodando.

### Criar um pedido

`POST /pedidos`

```json
{ "valorProdutos": 200.00, "frete": 20.00 }
```

Resposta `201` com o pedido, na situação `AGUARDANDO_PAGAMENTO`.

### Pedir uma ação

`POST /pedidos/{id}/acoes`

```json
{ "acao": "PAGAR" }
```

Resposta `200` com o pedido depois da ação.

### Consultar um pedido

`GET /pedidos/{id}`

Resposta `200` com o pedido.

### O pedido, nas três respostas

```json
{
  "id": "1",
  "situacao": "EM_SEPARACAO",
  "descricao": "Separando seus produtos",
  "valorProdutos": 200.00,
  "frete": 20.00,
  "valorTotal": 220.00,
  "valorReembolsado": 0.00,
  "estoqueDevolvido": false,
  "coletaAgendada": false,
  "historico": ["AGUARDANDO_PAGAMENTO", "PAGO", "EM_SEPARACAO"]
}
```

- `id` é um texto gerado pelo serviço, diferente para cada pedido.
- `historico` lista todas as situações por onde o pedido passou, em ordem, começando por `AGUARDANDO_PAGAMENTO` e terminando na atual.
- `estoqueDevolvido` fica `true` só quando os produtos voltam para o estoque na hora (o cancelamento em separação). `coletaAgendada` fica `true` só na devolução.
- Todos os valores em dinheiro com 2 casas decimais.

### Erros

A resposta de erro é sempre `{ "erro": "CODIGO" }`. Verificar nesta ordem e devolver o primeiro erro encontrado:

| Ordem | Situação | Código | Status |
|---|---|---|---|
| 1 | Ao criar: valor dos produtos zero, negativo ou ausente, ou frete negativo ou ausente | `PEDIDO_INVALIDO` | 400 |
| 2 | Pedido com esse `id` não existe | `PEDIDO_NAO_ENCONTRADO` | 404 |
| 3 | Ação que não existe ou não informada | `ACAO_INVALIDA` | 400 |
| 4 | Ação existe, mas não vale na situação atual do pedido | `ACAO_NAO_PERMITIDA` | 409 |

Quando uma ação dá erro, o pedido não muda nada.

### Exemplos conferidos pelo financeiro

**Exemplo 1**: cria com produtos 200,00 e frete 20,00; `PAGAR`, `SEPARAR`, `CANCELAR`
→ situação `CANCELADO` · total 220,00 · reembolso 205,00 · estoque devolvido sim · coleta não · histórico AGUARDANDO_PAGAMENTO, PAGO, EM_SEPARACAO, CANCELADO

**Exemplo 2**: cria com produtos 150,00 e frete 12,50; `PAGAR`, `SEPARAR`, `ENVIAR`, `ENTREGAR`, `DEVOLVER`
→ situação `DEVOLVIDO` · total 162,50 · reembolso 150,00 · estoque devolvido não · coleta sim

**Exemplo 3**: cria com produtos 80,00 e frete 0,00; `CANCELAR`
→ situação `CANCELADO` · reembolso 0,00 · estoque devolvido não · coleta não

**Exemplo 4**: cria com produtos 250,00 e frete 25,00; `PAGAR`, `CANCELAR`
→ situação `CANCELADO` · reembolso 275,00 · estoque devolvido não

**Exemplo 5**: cria com produtos 10,00 e frete 3,00; `PAGAR`, `SEPARAR`, `CANCELAR`
→ situação `CANCELADO` · total 13,00 · reembolso 0,00 (a taxa de 15,00 é maior que o total) · estoque devolvido sim

**Exemplo 6**: cria com produtos 99,90 e frete 15,00; `PAGAR`, `ENVIAR`
→ o `ENVIAR` dá erro `ACAO_NAO_PERMITIDA` (409), porque o pedido ainda não foi separado · o pedido continua `PAGO`

**Exemplo 7**: um pedido `ENVIADO` recebe `CANCELAR` → erro `ACAO_NAO_PERMITIDA` (409), e continua `ENVIADO`

**Exemplo 8**: qualquer pedido recebe `{ "acao": "TROCAR" }` → erro `ACAO_INVALIDA` (400)

---

## Observações do time técnico

- Não use banco de dados.
- Use Java 21 e Spring Boot 4.1.1.
- O projeto precisa funcionar com `mvn verify`.

Quando terminar, responda apenas "CONCLUÍDO".
