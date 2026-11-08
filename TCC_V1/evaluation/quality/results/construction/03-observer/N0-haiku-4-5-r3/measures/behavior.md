# Comportamento: o roteiro do cenário 03

Resultado: 30 de 64 conferências passaram (34 falharam).
- A parte comum do enunciado: 27 de 46
- Os dados continuam depois de reiniciar o serviço (o banco): 0 de 1
- Cenário 03: os avisos da confirmação e do cancelamento: 2 de 15
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
- FALHOU: dinheiro no JSON como número com 2 casas (amountPaid 0.00) (esperado: "amountPaid":0.00; veio: {"ratePlan":"STANDARD","roomNumber":101,"discount":0.0,"guestEmail":"carla@example.com","guestName":"Carla Dias","total":599.76,"checkIn":"10/07/2029","guestCpf":"111.222.333-44","amountPaid":0.0,"nights":2,"subtotal":599.76,"guests":2,"id")
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
- FALHOU: cancelar STANDARD confirmada devolve o pago (299.88) (esperado: CANCELLED 299.88; veio: CANCELLED null)
- FALHOU: cancelar NON_REFUNDABLE confirmada não devolve nada (0.00) (esperado: 0.00; veio: null)
- FALHOU: cancelar reserva pendente não devolve nada (0.00) (esperado: 0.00; veio: null)
- OK: a reserva cancelada libera o quarto
- OK: o relatório de ocupação responde 200 em text/csv
- OK: ocupação: o cabeçalho
- FALHOU: ocupação: a reserva com check-out conta na noite de entrada (esperado: a linha '101,STANDARD,1,2029-07-10,2029-07-12'; veio: room,type,reservation,check_in,check_out total,0/5)
- FALHOU: ocupação: o total (esperado: a linha 'total,1/5'; veio: room,type,reservation,check_in,check_out total,0/5)
- OK: financeiro: o cabeçalho
- FALHOU: financeiro: a reserva paga (esperado: a linha '1,101,2029-07-10,2029-07-12,CHECKED_OUT,599.76'; veio: reservation,room,check_in,check_out,status,amount total,0.00)
- FALHOU: financeiro: o total (a cancelada com devolução não conta) (esperado: a linha 'total,599.76'; veio: reservation,room,check_in,check_out,status,amount total,0.00)
- FALHOU: financeiro: a NON_REFUNDABLE cancelada continua contando (esperado: a linha '2,101,2029-03-10,2029-03-11,CANCELLED,212.42'; veio: reservation,room,check_in,check_out,status,amount total,0.00)
- OK: mês fora do formato: 400 INVALID_REQUEST

## Os dados continuam depois de reiniciar o serviço (o banco)

- FALHOU: o serviço sobe de novo sobre o mesmo banco (esperado: subir; veio: não subiu)

## Cenário 03: os avisos da confirmação e do cancelamento

- FALHOU: a confirmação responde 200 (esperado: 200; veio: 400)
- FALHOU: o hóspede continua recebendo o e-mail de confirmação (esperado: 1; veio: 0)
- FALHOU: a governança recebe "Reserva confirmada - quarto 201" (esperado: 1; veio: 0)
- FALHOU: o e-mail da governança traz a entrada (10/03/2027) (esperado: um texto com '10/03/2027'; veio: )
- FALHOU: o e-mail da governança traz a saída (12/03/2027) (esperado: um texto com '12/03/2027'; veio: )
- FALHOU: o e-mail da governança traz quantos hóspedes vêm (2) (esperado: o número 2; veio: )
- FALHOU: o financeiro recebe "Pagamento recebido - reserva 1" (esperado: 1; veio: 0)
- FALHOU: o e-mail do financeiro traz o valor pago (R$ 779,80) (esperado: um texto com 'R$ 779,80'; veio: )
- FALHOU: a governança recebe "Reserva cancelada - quarto 201" (esperado: 1; veio: 0)
- FALHOU: o aviso de cancelamento traz as datas que ficaram livres (10/03/2027) (esperado: um texto com '10/03/2027'; veio: )
- FALHOU: o financeiro recebe "Reembolso - reserva 1" (esperado: 1; veio: 0)
- FALHOU: o reembolso traz o valor a devolver (R$ 779,80) (esperado: um texto com 'R$ 779,80'; veio: )
- FALHOU: NON_REFUNDABLE cancelada: a governança é avisada (esperado: 1; veio: 0)
- OK: NON_REFUNDABLE cancelada: nada a devolver, o financeiro não é avisado
- OK: cancelar uma reserva pendente não manda nada

## Duas reservas do mesmo quarto, ao mesmo tempo

- FALHOU: em 10 rodadas, nenhuma com as duas reservas criadas (esperado: 0; veio: 10)
- OK: em 10 rodadas, todas com uma criada e a outra recusada com 409
