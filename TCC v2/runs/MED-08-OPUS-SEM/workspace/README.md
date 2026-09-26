# Serviço de resumo da compra

Calcula o resumo que o cliente vê antes de confirmar o pedido (produtos, cupom, frete,
ajuste do pagamento e valor final). Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Rodar

```
mvn verify        # compila e roda os testes
mvn spring-boot:run   # sobe em http://localhost:8080
```

Chamada: `POST /checkout/resumo` (JSON), conforme o combinado com o desenvolvedor do site.

## Onde mexer quando o negócio muda

- **Nova opção de entrega**: criar uma classe em `entrega/` que implemente `ModalidadeEntrega`
  (código, frete, prazo e, se houver, a restrição em `atende`) com a anotação `@Component`.
  Ela entra no catálogo sozinha.
- **Novo cupom**: mesma ideia em `cupom/`, implementando `Cupom`.
- **Nova forma de pagamento**: mesma ideia em `pagamento/`, implementando `FormaPagamento`.
- A ordem do cálculo e a ordem de verificação dos erros ficam em `CheckoutService`.
- O arredondamento em centavos (meio para o par) fica em `dominio/Dinheiro`.

Os quatro exemplos conferidos pelo financeiro estão nos testes em
`src/test/java/com/loja/checkout/CheckoutResumoApiTest.java`.
