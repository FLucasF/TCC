# Serviço de pedidos

Acompanha cada pedido da loja depois que o cliente finaliza a compra: guarda a
situação, o histórico, o reembolso e confere se cada ação pedida vale na
situação atual. Os pedidos ficam na memória enquanto o serviço estiver rodando
(não há banco de dados).

## Rodar

```
mvn verify          # compila e roda os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

## Como o site chama

```
POST /pedidos              {"valorProdutos": 200.00, "frete": 20.00}   -> 201
POST /pedidos/{id}/acoes   {"acao": "PAGAR"}                           -> 200
GET  /pedidos/{id}                                                     -> 200
```

Erros saem sempre como `{"erro": "CODIGO"}`: `PEDIDO_INVALIDO` (400),
`PEDIDO_NAO_ENCONTRADO` (404), `ACAO_INVALIDA` (400) e `ACAO_NAO_PERMITIDA`
(409). Quando uma ação dá erro, o pedido não muda nada.

## Onde ficam as regras do processo

Todo o fluxo está em `src/main/java/com/loja/pedidos/dominio/Situacao.java`:
cada situação declara o texto que o cliente vê e as ações que valem nela, com o
que cada ação provoca (nova situação, reembolso, volta do estoque na hora,
agendamento da coleta). Para mudar o processo — uma etapa nova no meio do
caminho, por exemplo — basta acrescentar a situação lá e declarar as suas
transições; nenhuma regra de fluxo vive fora desse arquivo.
