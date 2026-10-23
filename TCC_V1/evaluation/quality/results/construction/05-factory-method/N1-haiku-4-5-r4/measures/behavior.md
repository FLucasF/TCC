# Comportamento: o roteiro do cenário 05

Resultado: 39 de 83 conferências passaram (44 falharam).
- A parte comum do enunciado: 26 de 46
- Os dados continuam depois de reiniciar o serviço (o banco): 1 de 3
- Cenário 05: o documento por tipo de hóspede: 11 de 32
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
- FALHOU: dinheiro no JSON como número com 2 casas (amountPaid 0.00) (esperado: "amountPaid":0.00; veio: {"id":1,"roomNumber":101,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44","checkIn":[2029,7,10],"checkOut":[2029,7,12],"guests":2,"ratePlan":"STANDARD","nights":2,"subtotal":599.76,"discount":0.0,"total")
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
- FALHOU: financeiro: a reserva paga (esperado: a linha '1,101,2029-07-10,2029-07-12,CHECKED_OUT,599.76'; veio: reservation,room,check_in,check_out,status,amount 1,101,2029-07-10,2029-07-12,CANCELLED,0.00 3,102,2029-07-20,2029-07-21,CANCELLED,0.00 total,0.00)
- FALHOU: financeiro: o total (a cancelada com devolução não conta) (esperado: a linha 'total,599.76'; veio: reservation,room,check_in,check_out,status,amount 1,101,2029-07-10,2029-07-12,CANCELLED,0.00 3,102,2029-07-20,2029-07-21,CANCELLED,0.00 total,0.00)
- FALHOU: financeiro: a NON_REFUNDABLE cancelada continua contando (esperado: a linha '2,101,2029-03-10,2029-03-11,CANCELLED,212.42'; veio: reservation,room,check_in,check_out,status,amount 2,101,2029-03-10,2029-03-11,CANCELLED,0.00 total,0.00)
- FALHOU: mês fora do formato: 400 INVALID_REQUEST (esperado: 400 INVALID_REQUEST; veio: 500 INTERNAL_ERROR)

## Os dados continuam depois de reiniciar o serviço (o banco)

- FALHOU: a reserva continua lá, CHECKED_OUT (esperado: 200 CHECKED_OUT; veio: 200 CANCELLED)
- OK: as mensagens continuam lá
- FALHOU: o quarto continua ocupado depois de reiniciar (esperado: 409 ROOM_UNAVAILABLE; veio: 201)

## Cenário 05: o documento por tipo de hóspede

- OK: com CPF: 201
- OK: com CPF: os campos que não se aplicam vêm null
- FALHOU: com CPF: o documento continua o comprovante (esperado: Comprovante de reserva; veio: null)
- FALHOU: com CPF: o texto do documento (esperado: um texto com 'Hóspede: Carla Dias (CPF 111.222.333-44)'; veio: )
- OK: empresa (CNPJ e razão social): 201
- OK: empresa: os campos que não se aplicam vêm null
- OK: empresa: a consulta da reserva mostra os cinco campos
- FALHOU: empresa: o documento é a fatura (esperado: Fatura de hospedagem; veio: null)
- FALHOU: empresa: o documento traz os cinco campos (esperado: null|12.345.678/0001-90|Tecnosul Ltda|null|null; veio: null|null|null|null|null)
- FALHOU: fatura: a primeira linha (esperado: um texto com 'Fatura de hospedagem nº 2'; veio: )
- FALHOU: fatura: a empresa no lugar do CPF (esperado: um texto com 'Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)'; veio: )
- FALHOU: fatura: o hóspede (esperado: um texto com 'Hóspede: Ana Souza'; veio: )
- FALHOU: fatura: o ISS retido de 5% (esperado: um texto com 'ISS retido (5%): R$ 38,99'; veio: )
- FALHOU: fatura: o total (esperado: um texto com 'Total: R$ 779,80'; veio: )
- FALHOU: empresa: o e-mail (em português) leva a fatura (esperado: um texto com 'Fatura de hospedagem nº 2'; veio: )
- FALHOU: empresa: o e-mail continua em português (esperado: um texto com 'Olá, Ana Souza!'; veio: )
- FALHOU: empresa: o check-out devolve a fatura (esperado: 200 Fatura de hospedagem; veio: 409 null)
- OK: estrangeiro (passaporte e país): 201
- OK: estrangeiro: os campos que não se aplicam vêm null
- FALHOU: estrangeiro: o documento é o booking receipt (esperado: Booking receipt; veio: null)
- FALHOU: booking receipt: a primeira linha (esperado: um texto com 'Booking receipt no. 3'; veio: )
- FALHOU: booking receipt: o hóspede (esperado: um texto com 'Guest: John Smith (passport X1234567, United Kingdom)'; veio: )
- FALHOU: booking receipt: as datas no formato 2027-03-10 (esperado: um texto com 'Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights'; veio: )
- FALHOU: booking receipt: as diárias em BRL (esperado: um texto com 'Nights: BRL 779.80'; veio: )
- FALHOU: booking receipt: o total em BRL (esperado: um texto com 'Total: BRL 779.80'; veio: )
- FALHOU: booking receipt de uma noite: "1 night" (esperado: um texto com '| 1 night'; veio: )
- FALHOU: booking receipt com desconto: a linha Discount (esperado: um texto com 'Discount: BRL 58.48'; veio: )
- FALHOU: booking receipt: milhar com vírgula (BRL 2,783.52) (esperado: um texto com 'Total: BRL 2,783.52'; veio: )
- OK: sem nenhuma identificação: 400 INVALID_REQUEST
- OK: CPF e CNPJ juntos: 400 INVALID_REQUEST
- OK: CNPJ sem a razão social: 400 INVALID_REQUEST
- OK: passaporte sem o país: 400 INVALID_REQUEST

## Duas reservas do mesmo quarto, ao mesmo tempo

- FALHOU: em 10 rodadas, nenhuma com as duas reservas criadas (esperado: 0; veio: 10)
- OK: em 10 rodadas, todas com uma criada e a outra recusada com 409
