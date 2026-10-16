# As instruções do pattern: Observer

Este avaliador cuida de dois pedidos, que você reconhece pelo texto do `pedido.md`: o de **avisar a equipe
quando a reserva é confirmada ou cancelada** (cenário 03, que pede o pattern) e o do **texto do e-mail de
confirmação** (cenário 04, controle negativo).

## O que havia na base

Ao confirmar uma reserva, `ReservationService.confirm` monta o comprovante e chama o `EmailSender` direto, com o
texto do e-mail escrito em `confirmationEmailBody`. Havia um único interessado na confirmação, o hóspede, e
nenhum no cancelamento.

## Cenário 03: avisar a equipe (pede o pattern)

O pedido acrescenta, agora, mais dois interessados na confirmação (a governança e o financeiro) e dois no
cancelamento de uma reserva confirmada, e diz que vai ter cada vez mais gente querendo ser avisada (o
restaurante, o transfer). A resposta certa é o Observer: o serviço de reservas anuncia que a reserva foi
confirmada ou cancelada e não conhece quem é avisado; cada interessado (o hóspede, a governança, o financeiro) é
um ouvinte separado, registrado na montagem do sistema (a classe `Hotel`).

É **acerto** uma interface de ouvinte (ou de evento) com uma implementação por interessado, e o serviço
percorrendo a lista de ouvintes, que ele recebe de fora. O e-mail do hóspede, que já existia, também vira um
ouvinte. Não há alternativa equivalente aceita: um "notificador" único que conhece todos os destinos é omissão,
mesmo que esteja numa classe separada.

É **omissão** quando `confirm` e `cancel` mandam os e-mails um depois do outro, ou quando uma classe
"notificadora" recebe a reserva e manda os três e-mails ela mesma.

É **malfeito** quando a lista de ouvintes existe, mas o serviço ainda monta os e-mails da governança ou do
financeiro; quando só a confirmação passou a ter ouvintes e o cancelamento manda à mão; ou quando o hóspede
continua com chamada direta e só os novos viraram ouvintes.

O comportamento pedido: na confirmação, o e-mail do hóspede igual ao de antes, a governança
(`governanca@pousadamarealta.com.br`, assunto "Reserva confirmada - quarto N", com as datas e o número de
hóspedes) e o financeiro (`financeiro@pousadamarealta.com.br`, assunto "Pagamento recebido - reserva N", com o
valor pago); no cancelamento de uma reserva confirmada, a governança ("Reserva cancelada - quarto N", com as
datas) e o financeiro ("Reembolso - reserva N", com o valor), este só quando há algo a devolver; uma reserva
pendente cancelada não avisa ninguém.

## Cenário 04: o e-mail de confirmação (controle negativo)

O pedido muda o assunto do e-mail de confirmação e troca uma linha do texto por três. A frase "talvez um dia a
gente mande essa confirmação por WhatsApp também" é a isca.

É **contenção** mudar o assunto e o texto onde eles já estão (em `confirmationEmailBody`, ou onde o texto for
montado).

É **exagero** criar um canal de mensagem abstrato, uma interface de confirmação com uma implementação de e-mail,
um ouvinte de confirmação, ou um modelo de mensagem com várias implementações.

O comportamento pedido: o assunto "Sua reserva na Pousada Maré Alta está confirmada!"; as três linhas novas no
lugar da linha dos horários; a saudação, o comprovante e o "Até breve!" iguais.
