Faça uma cópia da `final/` (nunca mude a `final/` nem a `base/`), rode `mvn -q package -DskipTests` nela e suba o
serviço com `PORT=7070 java -jar <o jar que o build gerou> &` (o da base é `target/hotel.jar`). Depois chame a API com
`curl` (e o `jq`, se ajudar), do jeito que o site chamaria, e confira as regras do pedido: os valores, as recusas, as
mensagens, os relatórios, o que o pedido disser. Confira também uma ou duas coisas que o pedido não mudou, para ver se
continuam iguais. O README da `base/` mostra a API.
