Faça uma cópia do `projeto/` (nunca mude o `projeto/`), rode `mvn -q package -DskipTests` nela e suba o serviço com
`PORT=7070 java -jar <o jar que o build gerou> &` (o que tem a classe principal no manifesto). Depois chame a API com
`curl` (e o `jq`, se ajudar), do jeito que o enunciado diz que o site chama, e confira as regras da parte do fim: os
valores, as recusas, as mensagens, os relatórios, o que ela disser. Confira também uma ou duas coisas da parte comum do
enunciado (um preço, uma recusa, o comprovante).
