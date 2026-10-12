# Explicações interativas (`explanations/`)

Um guia para estudar o V5 por dentro: **`guia-completo.html`**, um HTML único que abre com
dois cliques, sem internet. Não faz parte do pré-registro nem da análise; foi feito em
12/10/2026, depois da leitura do Lucas, para estudo e para explicar ao orientador.

Ele tem uma barra lateral com as quatro partes e um **roteiro de leitura** na ordem da
pesquisa (a pergunta, a correção, rodar e medir, o acerto, o desenho, a qualidade, a
conferência, as hipóteses e a conclusão), que passa por todas as 40 abas.

| parte | o que explica |
|---|---|
| **1. O método** | a pergunta e o desenho, a sequência de verificação (o que cada passo protege), o que conta como acerto (o gabarito e a régua, com exercícios), as 32 hipóteses com o veredito do V5, as regras de leitura do §4.1 e o porquê de cada limite, as ameaças à validade e as defesas, e o que falta |
| **2. A bancada** | os 23 scripts que não são do Semgrep nem das métricas: a calculadora de referência e a suíte contra os 17 mutantes, o Docker e o caminho de uma execução (`run-levels`, `run-one`, `extract-meta`), a medição (`acceptance.sh`, `executor.sh`), os sorteios (`draw-order`, `anonymize`, `sample`), a conferência, a nota, as regras das hipóteses e o `verify` |
| **3. O desenho** | o Semgrep, linha por linha: `pontos.mjs`, `gerar-regras.mjs`, as regras do P4 e do P5, `detect.sh`, `copia-limpa.mjs` e `sem-comentarios.mjs`, e o juiz (`classificar.mjs`) rodando sobre 12 pacotes inventados, com as respostas comparadas às da régua |
| **4. A qualidade** | o SonarQube e o CK: as complexidades ciclomática e cognitiva, a duplicação, os code smells, as métricas de classe do CK, e os scripts `metrics.sh` e `aggregate-metrics.mjs` |

As quatro partes eram páginas separadas até 12/10 (estão no commit `9cf9516`); o guia as
traz inteiras, sem mudar um caractere.

**De onde vêm os números.** O guia usa só código **inventado** (nenhum pacote do V5). No
desenho, o gerador, o removedor de comentários, as regras de texto e o juiz rodam no
navegador a partir dos arquivos deste lote; as regras de árvore mostram o que o Semgrep
1.95.0 real achou, rodado pedaço por pedaço. Na qualidade, todo número de ferramenta é o
que o CK e o SonarQube reais devolveram, com as versões travadas do `metrics.sh`. Na
bancada, a calculadora, os mutantes, os casos da suíte e as regras das hipóteses são
carregados do texto dos arquivos deste lote, e os sorteios, a conferência e a nota são
réplicas conferidas contra os scripts reais; os sorteios usam execuções inventadas, para
nenhuma semente reproduzir o mapa ou a amostra do V5.
