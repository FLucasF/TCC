# Comportamento: o roteiro do cenário 03

Resultado: 62 de 66 conferências passaram (4 falharam).
- A parte comum do enunciado: 43 de 46
- Os dados continuam depois de reiniciar o serviço (o banco): 3 de 3
- Cenário 03: os avisos da confirmação e do cancelamento: 15 de 15
- Duas reservas do mesmo quarto, ao mesmo tempo: 1 de 2

## A parte comum do enunciado

- OK: GET /rooms responde 200
- OK: cinco quartos
- FALHOU: o 201 comporta 3 (esperado: 3; veio: )
- FALHOU: a diária do 201 é 389.90 (esperado: 389.90; veio: )
- OK: criar uma reserva responde 201
- OK: a reserva nasce PENDING
- OK: STANDARD de 10 a 12/07 (alta temporada): 2 noites
- OK: STANDARD de 10 a 12/07: total 599.76
- OK: nada pago antes da confirmação (amountPaid 0.00)
- OK: refund null antes do cancelamento
- FALHOU: dinheiro no JSON como número com 2 casas (amountPaid 0.00) (esperado: "amountPaid":0.00; veio: {"id":1,"roomNumber":"101","guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44","checkIn":"2029-07-10","checkOut":"2029-07-12","guests":2,"ratePlan":"STANDARD","nights":2,"subtotal":599.76,"discount":0.00,"t)
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
- OK: confirmar responde 200 com CONFIRMED
- OK: o pago é o total (599.76)
- OK: confirmar de novo: 409 OPERATION_NOT_ALLOWED
- OK: o hóspede recebe o e-mail "Reserva confirmada - Pousada Maré Alta"
- OK: o e-mail traz o comprovante ("Total: R$ 599,76")
- OK: a segunda via responde 200 com o título "Comprovante de reserva"
- OK: check-in responde 200 com CHECKED_IN
- OK: cancelar depois do check-in: 409 OPERATION_NOT_ALLOWED
- OK: check-out responde 200 com o comprovante
- OK: a reserva fica CHECKED_OUT
- OK: cancelar STANDARD confirmada devolve o pago (299.88)
- OK: cancelar NON_REFUNDABLE confirmada não devolve nada (0.00)
- OK: cancelar reserva pendente não devolve nada (0.00)
- OK: a reserva cancelada libera o quarto
- OK: o relatório de ocupação responde 200 em text/csv
- OK: ocupação: o cabeçalho
- OK: ocupação: a reserva com check-out conta na noite de entrada
- OK: ocupação: o total
- OK: financeiro: o cabeçalho
- OK: financeiro: a reserva paga
- OK: financeiro: o total (a cancelada com devolução não conta)
- OK: financeiro: a NON_REFUNDABLE cancelada continua contando
- OK: mês fora do formato: 400 INVALID_REQUEST

## Os dados continuam depois de reiniciar o serviço (o banco)

- OK: a reserva continua lá, CHECKED_OUT
- OK: as mensagens continuam lá
- OK: o quarto continua ocupado depois de reiniciar

## Cenário 03: os avisos da confirmação e do cancelamento

- OK: a confirmação responde 200
- OK: o hóspede continua recebendo o e-mail de confirmação
- OK: a governança recebe "Reserva confirmada - quarto 201"
- OK: o e-mail da governança traz a entrada (10/03/2027)
- OK: o e-mail da governança traz a saída (12/03/2027)
- OK: o e-mail da governança traz quantos hóspedes vêm (2)
- OK: o financeiro recebe "Pagamento recebido - reserva 1"
- OK: o e-mail do financeiro traz o valor pago (R$ 779,80)
- OK: a governança recebe "Reserva cancelada - quarto 201"
- OK: o aviso de cancelamento traz as datas que ficaram livres (10/03/2027)
- OK: o financeiro recebe "Reembolso - reserva 1"
- OK: o reembolso traz o valor a devolver (R$ 779,80)
- OK: NON_REFUNDABLE cancelada: a governança é avisada
- OK: NON_REFUNDABLE cancelada: nada a devolver, o financeiro não é avisado
- OK: cancelar uma reserva pendente não manda nada

## Duas reservas do mesmo quarto, ao mesmo tempo

- FALHOU: em 10 rodadas, nenhuma com as duas reservas criadas (esperado: 0; veio: 10)
- OK: em 10 rodadas, todas com uma criada e a outra recusada com 409
