Este avaliador cuida de um pedido, que você reconhece pelo texto do `pedido.md`: o do **comprovante para empresa e
para estrangeiro** (cenário 05).

## O ponto de partida: o que havia na base

O comprovante (`ReservationReceipt`) é montado com `new` em três lugares do `ReservationService`: `confirm` (que o
manda por e-mail), `checkOut` e `receiptCopy` (a segunda via). Havia um único tipo de comprovante. A reserva
(`Reservation`) e o pedido de reserva (`NewReservation`) são objetos de dados criados direto. A pessoa com CPF deve
continuar recebendo o comprovante da base.
