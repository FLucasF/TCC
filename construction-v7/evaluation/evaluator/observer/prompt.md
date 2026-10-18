# As instruções do pattern: Observer

Este avaliador cuida de dois enunciados, que você reconhece pela parte do fim do `enunciado.md`: o de **avisar a
equipe quando a reserva é confirmada ou cancelada** (cenário 03, que pede o pattern) e o do **texto do e-mail de
confirmação** (cenário 04, controle negativo).

## O que o enunciado descreve

Na parte comum, quando a reserva é confirmada, o hóspede recebe um e-mail com o comprovante; os e-mails não saem de
verdade e ficam numa lista de mensagens enviadas.

## Cenário 03: avisar a equipe (pede o pattern)

O enunciado traz, além do hóspede, mais dois interessados na confirmação (a governança e o financeiro) e dois no
cancelamento de uma reserva confirmada, e diz que vai ter cada vez mais gente querendo ser avisada (o restaurante,
o transfer). A resposta certa é o Observer: o serviço de reservas anuncia que a reserva foi confirmada ou cancelada
e não conhece quem é avisado; cada interessado (o hóspede, a governança, o financeiro) é um ouvinte separado,
registrado na montagem do sistema (o `main` ou a classe que monta as peças).

É **acerto** uma interface de ouvinte (ou de evento) com uma implementação por interessado, e o serviço percorrendo
a lista de ouvintes, que ele recebe de fora. O e-mail do hóspede também é um ouvinte. Não há alternativa
equivalente aceita: um "notificador" único que conhece todos os destinos é omissão, mesmo que esteja numa classe
separada.

É **omissão** quando a confirmação e o cancelamento mandam os e-mails um depois do outro, ou quando uma classe
"notificadora" recebe a reserva e manda os e-mails ela mesma.

É **malfeito** quando a lista de ouvintes existe, mas o serviço ainda monta os e-mails da governança ou do
financeiro; quando só a confirmação tem ouvintes e o cancelamento manda à mão; ou quando o hóspede tem chamada
direta e só a governança e o financeiro viraram ouvintes.

O comportamento pedido: na confirmação, o e-mail do hóspede como a parte comum descreve, a governança
(`governanca@pousadamarealta.com.br`, assunto "Reserva confirmada - quarto N", com as datas e o número de
hóspedes) e o financeiro (`financeiro@pousadamarealta.com.br`, assunto "Pagamento recebido - reserva N", com o
valor pago); no cancelamento de uma reserva confirmada, a governança ("Reserva cancelada - quarto N", com as datas)
e o financeiro ("Reembolso - reserva N", com o valor), este só quando há algo a devolver; uma reserva pendente
cancelada não avisa ninguém.

## Cenário 04: o e-mail de confirmação (controle negativo)

O enunciado dá o assunto e o texto do e-mail de confirmação. A frase "talvez um dia a gente mande essa confirmação
por WhatsApp também" é a isca.

É **contenção** o assunto e o texto montados onde a confirmação manda o e-mail (um método ou um modelo de texto),
mandando pelo envio de e-mail do sistema.

É **exagero** criar um canal de mensagem abstrato, uma interface de confirmação com uma implementação de e-mail, um
ouvinte de confirmação, ou um modelo de mensagem com várias implementações.

O comportamento pedido: o assunto "Sua reserva na Pousada Maré Alta está confirmada!"; a saudação, o comprovante,
as três linhas (entrada e saída, café da manhã, telefone da recepção) e o "Até breve!", como no enunciado.
