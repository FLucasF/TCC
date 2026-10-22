# Comportamento: o roteiro do cenário 07

Resultado: 18 de 63 conferências passaram (45 falharam).
- A parte comum do enunciado: 17 de 46
- Os dados continuam depois de reiniciar o serviço (o banco): 0 de 1
- Cenário 07: as regras de envio, no modo normal: 0 de 9
- Cenário 07: as regras de envio, no modo de teste (HOTEL_TEST_MODE=true): 0 de 5
- Duas reservas do mesmo quarto, ao mesmo tempo: 1 de 2

## A parte comum do enunciado

- FALHOU: GET /rooms responde 200 (esperado: 200; veio: 404)
- FALHOU: cinco quartos (esperado: 5; veio: )
- FALHOU: o 201 comporta 3 (esperado: 3; veio: )
- FALHOU: a diária do 201 é 389.90 (esperado: 389.90; veio: )
- FALHOU: criar uma reserva responde 201 (esperado: 201; veio: 404)
- FALHOU: a reserva nasce PENDING (esperado: PENDING; veio: )
- FALHOU: STANDARD de 10 a 12/07 (alta temporada): 2 noites (esperado: 2; veio: )
- FALHOU: STANDARD de 10 a 12/07: total 599.76 (esperado: 599.76; veio: )
- FALHOU: nada pago antes da confirmação (amountPaid 0.00) (esperado: 0.00; veio: )
- FALHOU: refund null antes do cancelamento (esperado: null; veio: )
- FALHOU: dinheiro no JSON como número com 2 casas (amountPaid 0.00) (esperado: "amountPaid":0.00; veio: Endpoint POST /reservations not found)
- FALHOU: as reservas são numeradas a partir de 1 (esperado: 1; veio: )
- OK: NON_REFUNDABLE de uma noite: desconto 37.48 (meio para o par)
- OK: NON_REFUNDABLE de uma noite: total 212.42
- FALHOU: quarto ocupado numa das noites: 409 ROOM_UNAVAILABLE (esperado: 409 ROOM_UNAVAILABLE; veio: 201)
- OK: mais hóspedes que a capacidade: 422 CAPACITY_EXCEEDED
- OK: quarto que não existe: 404 ROOM_NOT_FOUND
- OK: saída igual à entrada: 400 INVALID_REQUEST
- OK: nenhum hóspede: 400 INVALID_REQUEST
- OK: corpo que não é JSON: 400 INVALID_REQUEST
- OK: faltam campos: 400 INVALID_REQUEST
- OK: reserva que não existe: 404 RESERVATION_NOT_FOUND
- OK: segunda via de reserva pendente: 409 OPERATION_NOT_ALLOWED
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
- FALHOU: cancelar STANDARD confirmada devolve o pago (299.88) (esperado: CANCELLED 299.88; veio: CANCELLED 0.00)
- OK: cancelar NON_REFUNDABLE confirmada não devolve nada (0.00)
- OK: cancelar reserva pendente não devolve nada (0.00)
- OK: a reserva cancelada libera o quarto
- OK: o relatório de ocupação responde 200 em text/csv
- OK: ocupação: o cabeçalho
- FALHOU: ocupação: a reserva com check-out conta na noite de entrada (esperado: a linha '101,STANDARD,,2029-07-10,2029-07-12'; veio: room,type,reservation,check_in,check_out total,0/5)
- FALHOU: ocupação: o total (esperado: a linha 'total,1/5'; veio: room,type,reservation,check_in,check_out total,0/5)
- OK: financeiro: o cabeçalho
- FALHOU: financeiro: a reserva paga (esperado: a linha ',101,2029-07-10,2029-07-12,CHECKED_OUT,599.76'; veio: reservation,room,check_in,check_out,status,amount 3,102,2029-07-20,2029-07-21,CANCELLED,0.00 total,0.00)
- FALHOU: financeiro: o total (a cancelada com devolução não conta) (esperado: a linha 'total,599.76'; veio: reservation,room,check_in,check_out,status,amount 3,102,2029-07-20,2029-07-21,CANCELLED,0.00 total,0.00)
- FALHOU: financeiro: a NON_REFUNDABLE cancelada continua contando (esperado: a linha '1,101,2029-03-10,2029-03-11,CANCELLED,212.42'; veio: reservation,room,check_in,check_out,status,amount 1,101,2029-03-10,2029-03-11,CANCELLED,0.00 total,0.00)
- OK: mês fora do formato: 400 INVALID_REQUEST

## Os dados continuam depois de reiniciar o serviço (o banco)

- FALHOU: o serviço sobe de novo sobre o mesmo banco (esperado: subir; veio: não subiu)

## Cenário 07: as regras de envio, no modo normal

- FALHOU: endereço válido: a mensagem sai para o hóspede com status SENT (esperado: ana@example.com SENT; veio: )
- FALHOU: a cópia para o arquivo vem logo depois da original (esperado: arquivo@pousadamarealta.com.br; veio: )
- FALHOU: a cópia tem o assunto com "[Cópia] " (esperado: [Cópia] Reserva confirmada - Pousada Maré Alta; veio: )
- FALHOU: a cópia tem o mesmo texto (esperado: true; veio: )
- FALHOU: a cópia sai com status SENT (esperado: SENT; veio: )
- FALHOU: endereço inválido "ana@gmail": uma mensagem só, BLOCKED, com o endereço original, sem cópia (esperado: 1 ana@gmail BLOCKED; veio: 0 null null)
- FALHOU: endereço inválido "ana.gmail.com": uma mensagem só, BLOCKED, com o endereço original, sem cópia (esperado: 1 ana.gmail.com BLOCKED; veio: 0 null null)
- FALHOU: endereço inválido "ana@@gmail.com": uma mensagem só, BLOCKED, com o endereço original, sem cópia (esperado: 1 ana@@gmail.com BLOCKED; veio: 0 null null)
- FALHOU: endereço inválido "@gmail.com": uma mensagem só, BLOCKED, com o endereço original, sem cópia (esperado: 1 @gmail.com BLOCKED; veio: 0 null null)

## Cenário 07: as regras de envio, no modo de teste (HOTEL_TEST_MODE=true)

- FALHOU: modo de teste: uma mensagem só (sem a cópia para o arquivo) (esperado: 1; veio: )
- FALHOU: modo de teste: vai para o endereço de testes, SENT (esperado: testes@pousadamarealta.com.br SENT; veio: )
- FALHOU: modo de teste: o assunto começa com "[TESTE] " (esperado: [TESTE] Reserva confirmada - Pousada Maré Alta; veio: )
- FALHOU: modo de teste: a primeira linha diz o destinatário original (esperado: Destinatário original: ana@example.com; veio: )
- FALHOU: modo de teste: endereço inválido continua BLOCKED, com o endereço original (esperado: 1 ana@gmail BLOCKED; veio: )

## Duas reservas do mesmo quarto, ao mesmo tempo

- FALHOU: em 10 rodadas, nenhuma com as duas reservas criadas (esperado: 0; veio: 10)
- OK: em 10 rodadas, todas com uma criada e a outra recusada com 409
