# Serviço de Checkout - Loja Online

Serviço REST em Java 21 com Spring Boot 4.1.1 que calcula o resumo de compras.

## Executar

```bash
mvn verify
```

## Endpoint

**POST** `/checkout/resumo`

Recebe um pedido em JSON e retorna o resumo com todos os valores calculados ou um código de erro.

## Estrutura

O projeto foi organizado seguindo as orientações do CLAUDE.md:

- **Domínio** - interfaces e estratégias para cada comportamento que varia:
  - `CalculadoraFrete` - calcula frete por modalidade de entrega
  - `AplicadorCupom` - aplica desconto de cada tipo de cupom
  - `CalculadoraPagamento` - ajusta valor por forma de pagamento
  - `BeneficiosClube` - benefícios de cada nível do clube
  
- **Fábricas** - escolhem a estratégia correta usando Map (sem condicionais)

- **Serviço** - orquestra o cálculo na ordem especificada e valida os dados

- **Controller** - expõe o endpoint REST

Cada tipo de variação (modalidade, cupom, pagamento, clube) tem sua própria implementação isolada, facilitando adicionar novas opções sem modificar o código existente.
