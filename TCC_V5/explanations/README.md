# Explicações interativas (`explanations/`)

Páginas para estudar os instrumentos do V5 por dentro. Cada uma é um HTML único: abre
com dois cliques, sem internet. Não fazem parte do pré-registro nem da análise; foram
feitas em 12/10/2026, depois da leitura do Lucas, para estudo e para explicar ao
orientador.

| página | o que explica |
|---|---|
| `semgrep-por-dentro.html` | o Semgrep do experimento, linha por linha: `pontos.mjs`, `gerar-regras.mjs`, as regras do P4 e do P5, `detect.sh`, `copia-limpa.mjs` e `sem-comentarios.mjs`, e o juiz (`classificar.mjs`) rodando sobre 12 pacotes inventados, com as respostas comparadas às da régua |
| `metricas-por-dentro.html` | o SonarQube e o CK: as complexidades ciclomática e cognitiva, a duplicação, os code smells, as métricas de classe do CK, e os scripts `metrics.sh` e `aggregate-metrics.mjs` |
| `metodo-por-dentro.html` | o método: a pergunta e o desenho, a sequência de verificação (o que cada passo protege), o que conta como acerto (o gabarito e a régua, com exercícios), as 32 hipóteses com o veredito do V5, as regras de leitura do §4.1 e o porquê de cada limite, as ameaças à validade e as defesas, e o que falta |
| `scripts-por-dentro.html` | os outros 23 scripts: a calculadora de referência e a suíte contra os 17 mutantes, o Docker e o caminho de uma execução (`run-levels`, `run-one`, `extract-meta`), a medição (`acceptance.sh`, `executor.sh`), os sorteios (`draw-order`, `anonymize`, `sample`), a conferência, a nota, as regras das hipóteses e o `verify` |

**De onde vêm os números.** As páginas usam só código **inventado** (nenhum pacote do
V5). Na do Semgrep, o gerador, o removedor de comentários e as regras de texto rodam no
navegador a partir dos arquivos deste lote; as regras de árvore mostram o que o Semgrep
1.95.0 real achou, rodado pedaço por pedaço. Na das métricas, todo número de ferramenta é
o que o CK e o SonarQube reais devolveram, com as versões travadas do `metrics.sh`. Na dos
scripts, a calculadora, os mutantes, os casos da suíte e as regras das hipóteses são
carregados do texto dos arquivos deste lote, e os sorteios, a conferência e a nota são
réplicas conferidas contra os scripts reais; os sorteios usam execuções inventadas, para
nenhuma semente reproduzir o mapa ou a amostra do V5.
