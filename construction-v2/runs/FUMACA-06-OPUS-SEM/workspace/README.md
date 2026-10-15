# Serviço de resumo da compra

Calcula o resumo do checkout: produtos, cupom, frete, prazo, ajuste da forma de pagamento e valor final.

## Como rodar

```bash
mvn verify        # compila e roda os testes
mvn spring-boot:run   # sobe o serviço em http://localhost:8080
```

## Endpoint

`POST /checkout/resumo` (JSON), conforme combinado com o desenvolvedor do site.
Erros voltam com status 400 e corpo `{"erro":"CODIGO"}`.

## Onde mexer quando entrar coisa nova

- **Nova opção de entrega**: criar uma classe em `entrega/` que implemente `ModalidadeEntrega`
  com `@Component` (custo, prazo e, se precisar, a regra de `atende`). Ela entra sozinha no catálogo.
- **Novo cupom**: classe em `cupom/` implementando `Cupom` com `@Component`.
- **Nova forma de pagamento**: classe em `pagamento/` implementando `FormaPagamento` com `@Component`.

Regras de dinheiro ficam em `dominio/Dinheiro.java` (centavos, arredondamento meio para o par)
e a ordem do cálculo e das validações em `dominio/CalculadoraDeResumo.java`.
