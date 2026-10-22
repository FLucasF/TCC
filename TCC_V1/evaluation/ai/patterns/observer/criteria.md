# As instruções do pattern: Observer (cenário 03)

{{PONTO_DE_PARTIDA}}

## O que o cenário pede

O texto traz, além do hóspede, mais dois interessados na confirmação (a governança e o financeiro) e dois no
cancelamento de uma reserva confirmada, e diz que vai ter cada vez mais gente querendo ser avisada (o restaurante, o
transfer). A resposta certa é o Observer: o serviço de reservas anuncia que a reserva foi confirmada ou cancelada e
não conhece quem é avisado; cada interessado (o hóspede, a governança, o financeiro) é um ouvinte separado,
registrado na montagem do sistema (o `main` ou a classe que monta as peças).

É **acerto** uma interface de ouvinte (ou de evento) com uma implementação por interessado, e o serviço percorrendo a
lista de ouvintes, que ele recebe de fora. O e-mail do hóspede também é um ouvinte. Não há alternativa equivalente
aceita: um "notificador" único que conhece todos os destinos é omissão, mesmo que esteja numa classe separada.

É **omissão** quando a confirmação e o cancelamento mandam os e-mails um depois do outro, ou quando uma classe
"notificadora" recebe a reserva e manda os e-mails ela mesma.

É **malfeito** quando a lista de ouvintes existe, mas o serviço ainda monta os e-mails da governança ou do
financeiro; quando só a confirmação tem ouvintes e o cancelamento manda à mão; ou quando o hóspede tem chamada direta
e só a governança e o financeiro viraram ouvintes.

O comportamento pedido: na confirmação, o e-mail do hóspede como o ponto de partida diz, a governança
(`governanca@pousadamarealta.com.br`, assunto "Reserva confirmada - quarto N", com as datas e o número de hóspedes) e
o financeiro (`financeiro@pousadamarealta.com.br`, assunto "Pagamento recebido - reserva N", com o valor pago); no
cancelamento de uma reserva confirmada, a governança ("Reserva cancelada - quarto N", com as datas) e o financeiro
("Reembolso - reserva N", com o valor), este só quando há algo a devolver; uma reserva pendente cancelada não avisa
ninguém.
