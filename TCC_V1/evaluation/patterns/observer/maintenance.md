Este avaliador cuida de um pedido, que você reconhece pelo texto do `pedido.md`: o de **avisar a equipe quando a
reserva é confirmada ou cancelada** (cenário 03).

## O ponto de partida: o que havia na base

Ao confirmar uma reserva, `ReservationService.confirm` monta o comprovante e chama o `EmailSender` direto, com o texto
do e-mail escrito em `confirmationEmailBody`. Havia um único interessado na confirmação, o hóspede, e nenhum no
cancelamento. A montagem do sistema é a classe `Hotel`. O e-mail do hóspede deve continuar igual ao da base.
