# Comportamento: o roteiro do cenário 05

Resultado: 30 de 83 conferências passaram (53 falharam).
- A parte comum do enunciado: 13 de 46
- Os dados continuam depois de reiniciar o serviço (o banco): 1 de 3
- Cenário 05: o documento por tipo de hóspede: 15 de 32
- Duas reservas do mesmo quarto, ao mesmo tempo: 1 de 2

## A parte comum do enunciado

- OK: GET /rooms responde 200
- OK: cinco quartos
- FALHOU: o 201 comporta 3 (esperado: 3; veio: )
- FALHOU: a diária do 201 é 389.90 (esperado: 389.90; veio: )
- FALHOU: criar uma reserva responde 201 (esperado: 201; veio: 500)
- FALHOU: a reserva nasce PENDING (esperado: PENDING; veio: null)
- FALHOU: STANDARD de 10 a 12/07 (alta temporada): 2 noites (esperado: 2; veio: null)
- FALHOU: STANDARD de 10 a 12/07: total 599.76 (esperado: 599.76; veio: null)
- FALHOU: nada pago antes da confirmação (amountPaid 0.00) (esperado: 0.00; veio: null)
- OK: refund null antes do cancelamento
- FALHOU: dinheiro no JSON como número com 2 casas (amountPaid 0.00) (esperado: "amountPaid":0.00; veio: {"error":"INTERNAL_ERROR","message":"Internal server error"})
- FALHOU: as reservas são numeradas a partir de 1 (esperado: 1; veio: )
- FALHOU: NON_REFUNDABLE de uma noite: desconto 37.48 (meio para o par) (esperado: 37.48; veio: null)
- FALHOU: NON_REFUNDABLE de uma noite: total 212.42 (esperado: 212.42; veio: null)
- FALHOU: quarto ocupado numa das noites: 409 ROOM_UNAVAILABLE (esperado: 409 ROOM_UNAVAILABLE; veio: 500 INTERNAL_ERROR)
- OK: mais hóspedes que a capacidade: 422 CAPACITY_EXCEEDED
- OK: quarto que não existe: 404 ROOM_NOT_FOUND
- OK: saída igual à entrada: 400 INVALID_REQUEST
- OK: nenhum hóspede: 400 INVALID_REQUEST
- FALHOU: corpo que não é JSON: 400 INVALID_REQUEST (esperado: 400 INVALID_REQUEST; veio: 500 INTERNAL_ERROR)
- OK: faltam campos: 400 INVALID_REQUEST
- OK: reserva que não existe: 404 RESERVATION_NOT_FOUND
- FALHOU: segunda via de reserva pendente: 409 OPERATION_NOT_ALLOWED (esperado: 409 OPERATION_NOT_ALLOWED; veio: 404)
- FALHOU: confirmar responde 200 com CONFIRMED (esperado: 200 CONFIRMED; veio: 404 )
- FALHOU: o pago é o total (599.76) (esperado: 599.76; veio: )
- FALHOU: confirmar de novo: 409 OPERATION_NOT_ALLOWED (esperado: 409 OPERATION_NOT_ALLOWED; veio: 404)
- FALHOU: o hóspede recebe o e-mail "Reserva confirmada - Pousada Maré Alta" (esperado: 1; veio: 0)
- FALHOU: o e-mail traz o comprovante ("Total: R$ 599,76") (esperado: um texto com 'Total: R$ 599,76'; veio: )
- FALHOU: a segunda via responde 200 com o título "Comprovante de reserva" (esperado: 200 Comprovante de reserva; veio: 404 )
- FALHOU: check-in responde 200 com CHECKED_IN (esperado: 200 CHECKED_IN; veio: 404 )
- FALHOU: cancelar depois do check-in: 409 OPERATION_NOT_ALLOWED (esperado: 409 OPERATION_NOT_ALLOWED; veio: 404)
- FALHOU: check-out responde 200 com o comprovante (esperado: 200 Comprovante de reserva ; veio: 404  )
- FALHOU: a reserva fica CHECKED_OUT (esperado: CHECKED_OUT; veio: )
- FALHOU: cancelar STANDARD confirmada devolve o pago (299.88) (esperado: CANCELLED 299.88; veio:  )
- FALHOU: cancelar NON_REFUNDABLE confirmada não devolve nada (0.00) (esperado: 0.00; veio: )
- FALHOU: cancelar reserva pendente não devolve nada (0.00) (esperado: 0.00; veio: )
- FALHOU: a reserva cancelada libera o quarto (esperado: 201; veio: 500)
- OK: o relatório de ocupação responde 200 em text/csv
- OK: ocupação: o cabeçalho
- FALHOU: ocupação: a reserva com check-out conta na noite de entrada (esperado: a linha '101,STANDARD,,2029-07-10,2029-07-12'; veio: room,type,reservation,check_in,check_out total,0/5)
- FALHOU: ocupação: o total (esperado: a linha 'total,1/5'; veio: room,type,reservation,check_in,check_out total,0/5)
- OK: financeiro: o cabeçalho
- FALHOU: financeiro: a reserva paga (esperado: a linha ',101,2029-07-10,2029-07-12,CHECKED_OUT,599.76'; veio: reservation,room,check_in,check_out,status,amount total,0)
- FALHOU: financeiro: o total (a cancelada com devolução não conta) (esperado: a linha 'total,599.76'; veio: reservation,room,check_in,check_out,status,amount total,0)
- FALHOU: financeiro: a NON_REFUNDABLE cancelada continua contando (esperado: a linha ',101,2029-03-10,2029-03-11,CANCELLED,212.42'; veio: reservation,room,check_in,check_out,status,amount total,0)
- OK: mês fora do formato: 400 INVALID_REQUEST

## Os dados continuam depois de reiniciar o serviço (o banco)

- FALHOU: a reserva continua lá, CHECKED_OUT (esperado: 200 CHECKED_OUT; veio: 404 )
- OK: as mensagens continuam lá
- FALHOU: o quarto continua ocupado depois de reiniciar (esperado: 409 ROOM_UNAVAILABLE; veio: 500 INTERNAL_ERROR)

## Cenário 05: o documento por tipo de hóspede

- FALHOU: com CPF: 201 (esperado: 201; veio: 500)
- FALHOU: com CPF: os campos que não se aplicam vêm null (esperado: 111.222.333-44|null|null|null|null; veio: null|null|null|null|null)
- FALHOU: com CPF: o documento continua o comprovante (esperado: Comprovante de reserva; veio: )
- FALHOU: com CPF: o texto do documento (esperado: um texto com 'Hóspede: Carla Dias (CPF 111.222.333-44)'; veio: )
- FALHOU: empresa (CNPJ e razão social): 201 (esperado: 201; veio: 500)
- FALHOU: empresa: os campos que não se aplicam vêm null (esperado: null|12.345.678/0001-90|Tecnosul Ltda|null|null; veio: null|null|null|null|null)
- FALHOU: empresa: a consulta da reserva mostra os cinco campos (esperado: null|12.345.678/0001-90|Tecnosul Ltda|null|null; veio: )
- FALHOU: empresa: o documento é a fatura (esperado: Fatura de hospedagem; veio: )
- FALHOU: empresa: o documento traz os cinco campos (esperado: null|12.345.678/0001-90|Tecnosul Ltda|null|null; veio: )
- FALHOU: fatura: a primeira linha (esperado: um texto com 'Fatura de hospedagem nº '; veio: )
- FALHOU: fatura: a empresa no lugar do CPF (esperado: um texto com 'Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)'; veio: )
- FALHOU: fatura: o hóspede (esperado: um texto com 'Hóspede: Ana Souza'; veio: )
- FALHOU: fatura: o ISS retido de 5% (esperado: um texto com 'ISS retido (5%): R$ 38,99'; veio: )
- FALHOU: fatura: o total (esperado: um texto com 'Total: R$ 779,80'; veio: )
- FALHOU: empresa: o e-mail (em português) leva a fatura (esperado: um texto com 'Fatura de hospedagem nº '; veio: )
- FALHOU: empresa: o e-mail continua em português (esperado: um texto com 'Olá, Ana Souza!'; veio: )
- FALHOU: empresa: o check-out devolve a fatura (esperado: 200 Fatura de hospedagem; veio: 404 )
- OK: estrangeiro (passaporte e país): 201
- OK: estrangeiro: os campos que não se aplicam vêm null
- OK: estrangeiro: o documento é o booking receipt
- OK: booking receipt: a primeira linha
- OK: booking receipt: o hóspede
- OK: booking receipt: as datas no formato 2027-03-10
- OK: booking receipt: as diárias em BRL
- OK: booking receipt: o total em BRL
- OK: booking receipt de uma noite: "1 night"
- OK: booking receipt com desconto: a linha Discount
- OK: booking receipt: milhar com vírgula (BRL 2,783.52)
- OK: sem nenhuma identificação: 400 INVALID_REQUEST
- OK: CPF e CNPJ juntos: 400 INVALID_REQUEST
- OK: CNPJ sem a razão social: 400 INVALID_REQUEST
- OK: passaporte sem o país: 400 INVALID_REQUEST

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
