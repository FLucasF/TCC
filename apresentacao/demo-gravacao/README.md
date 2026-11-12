# Projeto para gravar o vídeo do slide 2

Um projeto Java pequeno, com a mesma regra dos slides 5 e 7: uma classe `Planos` que decide o preço e o limite com condicionais, para os planos BASICO e PLUS, e 5 testes que passam. O vídeo mostra o Claude Code acrescentando o plano Premium.

Requer Java 21 e Maven (os dois já estão instalados nesta máquina).

## Antes de gravar

1. Abra um terminal **só com esta pasta**, para que o caminho com o seu nome de usuário apareça o mínimo possível.
2. Use um tema claro no terminal e aumente a fonte (Ctrl + roda do mouse) para uns 150% a 200%.
3. Feche outras abas e esconda a barra de uso ou cota do Claude, se ela aparecer.
4. Confira que o projeto está limpo: `Planos.java` só com BASICO e PLUS. Se já gravou uma vez, desfaça a mudança antes de gravar de novo.

## O que gravar

1. Rode `claude` nesta pasta.
2. Digite o pedido:

   > Acrescente o plano PREMIUM: 20% de desconto no preço e limite de 10 usuários. Escreva os testes e rode o mvn test no final.

3. Deixe o agente trabalhar até o fim, sem interromper: ele lê os arquivos, muda o código, escreve os testes e roda o `mvn test`.
4. Pare a gravação depois da mensagem final.

Grave só a janela do terminal, sem som. Pode ser com a Ferramenta de Captura do Windows (Win+Shift+S e o ícone de câmera) ou com o Xbox Game Bar (Win+Alt+R).

## Depois

1. No Clipchamp, corte os trechos parados e acelere de 3 a 6 vezes, até ficar com 15 a 20 segundos.
2. Exporte em MP4, com 1280×720 ou 1920×1080.
3. Salve como `J:\TCC\apresentacao\media\agente.mp4`.

Sem `CLAUDE.md`, o agente provavelmente vai acrescentar mais um `if` em cada método, como no lado esquerdo do slide 7. Isso não é problema: o vídeo mostra o agente agindo, e não o *design*.
