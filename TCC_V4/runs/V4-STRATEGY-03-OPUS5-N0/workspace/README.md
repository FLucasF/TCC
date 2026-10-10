# Serviço de resumo da compra

Calcula o resumo que o site mostra antes do cliente confirmar o pedido.
Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Como rodar

```bash
mvn verify          # compila e roda todos os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

Exemplo de chamada:

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{
    "itens": [
      { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
      { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
    ],
    "modalidadeEntrega": "EXPRESSA",
    "cupom": "BEMVINDO10",
    "formaPagamento": "PIX",
    "parcelas": 1,
    "nivelClube": "OURO",
    "regiao": "SUDESTE"
  }'
```

Quando dá certo vem o resumo; quando o pedido é recusado vem só o código do
problema (`{"erro":"CUPOM_NAO_APLICAVEL"}`, por exemplo), com status 400.

## Onde fica cada regra

| Pasta | O que tem lá |
|---|---|
| `aplicacao/CalculadoraResumo` | a ordem do cálculo e a ordem das conferências de erro |
| `entrega/` | as opções de entrega (uma classe por opção) |
| `cupom/` | os cupons (uma classe por cupom) |
| `clube/` | os níveis do clube (uma classe por nível) |
| `pagamento/` | as formas de pagamento, inclusive os juros da tabela Price |
| `seguro/` | as regiões e o percentual do seguro de cada uma |
| `dominio/Dinheiro` | o arredondamento em centavos, "meio para o par" |

## Cadastrando coisas novas

Cada opção é uma classe anotada com `@Component`. Basta criar a classe nova:
ela entra no catálogo sozinho, sem mexer no resto do código.

- **Transportadora nova**: criar uma classe em `entrega/` que implemente
  `ModalidadeEntrega` (código, prazo, como cobra e, se tiver limitação,
  o `atende`, que faz o pedido cair em `MODALIDADE_INDISPONIVEL`).
- **Cupom novo**: criar uma classe em `cupom/` que implemente `Cupom`
  (código, desconto e, se tiver condição, o `aplicavel`).
- **Nível do clube novo**: criar uma classe em `clube/` que implemente
  `NivelClube` (crédito, frete grátis e brinde).
- **Forma de pagamento nova**: criar uma classe em `pagamento/` que implemente
  `FormaPagamento`.
- **Região nova**: acrescentar o valor no enum `seguro/Regiao` com o percentual.

## Testes

- `ResumoExemplosTest`: os cinco exemplos conferidos pelo financeiro, mais o
  exemplo do anexo e a conferência das 2 casas decimais.
- `ResumoRegrasTest`: frete grátis por cupom, brinde do ouro, cartão à vista,
  cartão em 12x e o limite de peso do motoboy.
- `ResumoRecusasTest`: os dez códigos de recusa e a ordem de conferência.
- `DinheiroTest`: o arredondamento "meio para o par".
