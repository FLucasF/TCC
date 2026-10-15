# Servico de resumo da compra

Java 21 + Spring Boot 4.1.1, sem banco de dados.

    mvn verify          # compila e roda os testes
    mvn spring-boot:run # sobe o servico na porta 8080

Endpoint: `POST /checkout/resumo` (JSON), conforme o combinado com o desenvolvedor do site.
Erros de negocio voltam como `400` com `{"erro": "CODIGO"}`.

## Onde mexer quando o negocio mudar

Cada regra que muda com frequencia fica em uma classe propria. Para adicionar
uma opcao nova basta criar a classe e anotar com `@Component`; ela entra
automaticamente no catalogo, sem alterar o calculo.

| O que | Onde | Interface |
|---|---|---|
| Opcao de entrega (transportadora nova) | `dominio/entrega` | `ModalidadeEntrega` (`EntregaPorPeso` ajuda quando cobra fixo + por kg) |
| Cupom / promocao | `dominio/cupom` | `Cupom` |
| Nivel do clube | `dominio/clube` | `NivelClube` |
| Forma de pagamento | `dominio/pagamento` | `FormaPagamento` |
| Aliquota por regiao | `dominio/regiao/Regiao` | enum (a conta e a mesma, so a aliquota muda) |

A ordem do calculo e a ordem das validacoes ficam em
`servico/CalculadoraResumoService`, e o arredondamento em centavos (meio para o
par) em `comum/Dinheiro`.

## Um ponto para o financeiro confirmar

Os exemplos 1 a 4 do anexo nao informam a regiao do cliente e os totais que eles
mostram fecham exatamente **sem** o imposto da regiao (ex.: exemplo 1,
401,83 - 5% = 381,74). Como a regiao e obrigatoria na chamada e a regra diz que o
imposto sempre entra no total, o servico segue a regra escrita: o imposto e
sempre aplicado. Assim, o exemplo 1 com regiao Sudeste da imposto 44,25 e total
final 423,78. O exemplo 5, que informa regiao, fecha exatamente com os numeros
do anexo. Os testes conferem os exemplos 1 a 4 nas partes que eles definem
(produtos, cupom, frete, prazo) e as contas de pagamento sobre os totais que o
financeiro usou.
