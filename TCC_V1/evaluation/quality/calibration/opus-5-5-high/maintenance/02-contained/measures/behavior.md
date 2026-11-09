# Comportamento: o roteiro do cenário 02

Resultado: 72 de 72 conferências passaram (0 falharam).
- A parte comum do enunciado: 46 de 46
- Os dados continuam depois de reiniciar o serviço (o banco): 3 de 3
- Cenário 02: o acréscimo novo: 5 de 5
- Cenário 02: a semana da Páscoa: 10 de 10
- Cenário 02: o resto dos períodos continua: 6 de 6
- Duas reservas do mesmo quarto, ao mesmo tempo: 2 de 2

## A parte comum do enunciado

- OK: GET /rooms responde 200
- OK: cinco quartos
- OK: o 201 comporta 3
- OK: a diária do 201 é 389.90
- OK: criar uma reserva responde 201
- OK: a reserva nasce PENDING
- OK: STANDARD de 10 a 12/07 (alta temporada): 2 noites
- OK: STANDARD de 10 a 12/07: total 624.76
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
- OK: confirmar responde 200 com CONFIRMED
- OK: o pago é o total (624.76)
- OK: confirmar de novo: 409 OPERATION_NOT_ALLOWED
- OK: o hóspede recebe o e-mail "Reserva confirmada - Pousada Maré Alta"
- OK: o e-mail traz o comprovante ("Total: R$ 624,76")
- OK: a segunda via responde 200 com o título "Comprovante de reserva"
- OK: check-in responde 200 com CHECKED_IN
- OK: cancelar depois do check-in: 409 OPERATION_NOT_ALLOWED
- OK: check-out responde 200 com o comprovante
- OK: a reserva fica CHECKED_OUT
- OK: cancelar STANDARD confirmada devolve o pago (312.38)
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

## Cenário 02: o acréscimo novo

- OK: standard numa noite de julho: 312.38
- OK: deluxe numa noite de julho: 487.38
- OK: suíte numa noite de julho: 724.88
- OK: não reembolsável em alta: desconto 46.86 sobre o valor novo
- OK: não reembolsável em alta: total 265.52

## Cenário 02: a semana da Páscoa

- OK: a noite de 2027-03-20 custa 249.90
- OK: a noite de 2027-03-21 custa 312.38
- OK: a noite de 2027-03-24 custa 312.38
- OK: a noite de 2027-03-28 custa 312.38
- OK: a noite de 2027-03-29 custa 249.90
- OK: a noite de 2028-04-08 custa 249.90
- OK: a noite de 2028-04-09 custa 312.38
- OK: a noite de 2028-04-16 custa 312.38
- OK: a noite de 2028-04-17 custa 249.90
- OK: de 20 a 22/03/2027, uma noite fora e uma dentro: 562.28

## Cenário 02: o resto dos períodos continua

- OK: a noite de 2027-12-14 custa 249.90
- OK: a noite de 2027-12-15 custa 312.38
- OK: a noite de 2027-02-28 custa 312.38
- OK: a noite de 2027-03-01 custa 249.90
- OK: a noite de 2027-07-31 custa 312.38
- OK: a noite de 2027-08-01 custa 249.90

## Duas reservas do mesmo quarto, ao mesmo tempo

- OK: em 10 rodadas, nenhuma com as duas reservas criadas
- OK: em 10 rodadas, todas com uma criada e a outra recusada com 409
