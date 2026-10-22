Este avaliador cuida de um pedido, que você reconhece pelo texto do `pedido.md`: o das **regras para as mensagens que
a pousada manda** (cenário 07).

## O ponto de partida: o que havia na base

Todo envio passa por `EmailSender.send`, que só guarda a mensagem na lista de mensagens enviadas (`SentMessages`). Não
havia nenhum comportamento extra no envio. A montagem do sistema é a classe `Hotel`.
