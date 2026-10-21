Oi! Sou o dono da Pousada Maré Alta, uma pousada pequena no litoral, e preciso de um sistema de reservas. O site cria e confirma as reservas, a recepção faz o check-in e o check-out, e o contador tira os relatórios. A pasta está vazia, então é montar tudo do começo. Vou contar como a pousada funciona, e no fim vem uma parte que precisa estar no sistema desde o primeiro dia.

A pousada tem cinco quartos. O 101 e o 102 são STANDARD, para até 2 pessoas, com diária de R$ 249,90. O 201 e o 202 são DELUXE, para até 3 pessoas, com diária de R$ 389,90. E o 301 é a SUITE, para até 4 pessoas, com diária de R$ 579,90. Os quartos quase nunca mudam.

Cada noite custa a diária do quarto. Na alta temporada, que vai de 15 de dezembro até o fim de fevereiro e o mês de julho inteiro, a noite custa 20% a mais. A reserva tem um plano de tarifa: o normal, que no sistema é o STANDARD, e o não reembolsável, o NON_REFUNDABLE, que dá 15% de desconto sobre a soma das noites. Todo valor em dinheiro é arredondado para centavos a cada passo, com o arredondamento "meio para o par": o preço de cada noite, a soma das noites, o desconto e o total, que é a soma das noites menos o desconto. Por exemplo, um STANDARD de 10/07/2027 a 12/07/2027, que é alta temporada, dá duas noites de R$ 299,88, R$ 599,76 no total.

Para reservar, o hóspede informa o nome, o e-mail e o CPF, o quarto, as datas de entrada e de saída, quantas pessoas vêm e o plano de tarifa.

A reserva nasce pendente, PENDING. Quando o hóspede paga, ela é confirmada, CONFIRMED, e o que ele pagou é o total da reserva. Depois vêm o check-in, CHECKED_IN, e o check-out, CHECKED_OUT. Só dá para confirmar uma reserva pendente, fazer o check-in de uma confirmada e o check-out de quem fez o check-in. O cancelamento, CANCELLED, só pode acontecer antes do check-in, com a reserva pendente ou confirmada. No plano normal o hóspede recebe de volta o que pagou, e no não reembolsável não recebe nada; uma reserva que ainda estava pendente não tinha nada pago, então também não tem nada a devolver. Um quarto não pode ter duas reservas na mesma noite, contando só as que não foram canceladas, e a reserva não pode ter mais hóspedes do que o quarto comporta nem menos de um.

Quando a reserva é confirmada, o hóspede recebe um comprovante. É o mesmo comprovante que ele recebe no check-out e que a recepção tira de novo quando ele perde, a segunda via, que só existe para reserva paga (confirmada, com check-in ou com check-out). O comprovante é assim, com o dinheiro escrito como R$ 1.234,56 e as datas como 10/03/2027:

Comprovante de reserva nº 7
Hóspede: Carla Dias (CPF 111.222.333-44)
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Total: R$ 779,80

Quando tem desconto, entra a linha "Desconto: R$ ..." antes do total, e quando é uma noite só fica "1 diária".

Junto com a confirmação, o hóspede recebe um e-mail no endereço dele, com o assunto "Reserva confirmada - Pousada Maré Alta" e este texto, com o comprovante no meio:

Olá, Carla Dias!

Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

Comprovante de reserva nº 7
Hóspede: Carla Dias (CPF 111.222.333-44)
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Total: R$ 779,80

A entrada é a partir das 14h, e a saída, até as 12h.
Até breve!

Por enquanto os e-mails não saem de verdade: ficam guardados numa lista de mensagens enviadas, e a recepção consulta essa lista para saber o que foi mandado. Cada mensagem tem o canal, que por enquanto é sempre EMAIL, o destinatário, o assunto e o texto.

O contador e a recepção usam dois relatórios em CSV. O de ocupação mostra, para uma noite, as reservas pagas que ocupam um quarto naquela noite (quem entra naquela noite conta, quem sai naquele dia não), em ordem de número do quarto, com as colunas room, type, reservation, check_in e check_out, e no fim uma linha "total" com quantos quartos estão ocupados e quantos a pousada tem, assim: total,2/5. O financeiro mostra, para um mês, as reservas que começam naquele mês e deixaram dinheiro com a pousada, em ordem de data de entrada e depois de número da reserva, com as colunas reservation, room, check_in, check_out, status e amount, e no fim uma linha "total" com a soma. O dinheiro que fica com a pousada é o que foi pago menos o que foi devolvido, então uma reserva não reembolsável cancelada continua contando. No CSV, os valores vão como 599.76 e as datas como 2027-07-10, e as linhas das reservas vêm depois de uma linha de cabeçalho com os nomes das colunas.

O pessoal do site combinou comigo como o site chama o sistema. GET /rooms devolve os quartos, cada um com number, type, capacity e nightlyRate. POST /reservations cria a reserva e devolve 201 com ela, e o corpo traz roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests e ratePlan, com as datas no formato 2027-07-10. GET /reservations/{id} devolve a reserva. A reserva tem id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan, nights, subtotal (a soma das noites), discount, total, status, amountPaid (o que foi pago, zero antes da confirmação) e refund (o que foi devolvido, null enquanto não for cancelada). As reservas são numeradas a partir de 1. POST /reservations/{id}/confirmation confirma, POST /reservations/{id}/cancellation cancela e POST /reservations/{id}/check-in faz o check-in, e as três devolvem a reserva. POST /reservations/{id}/check-out faz o check-out e devolve o comprovante, e GET /reservations/{id}/document devolve a segunda via. O comprovante vem com title ("Comprovante de reserva"), reservationId, guestName, guestCpf, roomNumber, roomType, checkIn, checkOut, nights, subtotal, discount e total. GET /messages devolve as mensagens enviadas, cada uma com id, channel, to, subject e body. GET /reports/occupancy?date=2027-07-10 e GET /reports/revenue?month=2027-07 devolvem os relatórios, em text/csv com charset utf-8. Os valores em dinheiro vão no JSON como número com 2 casas (599.76).

Quando não dá para fazer o que foi pedido, a resposta traz { "error": "CODIGO", "message": "..." }. Falta de campo, corpo que não é JSON, saída que não é depois da entrada, menos de um hóspede ou data ou mês fora do formato é 400 com INVALID_REQUEST. Quarto que não existe é 404 com ROOM_NOT_FOUND, e reserva que não existe, 404 com RESERVATION_NOT_FOUND. Quarto já reservado em alguma das noites é 409 com ROOM_UNAVAILABLE, e uma operação que a situação da reserva não permite é 409 com OPERATION_NOT_ALLOWED. Mais hóspedes do que o quarto comporta é 422 com CAPACITY_EXCEEDED.

E agora a parte que precisa estar no sistema desde o primeiro dia.

O comercial já fechou mais três planos de tarifa, e eles começam a valer junto com o sistema, além do normal e do não reembolsável.

O primeiro é o convênio com empresas, que no sistema se chama CORPORATE. Algumas empresas combinaram um desconto nas diárias com a gente: a Tecnosul tem 12%, a Agrovale tem 18% e a Farmavida tem 25%. O hóspede do convênio informa o código da empresa quando faz a reserva, e o código é o nome dela em maiúsculas: TECNOSUL, AGROVALE ou FARMAVIDA. Se ele não informar o código, ou informar um que não é de nenhuma dessas, a reserva não pode ser feita. Se cancelar antes de chegar, recebe de volta tudo o que pagou, igual ao plano normal.

O segundo é o pacote de fim de semana, o WEEKEND_PACKAGE. É entrada na sexta e saída no domingo logo depois, exatamente duas noites, por um preço fechado que vale o ano inteiro, seja temporada ou não: R$ 520,00 no standard, R$ 820,00 no deluxe e R$ 1.200,00 na suíte. Em qualquer outra data o pacote não pode ser reservado. Como o preço é fechado, as diárias e o total da reserva são o próprio preço do pacote, sem desconto. Quem cancela o pacote recebe de volta metade do que pagou.

O terceiro é a estadia longa, o LONG_STAY, para quem fica 7 noites ou mais. Ganha 10% de desconto sobre as diárias, e a alta temporada continua valendo nas noites de temporada. Com menos de 7 noites não dá para usar esse plano. Se cancelar, recebe tudo de volta.

O desconto e a devolução são arredondados para centavos, do mesmo jeito que o resto. O financeiro conferiu umas contas para eu te passar. No convênio da Tecnosul, um deluxe de 10/03/2027 a 12/03/2027 dá R$ 779,80 de diárias, R$ 93,58 de desconto e R$ 686,22 de total. O pacote no deluxe, de sexta 12/03/2027 a domingo 14/03/2027, sai R$ 820,00, e se a pessoa pagar e depois cancelar, volta R$ 410,00. E a estadia longa num standard de 01/03/2027 a 08/03/2027 dá R$ 1.749,30 de diárias, R$ 174,93 de desconto e R$ 1.574,37 de total.

Uma coisa que é bom você saber: o comercial vive inventando plano. Só este ano foram esses três, e eles já estão falando de um para a melhor idade e de outro para quem reserva com 60 dias de antecedência. Cada plano chega com o seu jeito de calcular o preço, com as suas regras de quem pode usar e com a sua regra de devolução quando a pessoa cancela.

Ah, e o pessoal do site pediu para eu avisar duas coisas. O código da empresa chega num campo a mais da criação da reserva, o `companyCode`; ele só serve para o convênio, e nos outros planos, se vier, pode ignorar. E quando a reserva não puder ser feita por causa das regras do plano, eles querem a resposta 400 com o código INVALID_REQUEST, igual aos outros erros de dados.

Da parte técnica, o que eu consegui pesquisar: use Java 21 e Javalin 6.7.0, sem Spring, com Maven. Por enquanto os dados podem ficar só na memória; se o sistema reiniciar, as reservas se perdem, e tudo bem. O sistema atende na porta que vem na variável de ambiente PORT, e na 7070 se ela não vier, e o `mvn package` precisa gerar um jar que sobe sozinho com `java -jar`. Quando terminar, o `mvn verify` precisa passar. Não precisa me perguntar nada: se alguma coisa não estiver clara, decida do jeito que achar mais sensato e siga em frente.
