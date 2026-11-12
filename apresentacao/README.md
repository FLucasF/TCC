# Apresentação da defesa

Apresentação em reveal.js, com 38 slides e 3 de reserva. A parte "Método" (slides 11 a 20) mostra onde e como o estudo rodou: a máquina, o contêiner de cada execução, as rodadas, a suíte de aceitação, o Semgrep (quatro slides, com a ideia de cada regra) e as outras ferramentas. Funciona sem internet: a biblioteca está em `vendor/reveal/` (versão 5.1.0).

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
