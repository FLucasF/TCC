# Explicações interativas (`explanations/`)

Um guia para estudar o V5 por dentro: **`guia-completo.html`**, um HTML único que abre com
dois cliques, sem internet. Não faz parte do pré-registro nem da análise; foi feito em
12/10/2026, depois da leitura do Lucas, para estudo e para explicar ao orientador.

Ele tem uma barra lateral com as quatro partes e um **roteiro de leitura** na ordem da
pesquisa (a pergunta e o desenho, a correção, rodar e medir, o acerto, o desenho, a qualidade,
a conferência, as hipóteses e a conclusão), que passa por todas as 53 abas.

| parte | o que explica |
|---|---|
| **1. O método** | a pergunta e o desenho; o desenho decisão por decisão (o resultado do V5 no topo; o ranking descritivo dos 11 modelos, a avaliação de cada um e o que surpreendeu; e 24 escolhas, cada uma com o porquê, o que deu no V5, o que foi descartado e o que custa: pares, quartetos, comparar com o vizinho, 5 réplicas, 11 modelos, a ordem, a cota, o enunciado, o ambiente, a medida, o pré-registro); as ferramentas, uma por uma (Claude Code, Docker, Maven, a suíte, Semgrep, SonarQube, CK, os scripts, os hashes, a estatística); a sequência de verificação (o que cada passo protege), o que conta como acerto (o gabarito e a régua, com exercícios), as 32 hipóteses com o veredito do V5, as regras de leitura do §4.1 e o porquê de cada limite, as ameaças à validade e as defesas, e o que falta |
| **2. Os scripts** | os 22 scripts do V5 que não são do Semgrep nem das métricas, **uma aba para cada um**: primeiro os 16 que rodaram nas 220 execuções, na ordem do caminho (`draw-order`, o `Dockerfile`, `run-levels`, `run-one`, `extract-meta`, a calculadora `ref-strategy`, a suíte `strategy`, `executor`, `acceptance`, `aggregate`, `verify`, `anonymize`, `sample`, `compare`, `nota`, `hipoteses`), depois os 6 testes (`check-prompt`, `mutants`, `validate-mutants`, `verify-teste`, `nota-teste`, `hipoteses-teste`). Cada aba abre com a ficha do script (o que é, o que entra e sai, como funciona, a prova de que rodou ou do que testa) e, embaixo, o script funcionando. O mapa explica a diferença entre os 16 e os 6, e a última aba é o fluxograma da bancada inteira |
| **3. O desenho** | o Semgrep, linha por linha: `pontos.mjs`, `gerar-regras.mjs`, as regras do P4 e do P5, `detect.sh`, `copia-limpa.mjs` e `sem-comentarios.mjs`, e o juiz (`classificar.mjs`) rodando sobre 12 pacotes inventados, com as respostas comparadas às da régua |
| **4. A qualidade** | o SonarQube e o CK: as complexidades ciclomática e cognitiva, a duplicação, os code smells, as métricas de classe do CK (com o que ele faz no V5: a versão, os parâmetros, por que entra e o que deu), os scripts `metrics.sh` e `aggregate-metrics.mjs`, e os cuidados de leitura (o que as métricas não dizem) |

As quatro partes eram páginas separadas até 12/10 (estão no commit `9cf9516`); o guia as
traz inteiras. Em 13/10, o método ganhou as abas do desenho decisão por decisão e das
ferramentas, e a aba do CK ganhou o quadro do CK no V5; nada do que existia foi tirado. O
ranking dos modelos é **descritivo**: não está no pré-registro, só organiza a nota de
`analysis/notas-V5-STRATEGY.csv` e as tabelas de `analysis/resultados-V5-STRATEGY.md`.

**De onde vêm os números.** O guia usa só código **inventado** (nenhum pacote do V5). No
desenho, o gerador, o removedor de comentários, as regras de texto e o juiz rodam no
navegador a partir dos arquivos deste lote; as regras de árvore mostram o que o Semgrep
1.95.0 real achou, rodado pedaço por pedaço. Na qualidade, todo número de ferramenta é o
que o CK e o SonarQube reais devolveram, com as versões travadas do `metrics.sh`. Nos
scripts, a calculadora, os mutantes, os casos da suíte e as regras das hipóteses são
carregados do texto dos arquivos deste lote, e os sorteios, a conferência e a nota são
réplicas conferidas contra os scripts reais; os sorteios usam execuções inventadas, para
nenhuma semente reproduzir o mapa ou a amostra do V5.
