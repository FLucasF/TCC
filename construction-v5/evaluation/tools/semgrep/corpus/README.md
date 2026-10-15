# Corpora de validação do Semgrep (`evaluation/tools/semgrep/corpus/`)

A prova de que as regras do Semgrep respondem o que a régua manda. Cada corpus é um
**gerador** de pacotes pequenos, cada pacote com **uma** forma de escrever o padrão (um
`Map` de estratégias, um `enum` com corpo, um `if` que remenda o Ouro, nomes em inglês...),
e um `esperado.csv` com a resposta da régua para cada pergunta, **escrita antes** de
rodar as regras. O repositório guarda só os geradores; os pacotes nascem numa pasta
temporária e somem no fim.

## Rodar

Da raiz do TCC, com o Docker aberto:

```bash
evaluation/tools/semgrep/corpus/validar.sh
```

Sai com 1 se alguma resposta divergir do esperado. Os pacotes dos corpora não têm
`pom.xml`, então o `copia-limpa.mjs` avisa "sem pom.xml; vale src/main" em cada um: é
o esperado (a regra de reserva), não um erro.

## Os arquivos

| arquivo | o que faz | como faz |
|---|---|---|
| `validar.sh` | refaz a validação inteira | para cada `corpus-*.mjs`: roda o gerador numa pasta temporária, roda o `detect.sh` nos pacotes gerados e compara a saída com o `esperado.csv` pelo `comparar.mjs`; imprime "N de N respostas certas" por corpus e, se houver, cada divergência |
| `comparar.mjs` | compara a saída do Semgrep com o esperado de um corpus | casa as linhas pelo pacote e compara cada resposta; `*` no esperado é "não importa" (o ponto não aparece no pacote). Duas colunas especiais conferem os avisos: `avisos_contem` (o aviso tem de ter o texto) e `avisos_sem` (não pode ter) |
| `corpus-1-desenvolvimento.mjs` | 40 pacotes, as formas que guiaram a versão 2 das regras | é de **desenvolvimento**: as regras foram ajustadas olhando para ele (a versão 1 acertou 41 de 80 respostas; a 2, 80 de 80) |
| `corpus-2-controle.mjs` | 22 pacotes com formas que as regras nunca viram | é o de **controle**: escrito depois de fechar o corpus 1, com a resposta decidida antes de rodar, inclusive formas que se previa que falhassem (42 de 44 na primeira passada, as 2 falhas eram as previstas; 44 de 44 corrigido) |
| `corpus-3-revisao.mjs` | 5 pacotes da revisão do código das regras e da regressão nos pacotes reais | desenvolvimento: dois defeitos que a versão 2 tinha introduzido e que a regressão pegou viraram caso aqui (10 de 10) |
| `corpus-4-sinonimos.mjs` | 6 pacotes com nomes em inglês (`GOLD`, `CREDIT_CARD`, `NORTH`) e `compareTo` | desenvolvimento: respostas escritas antes de implementar os sinônimos, mas por quem implementou (12 de 12) |

**Por que separar desenvolvimento e controle.** Um corpus usado para ajustar as regras
mede o ajuste, não a régua: acertar 80 de 80 nele é o mínimo. Só o corpus 2 diz quanto as
regras acertam em forma nova. A conferência humana (o Lucas lendo a amostra) é a prova
seguinte, nos pacotes reais.

## O último resultado

10/10/2026, depois da correção da escolha do projeto (`copia-limpa.mjs`): **80 de 80,
44 de 44, 10 de 10 e 12 de 12**, os mesmos de antes da correção.
