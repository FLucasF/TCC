# Resumo da compra (checkout)

Serviço que calcula o resumo que o site mostra antes de o cliente confirmar o
pedido. Sem banco de dados: as opções de entrega, os cupons, os níveis do
clube e as formas de pagamento ficam no próprio código.

## Como rodar

```bash
mvn verify          # compila e roda todos os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

Exemplo de chamada:

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
               {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}],
       "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
       "parcelas":1,"nivelClube":"BRONZE","regiao":"NORTE"}'
```

Respostas: `200` com o resumo quando dá certo; `422` com `{"erro":"CODIGO"}`
quando o pedido é recusado (e `400` com `PEDIDO_INVALIDO` quando o JSON nem
chega a formar um pedido, por exemplo texto onde devia vir número).

## Como o código está organizado

A sequência do cálculo é a mesma para todo pedido e mora num lugar só:
`dominio/CalculadoraResumo`. Ela faz as seis etapas na ordem combinada
(produtos, desconto, frete, seguro, total, ajuste do pagamento) e confere cada
recusa assim que o valor de que ela depende fica pronto.

O que muda de caso para caso está separado por assunto, cada caso numa classe
só dele:

| Assunto | Contrato | Casos de hoje |
|---|---|---|
| Entrega | `entrega/ModalidadeEntrega` | `Economica`, `Expressa`, `RetiradaLoja`, `Motoboy` |
| Cupons | `cupom/Cupom` | `Bemvindo10`, `Menos50`, `FreteGratis`, `Leve3Pague2` |
| Clube | `clube/NivelClube` | `Bronze`, `Prata`, `Ouro` |
| Pagamento | `pagamento/FormaPagamento` | `Pix`, `Cartao`, `Boleto` |

A escolha do caso pelo código que o site envia é sempre a mesma coisa, então
também mora num lugar só: `catalogo/Catalogo`. Não existe cadeia de `if`
decidindo qual opção usar — o catálogo acha a opção pelo código e, quando o
código não existe, recusa o pedido com o erro daquele assunto.

O seguro **não** é um desses casos: a conta é a mesma em todas as regiões, só
a porcentagem muda. Por isso é apenas um número em `dominio/Regiao`.

O arredondamento para centavos (meio para o par) acontece em
`dominio/Dinheiro`, usado por todas as etapas.

## Como entra uma opção nova

**Transportadora nova** — crie uma classe em `entrega` que implementa
`ModalidadeEntrega`, com `@Component`:

```java
@Component
public class Drone implements ModalidadeEntrega {
    public String codigo()                     { return "DRONE"; }
    public BigDecimal custo(BigDecimal pesoKg) { return new BigDecimal("40.00"); }
    public int prazoDias()                     { return 1; }
    public boolean atende(BigDecimal pesoKg)   { return pesoKg.compareTo(BigDecimal.TWO) <= 0; }
}
```

Pronto: ela passa a valer. Nada mais no projeto muda.

**Promoção nova**, **nível novo do clube** e **forma de pagamento nova**
funcionam do mesmo jeito: uma classe nova em `cupom`, `clube` ou `pagamento`,
implementando o contrato do assunto, com `@Component`.

## Testes

- `ExemplosDoFinanceiroTest` — os cinco exemplos conferidos pelo financeiro,
  mais o do anexo, campo por campo.
- `RegrasDeCalculoTest` — entrega, cupons, clube, seguro e pagamento, com os
  casos de limite (motoboy com 5 kg exatos, brinde em R$ 500,00, boleto em
  R$ 1.000,00, `MENOS50` em R$ 300,00).
- `RecusasTest` — os dez códigos de recusa e a ordem de conferência.
- `ResumoHttpTest` — o formato do JSON de ida e volta.
- `DinheiroTest` — o arredondamento meio para o par.
