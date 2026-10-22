# Comportamento: o roteiro do cenário 09

Resultado: 36 de 64 conferências passaram (28 falharam).
- A parte comum do enunciado: 27 de 46
- Os dados continuam depois de reiniciar o serviço (o banco): 1 de 3
- Cenário 09: os relatórios: 7 de 13
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
- FALHOU: dinheiro no JSON como número com 2 casas (amountPaid 0.00) (esperado: "amountPaid":0.00; veio: {"ratePlan":"STANDARD","roomNumber":101,"discount":0,"guestEmail":"carla@example.com","guestName":"Carla Dias","total":599.76,"checkIn":"2029-07-10","guestCpf":"111.222.333-44","amountPaid":0,"nights":2,"subtotal":599.76,"guests":2,"id":1,")
- OK: as reservas são numeradas a partir de 1
- OK: NON_REFUNDABLE de uma noite: desconto 37.48 (meio para o par)
- OK: NON_REFUNDABLE de uma noite: total 212.42
- OK: quarto ocupado numa das noites: 409 ROOM_UNAVAILABLE
- OK: mais hóspedes que a capacidade: 422 CAPACITY_EXCEEDED
- OK: quarto que não existe: 404 ROOM_NOT_FOUND
- OK: saída igual à entrada: 400 INVALID_REQUEST
- FALHOU: nenhum hóspede: 400 INVALID_REQUEST (esperado: 400 INVALID_REQUEST; veio: 422 CAPACITY_EXCEEDED)
- OK: corpo que não é JSON: 400 INVALID_REQUEST
- OK: faltam campos: 400 INVALID_REQUEST
- OK: reserva que não existe: 404 RESERVATION_NOT_FOUND
- FALHOU: segunda via de reserva pendente: 409 OPERATION_NOT_ALLOWED (esperado: 409 OPERATION_NOT_ALLOWED; veio: 404 RESERVATION_NOT_FOUND)
- FALHOU: confirmar responde 200 com CONFIRMED (esperado: 200 CONFIRMED; veio: 400 null)
- FALHOU: o pago é o total (599.76) (esperado: 599.76; veio: null)
- FALHOU: confirmar de novo: 409 OPERATION_NOT_ALLOWED (esperado: 409 OPERATION_NOT_ALLOWED; veio: 400 INVALID_REQUEST)
- FALHOU: o hóspede recebe o e-mail "Reserva confirmada - Pousada Maré Alta" (esperado: 1; veio: 0)
- FALHOU: o e-mail traz o comprovante ("Total: R$ 599,76") (esperado: um texto com 'Total: R$ 599,76'; veio: )
- FALHOU: a segunda via responde 200 com o título "Comprovante de reserva" (esperado: 200 Comprovante de reserva; veio: 404 null)
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
- FALHOU: ocupação: a reserva com check-out conta na noite de entrada (esperado: a linha '101,STANDARD,1,2029-07-10,2029-07-12'; veio: Pousada Maré Alta,occupancy,2029-07-10 room,type,reservation,check_in,check_out total,0/5)
- FALHOU: ocupação: o total (esperado: a linha 'total,1/5'; veio: Pousada Maré Alta,occupancy,2029-07-10 room,type,reservation,check_in,check_out total,0/5)
- OK: financeiro: o cabeçalho
- FALHOU: financeiro: a reserva paga (esperado: a linha '1,101,2029-07-10,2029-07-12,CHECKED_OUT,599.76'; veio: Pousada Maré Alta,revenue,2029-07 reservation,room,check_in,check_out,status,amount total,0.00)
- FALHOU: financeiro: o total (a cancelada com devolução não conta) (esperado: a linha 'total,599.76'; veio: Pousada Maré Alta,revenue,2029-07 reservation,room,check_in,check_out,status,amount total,0.00)
- FALHOU: financeiro: a NON_REFUNDABLE cancelada continua contando (esperado: a linha '2,101,2029-03-10,2029-03-11,CANCELLED,212.42'; veio: Pousada Maré Alta,revenue,2029-03 reservation,room,check_in,check_out,status,amount total,0.00)
- OK: mês fora do formato: 400 INVALID_REQUEST

## Os dados continuam depois de reiniciar o serviço (o banco)

- FALHOU: a reserva continua lá, CHECKED_OUT (esperado: 200 CHECKED_OUT; veio: 200 CANCELLED)
- OK: as mensagens continuam lá
- FALHOU: o quarto continua ocupado depois de reiniciar (esperado: 409 ROOM_UNAVAILABLE; veio: 201)

## Cenário 09: os relatórios

- OK: ocupação: a linha de título vem primeiro
- OK: ocupação: o cabeçalho continua
- FALHOU: ocupação: a reserva da noite (esperado: a linha '101,STANDARD,1,2027-07-10,2027-07-13'; veio: Pousada Maré Alta,occupancy,2027-07-10 room,type,reservation,check_in,check_out total,0/5)
- FALHOU: ocupação: o total (esperado: a linha 'total,1/5'; veio: Pousada Maré Alta,occupancy,2027-07-10 room,type,reservation,check_in,check_out total,0/5)
- OK: financeiro: a linha de título vem primeiro
- FALHOU: financeiro: título, cabeçalho, linhas em ordem e total (esperado: Pousada Maré Alta,revenue,2027-07|reservation,room,check_in,check_out,status,amount|2,201,2027-07-05,2027-07-07,CANCELLED,795.40|1,101,2027-07-10,2027-07-13,CONFIRMED,899.64|3,301,2027-07-20,2027-07-21,CHECKED_IN,695.88|total,2390.92; veio: Pousada Maré Alta,revenue,2027-07|reservation,room,check_in,check_out,status,amount|total,0.00)
- OK: taxa de turismo: responde 200 em text/csv
- FALHOU: taxa de turismo: título, cabeçalho, as pagas que começam no mês e o total (esperado: Pousada Maré Alta,tourism-tax,2027-07|reservation,room,check_in,check_out,guests,nights,tax|1,101,2027-07-10,2027-07-13,2,3,27.00|3,301,2027-07-20,2027-07-21,4,1,18.00|total,45.00; veio: Pousada Maré Alta,tourism-tax,2027-07|reservation,room,check_in,check_out,guests,nights,tax|total,0.00)
- OK: taxa de turismo de um mês vazio: título, cabeçalho e total 0.00
- OK: planos: responde 200 em text/csv
- FALHOU: planos: título, cabeçalho, STANDARD, NON_REFUNDABLE e o total (esperado: Pousada Maré Alta,rate-plans,2027-07|rate_plan,reservations,amount|STANDARD,2,1595.52|NON_REFUNDABLE,1,795.40|total,3,2390.92; veio: Pousada Maré Alta,rate-plans,2027-07|rate_plan,reservations,amount|STANDARD,0,0.00|NON_REFUNDABLE,0,0.00|total,0,0.00)
- FALHOU: planos: um plano sem reserva no mês aparece com 0 (esperado: Pousada Maré Alta,rate-plans,2027-06|rate_plan,reservations,amount|STANDARD,1,799.68|NON_REFUNDABLE,0,0.00|total,1,799.68; veio: Pousada Maré Alta,rate-plans,2027-06|rate_plan,reservations,amount|STANDARD,0,0.00|NON_REFUNDABLE,0,0.00|total,0,0.00)
- OK: taxa de turismo com mês fora do formato: 400 INVALID_REQUEST

## Duas reservas do mesmo quarto, ao mesmo tempo

- FALHOU: em 10 rodadas, nenhuma com as duas reservas criadas (esperado: 0; veio: 9)
- OK: em 10 rodadas, todas com uma criada e a outra recusada com 409
