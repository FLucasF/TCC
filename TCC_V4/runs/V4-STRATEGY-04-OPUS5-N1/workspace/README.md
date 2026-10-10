# Servico de resumo da compra

Calcula o resumo do pedido no checkout (produtos, cupom, frete, seguro, clube e
forma de pagamento). Sem banco de dados: os cupons, as opcoes de entrega e as
formas de pagamento ficam fixos no codigo.

## Como rodar

- Testes e empacotamento: `mvn verify`
- Subir o servico: `mvn spring-boot:run` (porta 8080)

Exemplo de chamada:

```
curl -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}],
       "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
       "parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}'
```

Quando o pedido e recusado, a resposta e so `{"erro":"CODIGO"}`. O enunciado nao
disse qual codigo HTTP usar nesses casos: o servico responde `400 Bad Request`
(nas recusas o corpo e sempre so o codigo do problema).

## Onde fica cada coisa

- `dominio/entrega`: uma classe por opcao de entrega (valor, prazo, limitacoes)
  e o catalogo com as opcoes de hoje. Opcao nova = classe nova + uma linha no
  catalogo `ModalidadesDeEntrega`.
- `dominio/cupom`: uma classe por regra de cupom e o catalogo `Cupons`.
- `dominio/pagamento`: uma classe por forma de pagamento (parcelamento, limites
  e ajuste no total) e o catalogo `FormasDePagamento`.
- `dominio/NivelClube`: cada nivel do clube com suas vantagens (credito, frete,
  brinde).
- `dominio/Regiao`: so a porcentagem do seguro muda por regiao, a conta e a mesma.
- `dominio/Centavos`: o arredondamento para centavos (meio para o par), num lugar so.
- `aplicacao/CalculadoraDeResumo`: a ordem do calculo e a ordem de conferencia
  dos problemas.
- `web`: o endereco `/checkout/resumo` e a resposta de recusa.
