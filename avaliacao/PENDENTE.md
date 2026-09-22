# avaliacao — o que falta

Nada nesta pasta chega ao agente. O `.dockerignore` é lista branca, e a
conferência de 19/09/2026 mostrou `avaliacao/`, `docs/`, `runs/`, `.env` e
`experimento/harness/` todos barrados do contexto de build.

## Já existe

| | |
|---|---|
| `gabarito-avaliador.md` | onde estão os três pontos avaliados e o que se espera de cada um |
| `rotas-descobertas.md` | o que os quatro exemplos do enunciado **não** cobrem, por ordem de risco |
| `ferramentas/conferir-exemplos.sh` | roda os quatro exemplos contra a app de uma run |
| `casos/rotas-sem-exemplo.json` | a rota do `FRETEGRATIS`, que nenhum exemplo cobre. Passe em `CASOS=` |
| `casos/precedencia-erros.json` | sete casos de precedência dos oito códigos de erro, escritos em 20/09/2026 |
| `ferramentas/autoteste.mjs` | testa o testador: sobe app de mentira com defeito conhecido e confere se o `comparar.mjs` acusa. `node avaliacao/ferramentas/autoteste.mjs` |
| ~~`rubrica-strategy.md`~~ | escrita em 20/09, **removida em 21/09/2026**. Recuperável em `git show aa71c81:avaliacao/rubrica-strategy.md` |
| `testes-extensao/` | `DRONE`, `DEZOFF` e `CARTEIRA_DIGITAL`, 11 casos, mais o procedimento de contagem. Escrito em 20/09/2026 |
| `casos/` | 60 casos nos quatro grupos da §14.3, gerados por `ferramentas/gerar-casos.mjs` com BigInt. Ver `casos/README.md` |
| `ferramentas/anonimizar.mjs` | prepara os pacotes para a avaliação às cegas, gera o mapa e as planilhas, e conta as pistas que o modelo deixou |
| `README.md` | o fluxo da §14 em ordem, e o que cada ferramenta faz |

## Falta criar

| item | o que é | prioridade |
|---|---|---|
| planilhas de notas | geradas pelo `anonimizar.mjs` na hora de avaliar o lote | depois |

## Regras que as dez execuções de 19/09 já ensinaram

**Conferir campo a campo, nunca só o `totalFinal`.** Duas execuções acertam o
total e erram `frete` e `descontoCupom`. Uma suíte que compare só o total
aprova as duas.

**Separar erro de dinheiro de erro de apresentação.** Cobrar frete que deveria
ser gratuito e mostrar o resumo errado com o total certo são falhas de
gravidade muito diferente, e somar as duas apaga o sinal.

## Decisões de rubrica ainda abertas (P6 do plano)

**Resolvidas em 20/09/2026**, na §14.4a do plano e na `rubrica-strategy.md`.

A varredura das 24 execuções mostrou que a pergunta era mais larga do que
parecia: não são duas formas, são **seis**. A nota antiga aqui dizia que o
braço sem harness usou `String` com `switch` — no `MED-05-HAIKU-SEM` ele usou
um `Map<String, EntregaConfig>` de dados com casos especiais por identidade, e o
`switch` puro só aparece no `MED-07`.

| forma | como pontua |
|---|---|
| classes por variante | C1/C2/C3 = 2, **C5 = 2** |
| `enum` com corpo por constante | C1/C2/C3 = 2, **C5 = 1** |
| regra parametrizada / mapa de dados | C1 = 1; C3 ≤ 1 se sobrar caso especial por identidade |
| `enum` só com as constantes | C1 = 1: nomeia a variante, não abstrai o comportamento |
| `switch` com a lógica dentro | C1 = 0 |
| condicional concentrada em P3 | "sem Strategy", anotando concentrada ou espalhada |
