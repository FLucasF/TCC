# Servico de resumo da compra

Java 21 + Spring Boot 4.1.1, sem banco de dados.

- Rodar os testes: `mvn verify`
- Subir o servico: `mvn spring-boot:run` (porta 8080)
- Endpoint: `POST /checkout/resumo`

## Onde mexer quando o negocio muda

| Mudanca | Onde |
|---|---|
| Nova opcao de entrega | uma classe nova em `dominio/entrega` implementando `ModalidadeEntrega` |
| Novo cupom | uma classe nova em `dominio/cupom` implementando `Cupom` |
| Novo nivel do clube | uma constante nova em `dominio/clube/NivelClube` |
| Aliquota de regiao | `dominio/Regiao` |

Entregas e cupons novos sao descobertos sozinhos: basta a anotacao `@Component`,
nenhum outro arquivo precisa ser alterado.
