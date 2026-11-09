# Comportamento: o roteiro do cenário 06

Resultado: 60 de 61 conferências passaram (1 falharam).
- A parte comum do enunciado: 46 de 46
- Os dados continuam depois de reiniciar o serviço (o banco): 3 de 3
- Cenário 06: as crianças na reserva: 7 de 7
- Cenário 06: o comprovante: 1 de 2
- Cenário 06: as crianças continuam depois de reiniciar: 1 de 1
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

## Cenário 06: as crianças na reserva

- OK: sem o campo, são zero crianças
- OK: dois adultos e uma criança num quarto para três
- OK: criança não muda o preço
- OK: a consulta mostra as crianças
- OK: adultos e crianças acima da capacidade: 422 CAPACITY_EXCEEDED
- OK: o quarto para dois com dois adultos e uma criança: 422 CAPACITY_EXCEEDED
- OK: número negativo: 400 INVALID_REQUEST

## Cenário 06: o comprovante

- FALHOU: a linha "Crianças: 2" logo depois da linha do quarto (esperado: Crianças: 2; veio: Entrada: 10/05/2027 | Saída: 11/05/2027 | 1 diária)
- OK: sem criança, o comprovante fica como está

## Cenário 06: as crianças continuam depois de reiniciar

- OK: a reserva continua com uma criança

## Duas reservas do mesmo quarto, ao mesmo tempo

- OK: em 10 rodadas, nenhuma com as duas reservas criadas
- OK: em 10 rodadas, todas com uma criada e a outra recusada com 409
