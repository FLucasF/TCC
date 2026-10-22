# Comportamento: o roteiro do cenário 09

Resultado: 63 de 64 conferências passaram (1 falharam).
- A parte comum do enunciado: 46 de 46
- Os dados continuam depois de reiniciar o serviço (o banco): 3 de 3
- Cenário 09: os relatórios: 12 de 13
- Duas reservas do mesmo quarto, ao mesmo tempo: 2 de 2

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

## Cenário 09: os relatórios

- OK: ocupação: a linha de título vem primeiro
- OK: ocupação: o cabeçalho continua
- OK: ocupação: a reserva da noite
- OK: ocupação: o total
- OK: financeiro: a linha de título vem primeiro
- OK: financeiro: título, cabeçalho, linhas em ordem e total
- OK: taxa de turismo: responde 200 em text/csv
- OK: taxa de turismo: título, cabeçalho, as pagas que começam no mês e o total
- FALHOU: taxa de turismo de um mês vazio: título, cabeçalho e total 0.00 (esperado: Pousada Maré Alta,tourism-tax,2027-08|reservation,room,check_in,check_out,guests,nights,tax|total,0.00; veio: Pousada Maré Alta,tourism-tax,2027-08|reservation,room,check_in,check_out,guests,nights,tax|total,0)
- OK: planos: responde 200 em text/csv
- OK: planos: título, cabeçalho, STANDARD, NON_REFUNDABLE e o total
- OK: planos: um plano sem reserva no mês aparece com 0
- OK: taxa de turismo com mês fora do formato: 400 INVALID_REQUEST

## Duas reservas do mesmo quarto, ao mesmo tempo

- OK: em 10 rodadas, nenhuma com as duas reservas criadas
- OK: em 10 rodadas, todas com uma criada e a outra recusada com 409
