## Planos de tarifa novos

Hoje a pousada tem dois planos de tarifa: o normal (`STANDARD`) e o não reembolsável (`NON_REFUNDABLE`), com 15% de desconto. O comercial fechou três planos novos, que começam a valer agora.

**Convênio com empresas (`CORPORATE`).** Algumas empresas têm um desconto combinado sobre as diárias. Hoje são estas:

| empresa | código | desconto |
|---|---|---|
| Tecnosul | `TECNOSUL` | 12% |
| Agrovale | `AGROVALE` | 18% |
| Farmavida | `FARMAVIDA` | 25% |

O hóspede do convênio informa o código da empresa na reserva. Sem o código, ou com um código que não está na lista, a reserva não pode ser feita. Se cancelar antes da entrada, recebe de volta tudo o que pagou, como no plano normal.

**Pacote de fim de semana (`WEEKEND_PACKAGE`).** Entrada na sexta e saída no domingo logo depois: exatamente duas noites, por um preço fechado que vale o ano todo, com ou sem temporada. São R$ 520,00 no standard, R$ 820,00 no deluxe e R$ 1.200,00 na suíte. Em outras datas, o pacote não pode ser reservado. No pacote, as diárias e o total são o preço do pacote, sem desconto. Se cancelar, recebe de volta metade do que pagou.

**Estadia longa (`LONG_STAY`).** Para quem fica 7 noites ou mais: 10% de desconto sobre as diárias (a alta temporada continua valendo nas noites de temporada). Com menos de 7 noites, o plano não pode ser usado. Se cancelar, recebe de volta tudo o que pagou.

O desconto e a devolução são arredondados para centavos, como já é hoje.

Uns exemplos que o financeiro conferiu:
- convênio Tecnosul, quarto deluxe, de 10/03/2027 a 12/03/2027: diárias R$ 779,80, desconto R$ 93,58, total R$ 686,22;
- pacote de fim de semana no deluxe, de sexta 12/03/2027 a domingo 14/03/2027: total R$ 820,00; se cancelar depois de pagar, volta R$ 410,00;
- estadia longa no standard, de 01/03/2027 a 08/03/2027: diárias R$ 1.749,30, desconto R$ 174,93, total R$ 1.574,37.

O comercial vive inventando plano: só este ano foram esses três, e eles já estão falando de um para a melhor idade e de outro para quem reserva com 60 dias de antecedência. Cada plano vem com o seu jeito de calcular o preço, com as suas regras de quem pode usar e com a sua regra de devolução no cancelamento.

**Para o pessoal do site:** na criação da reserva entra o campo `companyCode`, usado só no convênio (nos outros planos, se vier, é ignorado). Quando a reserva não pode ser feita por causa das regras do plano, a resposta é `400` com o código `INVALID_REQUEST`, como nos outros erros de dados.
