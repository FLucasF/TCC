# Comportamento: o roteiro do cenário 09

Resultado: 28 de 64 conferências passaram (36 falharam).
- A parte comum do enunciado: 14 de 46
- Os dados continuam depois de reiniciar o serviço (o banco): 2 de 3
- Cenário 09: os relatórios: 11 de 13
- Duas reservas do mesmo quarto, ao mesmo tempo: 1 de 2

## A parte comum do enunciado

- OK: GET /rooms responde 200
- OK: cinco quartos
- FALHOU: o 201 comporta 3 (esperado: 3; veio: )
- FALHOU: a diária do 201 é 389.90 (esperado: 389.90; veio: )
- FALHOU: criar uma reserva responde 201 (esperado: 201; veio: 500)
- FALHOU: a reserva nasce PENDING (esperado: PENDING; veio: 500)
- FALHOU: STANDARD de 10 a 12/07 (alta temporada): 2 noites (esperado: 2; veio: null)
- FALHOU: STANDARD de 10 a 12/07: total 599.76 (esperado: 599.76; veio: null)
- FALHOU: nada pago antes da confirmação (amountPaid 0.00) (esperado: 0.00; veio: null)
- OK: refund null antes do cancelamento
- FALHOU: dinheiro no JSON como número com 2 casas (amountPaid 0.00) (esperado: "amountPaid":0.00; veio: {     "title": "Server Error",     "status": 500,     "type": "https://javalin.io/documentation#internalservererrorresponse",     "details": {} })
- FALHOU: as reservas são numeradas a partir de 1 (esperado: 1; veio: )
- FALHOU: NON_REFUNDABLE de uma noite: desconto 37.48 (meio para o par) (esperado: 37.48; veio: null)
- FALHOU: NON_REFUNDABLE de uma noite: total 212.42 (esperado: 212.42; veio: null)
- OK: quarto ocupado numa das noites: 409 ROOM_UNAVAILABLE
- OK: mais hóspedes que a capacidade: 422 CAPACITY_EXCEEDED
- OK: quarto que não existe: 404 ROOM_NOT_FOUND
- OK: saída igual à entrada: 400 INVALID_REQUEST
- OK: nenhum hóspede: 400 INVALID_REQUEST
- FALHOU: corpo que não é JSON: 400 INVALID_REQUEST (esperado: 400 INVALID_REQUEST; veio: 500)
- OK: faltam campos: 400 INVALID_REQUEST
- OK: reserva que não existe: 404 RESERVATION_NOT_FOUND
- FALHOU: segunda via de reserva pendente: 409 OPERATION_NOT_ALLOWED (esperado: 409 OPERATION_NOT_ALLOWED; veio: 404)
- FALHOU: confirmar responde 200 com CONFIRMED (esperado: 200 CONFIRMED; veio: 404 404)
- FALHOU: o pago é o total (599.76) (esperado: 599.76; veio: null)
- FALHOU: confirmar de novo: 409 OPERATION_NOT_ALLOWED (esperado: 409 OPERATION_NOT_ALLOWED; veio: 404)
- FALHOU: o hóspede recebe o e-mail "Reserva confirmada - Pousada Maré Alta" (esperado: 1; veio: 0)
- FALHOU: o e-mail traz o comprovante ("Total: R$ 599,76") (esperado: um texto com 'Total: R$ 599,76'; veio: )
- FALHOU: a segunda via responde 200 com o título "Comprovante de reserva" (esperado: 200 Comprovante de reserva; veio: 404 Endpoint GET /reservations//document not found)
- FALHOU: check-in responde 200 com CHECKED_IN (esperado: 200 CHECKED_IN; veio: 404 404)
- FALHOU: cancelar depois do check-in: 409 OPERATION_NOT_ALLOWED (esperado: 409 OPERATION_NOT_ALLOWED; veio: 404)
- FALHOU: check-out responde 200 com o comprovante (esperado: 200 Comprovante de reserva ; veio: 404 Endpoint POST /reservations//check-out not found null)
- FALHOU: a reserva fica CHECKED_OUT (esperado: CHECKED_OUT; veio: 404)
- FALHOU: cancelar STANDARD confirmada devolve o pago (299.88) (esperado: CANCELLED 299.88; veio: 404 null)
- FALHOU: cancelar NON_REFUNDABLE confirmada não devolve nada (0.00) (esperado: 0.00; veio: null)
- FALHOU: cancelar reserva pendente não devolve nada (0.00) (esperado: 0.00; veio: null)
- FALHOU: a reserva cancelada libera o quarto (esperado: 201; veio: 409)
- OK: o relatório de ocupação responde 200 em text/csv
- OK: ocupação: o cabeçalho
- FALHOU: ocupação: a reserva com check-out conta na noite de entrada (esperado: a linha '101,STANDARD,,2029-07-10,2029-07-12'; veio: Pousada Maré Alta,occupancy,2029-07-10 room,type,reservation,check_in,check_out total,0/5)
- FALHOU: ocupação: o total (esperado: a linha 'total,1/5'; veio: Pousada Maré Alta,occupancy,2029-07-10 room,type,reservation,check_in,check_out total,0/5)
- OK: financeiro: o cabeçalho
- FALHOU: financeiro: a reserva paga (esperado: a linha ',101,2029-07-10,2029-07-12,CHECKED_OUT,599.76'; veio: Pousada Maré Alta,revenue,2029-07 reservation,room,check_in,check_out,status,amount total,0.00)
- FALHOU: financeiro: o total (a cancelada com devolução não conta) (esperado: a linha 'total,599.76'; veio: Pousada Maré Alta,revenue,2029-07 reservation,room,check_in,check_out,status,amount total,0.00)
- FALHOU: financeiro: a NON_REFUNDABLE cancelada continua contando (esperado: a linha ',101,2029-03-10,2029-03-11,CANCELLED,212.42'; veio: Pousada Maré Alta,revenue,2029-03 reservation,room,check_in,check_out,status,amount total,0.00)
- OK: mês fora do formato: 400 INVALID_REQUEST

## Os dados continuam depois de reiniciar o serviço (o banco)

- FALHOU: a reserva continua lá, CHECKED_OUT (esperado: 200 CHECKED_OUT; veio: 404 404)
- OK: as mensagens continuam lá
- OK: o quarto continua ocupado depois de reiniciar

## Cenário 09: os relatórios

- nota: as reservas não vieram numeradas de 1 a 6 (a última foi ''): as linhas abaixo esperam essa numeração
- OK: ocupação: a linha de título vem primeiro
- OK: ocupação: o cabeçalho continua
- OK: ocupação: a reserva da noite
- OK: ocupação: o total
- OK: financeiro: a linha de título vem primeiro
- FALHOU: financeiro: título, cabeçalho, linhas em ordem e total (esperado: Pousada Maré Alta,revenue,2027-07|reservation,room,check_in,check_out,status,amount|2,201,2027-07-05,2027-07-07,CANCELLED,795.40|1,101,2027-07-10,2027-07-13,CONFIRMED,899.64|3,301,2027-07-20,2027-07-21,CHECKED_IN,695.88|total,2390.92; veio: Pousada Maré Alta,revenue,2027-07|reservation,room,check_in,check_out,status,amount|5,202,2027-07-01,2027-07-03,CANCELLED,0.00|2,201,2027-07-05,2027-07-07,CANCELLED,795.40|1,101,2027-07-10,2027-07-13,CONFIRMED,899.64|3,301,2027-07-20,2027-)
- OK: taxa de turismo: responde 200 em text/csv
- OK: taxa de turismo: título, cabeçalho, as pagas que começam no mês e o total
- OK: taxa de turismo de um mês vazio: título, cabeçalho e total 0.00
- OK: planos: responde 200 em text/csv
- FALHOU: planos: título, cabeçalho, STANDARD, NON_REFUNDABLE e o total (esperado: Pousada Maré Alta,rate-plans,2027-07|rate_plan,reservations,amount|STANDARD,2,1595.52|NON_REFUNDABLE,1,795.40|total,3,2390.92; veio: Pousada Maré Alta,rate-plans,2027-07|rate_plan,reservations,amount|STANDARD,3,1595.52|NON_REFUNDABLE,1,795.40|total,4,2390.92)
- OK: planos: um plano sem reserva no mês aparece com 0
- OK: taxa de turismo com mês fora do formato: 400 INVALID_REQUEST

## Duas reservas do mesmo quarto, ao mesmo tempo

- nota: rodada 1: 500 500
- nota: rodada 2: 500 500
- nota: rodada 3: 500 500
- nota: rodada 4: 500 500
- nota: rodada 5: 500 500
- nota: rodada 6: 500 500
- nota: rodada 7: 500 500
- nota: rodada 8: 500 500
- nota: rodada 9: 500 500
- nota: rodada 10: 500 500
- OK: em 10 rodadas, nenhuma com as duas reservas criadas
- FALHOU: em 10 rodadas, todas com uma criada e a outra recusada com 409 (esperado: 0; veio: 10)
