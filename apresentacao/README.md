# Apresentação da defesa

Apresentação em reveal.js, com 32 slides e 4 de reserva (o glossário, as duas perguntas do Semgrep em detalhe e os dois verificadores estão na reserva). Todos os resultados são slides principais: as hipóteses, o pattern certo por pattern e tarefa, a exceção do Strategy, as dimensões e "além do pattern". Depois da lacuna vêm os trabalhos relacionados, e depois dos níveis, o texto real do `CLAUDE.md`. A parte "Método" (slides 10 a 15) mostra como a pesquisa evoluiu, onde o estudo rodou, como cada execução foi isolada, a execução e a medida, o Semgrep (um slide, com um exemplo real do cupom) e as outras ferramentas. Depois do resultado principal há um exemplo real de código (Opus 4.7, N0 e N1). O visual segue um modelo corporativo em vermelho, preto e branco: faixa vermelha lateral com o nome do capítulo, rótulos em caixa e capa em bloco vermelho; as cores ficam em `style.css`, no começo do arquivo. Funciona sem internet: a biblioteca está em `vendor/reveal/` (versão 5.1.0).

## Como abrir

Dentro desta pasta, rode um servidor local e abra o endereço no navegador:

```
python servir.py 8766
```

Depois, acesse http://localhost:8766. O `servir.py` avisa o navegador para nunca guardar cache, então uma mudança nos arquivos aparece num F5 comum. Abrir o `index.html` com dois cliques também costuma funcionar, mas alguns navegadores bloqueiam partes da página quando ela é aberta como arquivo local.

## Como apresentar

- **→**, **espaço** ou clique: avança. Todo fluxo e todo gráfico avança só no clique; nada se move sozinho.
- **←**: volta um passo.
- **S**: abre a janela do apresentador, com notas, próximo slide e cronômetro.
- **ESC**: mostra todos os slides de uma vez.
- **F**: tela cheia.
- Os slides de reserva ficam depois do "Obrigado".

## O que ajuda quem não é da área

- **Trilho no rodapé**: mostra as partes da apresentação e onde estamos. A bolinha laranja é a mesma que percorre o ciclo do agente no slide 5.
- **Faixa "em outras palavras"**: nos slides com termos técnicos, o último clique traz a ideia em linguagem simples.
- **Legenda dos níveis**: nos slides de resultado, o canto superior direito lembra o que são N0, N1, N2 e N3, sempre com as mesmas cores.
- **Glossário**: é o slide 2, logo depois da capa, para explicar os termos antes de usá-los.
- Na parte da extensão há dois slides sobre quem revisou: o verificador oficial e o segundo verificador.

## Como mudar

- **Cores e fontes**: no começo do `style.css`, no bloco `:root`. As fontes (Bahnschrift, Segoe UI e Cascadia Mono) já vêm no Windows. Cada nível (N0 a N3) tem uma variável própria (`--n0` a `--n3`), usada em todos os gráficos e diagramas.
- **Texto dos slides**: no `index.html`, um bloco `<section>` por slide, com um comentário que indica o número.
- **Vídeo do slide 2**: é o arquivo `media/agente.webm`, uma reprodução de uma sessão real. Para trocar por uma gravação sua, salve o vídeo na pasta `media` e mude o nome no `index.html`. O vídeo só toca quando você clica nele.

## PDF de reserva

Abra http://localhost:8766/?print-pdf no Chrome e imprima como PDF, com margens "nenhuma" e "gráficos de fundo" ligado. Cada passo das animações vira uma página.
