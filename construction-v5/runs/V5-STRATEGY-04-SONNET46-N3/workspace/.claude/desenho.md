# Desenho — checkout/resumo

## O que muda de caso para caso
- **Entrega**: fórmula de custo, prazo em dias, restrição de disponibilidade (ex.: motoboy ≤ 5 kg)
- **Cupom**: lógica de desconto e condição de aplicabilidade (porcentagem, valor fixo com mínimo, frete grátis, leve-3-pague-2)
- **Nível de clube**: taxa de cashback, se isenta frete, se dá brinde acima de limiar
- **Forma de pagamento**: cálculo do ajuste, parcelas válidas, restrição de disponibilidade (boleto ≤ R$ 1.000)
- **Região**: taxa do seguro

## O que é igual em todos os casos
- Pipeline de validação (ordem fixa de 10 erros)
- Pipeline de cálculo: subtotal → frete → desconto → seguro → total → ajuste de pagamento
- Arredondamento HALF_EVEN (meio-para-par) em cada etapa
- Estrutura do response

## Estrutura escolhida
- `ShippingOption` — enum com métodos abstratos (`cost`, `deliveryDays`, `isAvailable`); cada constante implementa seu comportamento, sem dispatch externo
- `Coupon` — interface com `discount(subtotal, items, frete)` e `isApplicable(subtotal, items)`; implementações por cupom guardadas em `CouponRegistry` (Map); FRETEGRATIS recebe o frete calculado como parâmetro
- `ClubLevel` — enum com métodos abstratos (`cashbackOn`, `freeShipping`, `hasGiftAbove`)
- `PaymentMethod` — enum com métodos abstratos (`finalTotal`, `installmentValue`, `validParcelas`, `isAvailable`)
- `Region` — enum com taxa de seguro; método `insuranceOn(subtotal)`
- `CheckoutService` — orquestra validação e cálculo; usa `Enum.valueOf` para parse com mensagem de erro controlada

## Por que essa estrutura
O comportamento de cada opção mora só na sua constante/implementação; escolher entre elas é sempre via mapa ou valueOf, nunca cadeia de if/else.
