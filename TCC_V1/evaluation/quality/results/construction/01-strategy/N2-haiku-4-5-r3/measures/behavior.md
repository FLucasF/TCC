# Comportamento: o roteiro do cenário 01

Resultado: 50 de 72 conferências passaram (22 falharam).
- A parte comum do enunciado: 30 de 46
- Os dados continuam depois de reiniciar o serviço (o banco): 1 de 3
- Cenário 01: os planos de tarifa novos: 18 de 21
- Duas reservas do mesmo quarto, ao mesmo tempo: 1 de 2

## A parte comum do enunciado

- OK: GET /rooms responde 200
- OK: cinco quartos
- OK: o 201 comporta 3
- OK: a diária do 201 é 389.90
- OK: criar uma reserva responde 201
- OK: a reserva nasce PENDING
- OK: STANDARD de 10 a 12/07 (alta temporada): 2 noites
- OK: STANDARD de 10 a 12/07: total 599.76
- OK: nada pago antes da confirmação (amountPaid 0.00)
- OK: refund null antes do cancelamento
- OK: dinheiro no JSON como número com 2 casas (amountPaid 0.00)
- OK: as reservas são numeradas a partir de 1
- OK: NON_REFUNDABLE de uma noite: desconto 37.48 (meio para o par)
- OK: NON_REFUNDABLE de uma noite: total 212.42
- OK: quarto ocupado numa das noites: 409 ROOM_UNAVAILABLE
- OK: mais hóspedes que a capacidade: 422 CAPACITY_EXCEEDED
- OK: quarto que não existe: 404 ROOM_NOT_FOUND
- OK: saída igual à entrada: 400 INVALID_REQUEST
- OK: nenhum hóspede: 400 INVALID_REQUEST
- OK: corpo que não é JSON: 400 INVALID_REQUEST
- OK: faltam campos: 400 INVALID_REQUEST
- OK: reserva que não existe: 404 RESERVATION_NOT_FOUND
- OK: segunda via de reserva pendente: 409 OPERATION_NOT_ALLOWED
- FALHOU: confirmar responde 200 com CONFIRMED (esperado: 200 CONFIRMED; veio: 400 null)
- FALHOU: o pago é o total (599.76) (esperado: 599.76; veio: null)
- FALHOU: confirmar de novo: 409 OPERATION_NOT_ALLOWED (esperado: 409 OPERATION_NOT_ALLOWED; veio: 400 INVALID_REQUEST)
- FALHOU: o hóspede recebe o e-mail "Reserva confirmada - Pousada Maré Alta" (esperado: 1; veio: 0)
- FALHOU: o e-mail traz o comprovante ("Total: R$ 599,76") (esperado: um texto com 'Total: R$ 599,76'; veio: )
- FALHOU: a segunda via responde 200 com o título "Comprovante de reserva" (esperado: 200 Comprovante de reserva; veio: 409 null)
- FALHOU: check-in responde 200 com CHECKED_IN (esperado: 200 CHECKED_IN; veio: 409 null)
- FALHOU: cancelar depois do check-in: 409 OPERATION_NOT_ALLOWED (esperado: 409 OPERATION_NOT_ALLOWED; veio: 200)
- FALHOU: check-out responde 200 com o comprovante (esperado: 200 Comprovante de reserva 1; veio: 409 null null)
- FALHOU: a reserva fica CHECKED_OUT (esperado: CHECKED_OUT; veio: CANCELLED)
- FALHOU: cancelar STANDARD confirmada devolve o pago (299.88) (esperado: CANCELLED 299.88; veio: CANCELLED 0.00)
- OK: cancelar NON_REFUNDABLE confirmada não devolve nada (0.00)
- OK: cancelar reserva pendente não devolve nada (0.00)
- OK: a reserva cancelada libera o quarto
- OK: o relatório de ocupação responde 200 em text/csv
- OK: ocupação: o cabeçalho
- FALHOU: ocupação: a reserva com check-out conta na noite de entrada (esperado: a linha '101,STANDARD,1,2029-07-10,2029-07-12'; veio: room,type,reservation,check_in,check_out total,0/5)
- FALHOU: ocupação: o total (esperado: a linha 'total,1/5'; veio: room,type,reservation,check_in,check_out total,0/5)
- OK: financeiro: o cabeçalho
- FALHOU: financeiro: a reserva paga (esperado: a linha '1,101,2029-07-10,2029-07-12,CHECKED_OUT,599.76'; veio: reservation,room,check_in,check_out,status,amount 5,102,2029-07-20,2029-07-21,PENDING,0.00 total,,,,0.00)
- FALHOU: financeiro: o total (a cancelada com devolução não conta) (esperado: a linha 'total,599.76'; veio: reservation,room,check_in,check_out,status,amount 5,102,2029-07-20,2029-07-21,PENDING,0.00 total,,,,0.00)
- FALHOU: financeiro: a NON_REFUNDABLE cancelada continua contando (esperado: a linha '2,101,2029-03-10,2029-03-11,CANCELLED,212.42'; veio: reservation,room,check_in,check_out,status,amount total,,,,0.00)
- OK: mês fora do formato: 400 INVALID_REQUEST

## Os dados continuam depois de reiniciar o serviço (o banco)

- FALHOU: a reserva continua lá, CHECKED_OUT (esperado: 200 CHECKED_OUT; veio: 200 CANCELLED)
- OK: as mensagens continuam lá
- FALHOU: o quarto continua ocupado depois de reiniciar (esperado: 409 ROOM_UNAVAILABLE; veio: 201)

## Cenário 01: os planos de tarifa novos

- OK: CORPORATE TECNOSUL, deluxe de 10 a 12/03: 201
- OK: TECNOSUL: diárias 779.80, desconto 93.58, total 686.22
- OK: AGROVALE (18%): desconto 140.36, total 639.44
- OK: FARMAVIDA (25%): desconto 194.95, total 584.85
- OK: CORPORATE sem o código da empresa: 400 INVALID_REQUEST
- OK: CORPORATE com um código que não existe: 400 INVALID_REQUEST
- FALHOU: CORPORATE cancelada depois de paga devolve tudo (686.22) (esperado: 686.22; veio: 0.00)
- OK: WEEKEND_PACKAGE deluxe, sexta 12/03 a domingo 14/03: 201
- OK: pacote deluxe: diárias 820.00, desconto 0.00, total 820.00
- FALHOU: pacote pago e cancelado devolve a metade (410.00) (esperado: 410.00; veio: 0.00)
- OK: pacote standard: total 520.00
- OK: pacote na suíte: total 1200.00
- OK: pacote em julho (alta temporada) continua 820.00
- OK: pacote de sábado a segunda: 400 INVALID_REQUEST
- OK: pacote de sexta a segunda (3 noites): 400 INVALID_REQUEST
- OK: LONG_STAY standard de 01 a 08/03: diárias 1749.30, desconto 174.93, total 1574.37
- OK: LONG_STAY com noites de temporada (28/06 a 05/07): total 1754.30
- OK: LONG_STAY com 6 noites: 400 INVALID_REQUEST
- FALHOU: LONG_STAY cancelada depois de paga devolve tudo (1574.37) (esperado: 1574.37; veio: 0.00)
- OK: STANDARD com companyCode: o código é ignorado (201, desconto 0.00)
- OK: NON_REFUNDABLE continua com 15% (desconto 37.48)

## Duas reservas do mesmo quarto, ao mesmo tempo

- FALHOU: em 10 rodadas, nenhuma com as duas reservas criadas (esperado: 0; veio: 9)
- OK: em 10 rodadas, todas com uma criada e a outra recusada com 409
