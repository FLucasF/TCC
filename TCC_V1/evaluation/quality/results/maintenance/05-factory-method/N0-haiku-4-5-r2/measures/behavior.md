# Comportamento: o roteiro do cenário 05

Resultado: 67 de 83 conferências passaram (16 falharam).
- A parte comum do enunciado: 46 de 46
- Os dados continuam depois de reiniciar o serviço (o banco): 3 de 3
- Cenário 05: o documento por tipo de hóspede: 16 de 32
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

## Cenário 05: o documento por tipo de hóspede

- OK: com CPF: 201
- OK: com CPF: os campos que não se aplicam vêm null
- OK: com CPF: o documento continua o comprovante
- OK: com CPF: o texto do documento
- OK: empresa (CNPJ e razão social): 201
- OK: empresa: os campos que não se aplicam vêm null
- OK: empresa: a consulta da reserva mostra os cinco campos
- FALHOU: empresa: o documento é a fatura (esperado: Fatura de hospedagem; veio: null)
- OK: empresa: o documento traz os cinco campos
- FALHOU: fatura: a primeira linha (esperado: um texto com 'Fatura de hospedagem nº 2'; veio: )
- FALHOU: fatura: a empresa no lugar do CPF (esperado: um texto com 'Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)'; veio: )
- FALHOU: fatura: o hóspede (esperado: um texto com 'Hóspede: Ana Souza'; veio: )
- FALHOU: fatura: o ISS retido de 5% (esperado: um texto com 'ISS retido (5%): R$ 38,99'; veio: )
- FALHOU: fatura: o total (esperado: um texto com 'Total: R$ 779,80'; veio: )
- OK: empresa: o e-mail (em português) leva a fatura
- OK: empresa: o e-mail continua em português
- FALHOU: empresa: o check-out devolve a fatura (esperado: 200 Fatura de hospedagem; veio: 200 null)
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

- OK: em 10 rodadas, nenhuma com as duas reservas criadas
- OK: em 10 rodadas, todas com uma criada e a outra recusada com 409
