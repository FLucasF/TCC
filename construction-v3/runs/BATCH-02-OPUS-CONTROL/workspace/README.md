# Serviço de resumo da compra

Java 21 + Spring Boot 4.1.1, sem banco de dados.

```bash
mvn verify          # compila e roda os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

`POST /checkout/resumo` devolve o resumo (200) ou `{"erro": "CODIGO"}` (400).

## Onde mexer quando a loja muda

- **Nova opção de entrega**: uma classe em `entrega/` com `@Component` implementando
  `ModalidadeEntrega` (código, prazo, cálculo do frete e, se precisar, a regra de
  `atende` — como o limite de 5 kg do motoboy). Ela entra no catálogo sozinha.
- **Novo cupom**: uma classe em `cupom/` com `@Component` implementando `Cupom`.
- **Nova forma de pagamento**: uma classe em `pagamento/` com `@Component`
  implementando `FormaPagamento`.

A ordem do cálculo e a ordem de verificação dos erros ficam em
`dominio/CalculadoraResumo`. Todo arredondamento passa por `dominio/Dinheiro`
(centavos, meio para o par).
