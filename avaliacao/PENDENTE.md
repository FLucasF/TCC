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

## Falta criar

| item | o que é | prioridade |
|---|---|---|
| `testes-escondidos/` | o resto da suíte contra o contrato. A precedência dos erros já saiu; falta fronteiras (5,00 kg, R$ 300, R$ 1.000, 3× e 12×), empates de arredondamento e combinações de `LEVE3PAGUE2` | **alta** |
| `rubrica-strategy.md` | escala 0/1/2 por ponto, com exemplo-âncora de cada nível | **alta** |
| `testes-extensao/` | um caso novo por ponto — `DRONE`, `DEZOFF`, `CARTEIRA_DIGITAL`. Mede se estender toca código existente | média |
| `mapa-anonimizacao.csv` | id cego → run. Só abrir depois de fechar as notas | depois |
| `notas-autor.csv` | suas notas, às cegas | depois |
| `notas-professor.csv` | avaliação independente | depois |
| `consenso.csv` | divergências resolvidas | depois |

## Regras que as dez execuções de 19/09 já ensinaram

**Conferir campo a campo, nunca só o `totalFinal`.** Duas execuções acertam o
total e erram `frete` e `descontoCupom`. Uma suíte que compare só o total
aprova as duas.

**Separar erro de dinheiro de erro de apresentação.** Cobrar frete que deveria
ser gratuito e mostrar o resumo errado com o total certo são falhas de
gravidade muito diferente, e somar as duas apaga o sinal.

## Decisões de rubrica ainda abertas (P6 do plano)

**`enum` com método por constante conta como Strategy?** Deixou de ser
hipotética: nas execuções de 19/09 o braço sem harness usou `String` com
`switch`, e o com harness usou método por constante. Se a resposta for "não
conta", os dois caem em zero e a escala perde justamente a distinção que o
experimento produziu. Recomendação: **sim**, com anotação de que foi por
constante e não por classe.

- Regra genérica parametrizada conta, se a seleção não usar condicional?
- Condicional concentrada num único ponto conta como "sem Strategy"?
