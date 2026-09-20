# O que falta para o lote

Levantado em 19/09/2026, depois de 24 execuções de medição.

**Situação em uma frase:** a parte que produz dado está pronta e provada; a
parte que lê o dado tem defeito conhecido e o desfecho primário não tem
instrumento nenhum.

---

## A. Instrumento de avaliação — bloqueia o desfecho primário

Sem isto você roda o lote e não consegue pontuá-lo. É a lição do piloto de
setembro se repetindo: *"tudo que avalia precisa existir e ser testado antes de
rodar"*.

### A1. `avaliacao/rubrica-strategy.md` — **feito em 20/09/2026**

Critérios instanciados por ponto, catálogo das seis formas observadas, e
âncoras de código real em C1, C2, C3 e C5, todas conferidas contra o arquivo da
run. C4 e C6 ficaram em prosa, por escolha.

Duas lacunas registradas no próprio arquivo:

- **C2 = 1 não tem âncora.** Nenhuma das 24 execuções isolou parte das variantes
  de um mesmo ponto deixando as outras soltas.
- **As três extensões de hoje não separam "parametrizado" de "uma classe por
  variante".** `DEZOFF` é da mesma família de regra do `MENOS50`, então num
  desenho parametrizado ele entra como uma linha de dado e pontua C5 = 2 sem
  classe nova. Para distinguir, seria preciso uma segunda extensão por ponto que
  exigisse comportamento de família nova. Decidir antes do lote.

### A2. `avaliacao/casos/` — os testes escondidos

Existe: os quatro exemplos do enunciado e a rota do `FRETEGRATIS`.

Falta, por ordem de risco (ver `avaliacao/rotas-descobertas.md`):

1. **Precedência dos oito erros.** Cobertura zero hoje. É onde mais
   implementações devem divergir e é o mais barato de escrever.
2. **Fronteiras:** 5,00 kg no motoboy, R$ 300,00 no MENOS50, R$ 1.000,00 no
   boleto, 3× e 12× no cartão.
3. **Empates de arredondamento** meio-para-o-par, que nenhum exemplo exercita.
4. Combinações de `LEVE3PAGUE2`, campos opcionais, prazo por modalidade.

Formato já resolvido: JSON com `requisicao` e `esperado`, rodando pela
ferramenta que existe. Acrescentar caso é editar JSON.

### A3. `avaliacao/testes-extensao/` — **feito em 20/09/2026**

Onze casos nos três arquivos, com os valores conferidos pela mesma rotina
validada contra os exemplos do enunciado. Mais o procedimento em
`testes-extensao/README.md`: congelar o original com `git init`, implementar a
menor alteração que funcione, rodar os casos, e contar com `git status` e
`git diff --numstat`. A contagem virou mecânica.

A limitação das três extensões serem todas de família já existente ficou
declarada lá, e a decisão de aceitar está registrada.

### A4. Casos de referência para testar as ferramentas

**Nenhum teste testa o testador.** Em 19/09 apareceram cinco defeitos nas
ferramentas de medição, três achados por acaso:

| defeito | o que teria reportado |
|---|---|
| auditoria marcava `curl localhost` como web | acesso externo em quase toda run |
| auditoria marcava URL em heredoc de `pom.xml` (achado em 20/09) | acesso externo nas duas runs de Opus, e só nelas |
| `comparar.mjs` não sabia expressar caso de erro (achado em 20/09) | precedência dos oito códigos impossível de testar |
| build procurava o pom só na raiz | app que funciona marcada como quebrada |
| avaliador montava o arquivo em modo escrita | corrompeu a FUMACA-01 |
| contador somava o código 66 como 66 casos | 70 erros onde havia 4 |
| `pacote_raiz` nulo em projeto na raiz | coluna vazia na tabela |

Precisa de apps de referência com defeito conhecido, para provar que a
ferramenta acusa o que deve acusar. As 24 execuções servem de base: dá para
pegar uma correta e introduzir um erro de propósito.

### A5. Anonimização

Se a pontuação for às cegas, falta o script que tira `CLAUDE.md`, `.claude/` e
identificadores de run, mais o mapa id cego → run, aberto só depois de fechar
as notas.

---

## B. Decisões a fechar antes de rodar

Todas precisam estar no pré-registro. Decididas **depois** de ver o resultado,
viram escolha conveniente.

### B1. P6 — regras de aceitação da rubrica

Três perguntas abertas no plano. Uma delas os dados já forçaram:

> `enum` com método por constante conta como Strategy?

Nas execuções de 19/09 o braço sem harness usou `String` com `switch` e o com
harness usou método por constante. Se a resposta for "não conta", os dois caem
em zero e a escala perde a única distinção que o experimento produziu.

Recomendação: **sim**, anotando que foi por constante e não por classe.

### B2. Esqueleto: fica ou sai

Medido nos dois modos. O que os dados dizem:

- **Não** protege correção funcional — sem esqueleto o resultado foi igual ou melhor.
- **Protege a comparação entre modelos:** Opus escolhe Spring Boot 4.1.1 e Java
  21, Sonnet 3.3.4 e 21, Haiku 3.1.x e 17. Cada modelo é consistente consigo
  mesmo e os três discordam. Java 17 contra 21 muda o vocabulário disponível
  para expressar design.
- Para a comparação **com × sem harness dentro do mesmo modelo**, é dispensável:
  cada modelo escolheu a mesma base nos dois braços.

Caminho possível: sem esqueleto no geral, com esqueleto só se a comparação
entre modelos for desfecho declarado.

### B3. `effort`: high ou medium

O D8 diz `high`. **Vinte e duas das 24 execuções foram `medium`.** Precisa
decidir e fixar; os números das duas famílias não se comparam.

### B4. Dependência acrescentada: covariável ou exclusão

O Maven agora é online e o agente pode acrescentar biblioteca. O `meta.json`
registra em `dependencias.acrescentadas`. Falta decidir o que fazer quando
acontecer — anotação que gera código desloca arquivos, linhas e métodos sem que
o design tenha mudado.

### B5. `n` por célula

O plano fixa 3. A dispersão observada no braço sem harness foi de 30 a 55
turnos sem nenhuma mudança de tratamento. Custo medido: uma rodada de seis
consome ~8% da janela de cinco horas, então cabem ~6 rodadas por janela — `n`
maior é viável.

### B6. P4 — o significado do "1" na tabela do professor

Só ele responde. Muda o número de repetições por célula.

---

## C. Infraestrutura

### C1. `git init`

Não é repositório. Os cinco defeitos de hoje foram corrigidos editando arquivo
por cima, sem rede. E o `.gitignore` que protege o `.env` não protege nada
enquanto não houver git.

### C2. Agregador de `meta.json` → CSV

Não existe. Toda tabela desta sessão saiu de `node -e` improvisado. Para 24
execuções dá; para o lote, não.

### C3. Análise estatística

O `analise.py` do piloto foi apagado na limpeza — tinha Mann-Whitney exato
conferido contra o scipy. Precisa voltar, adaptado ao `meta.json`.

### C4. Procedimento do campo `valida`

Está `null` nas 24. Ninguém definiu quem marca, quando e com que critério.

---

## D. Documentos desatualizados

### D1. Seção 6.2 do plano

Descreve ordem sorteada com `schedule.csv` e a restrição de não repetir
condição mais de duas vezes seguidas. Na prática passamos a rodar `SEM` e `COM`
**simultâneos**, o que elimina a ordem em vez de sorteá-la. É melhor — mas a
seção não foi reescrita.

### D2. P2, P3 e P5 continuam marcadas como pendentes

Já foram resolvidas de fato: versões fixadas na imagem, limites removidos,
harness escrito. Falta mudar o estado no plano.

### D3. Checklists

Onze itens marcados de 143. A maioria dos não marcados já foi feita.

---

## Feito em 20/09/2026

| item | o que saiu |
|---|---|
| **C1** `git init` | feito. `runs/` passou a ser versionada menos `target/`: 483 MB de build fora, 11 MB de dado dentro |
| **A2**, prioridade 1 | `casos/precedencia-erros.json`, sete casos. Para isso o `comparar.mjs` precisou aprender a expressar erro — eram dois defeitos somados, e o sexto e sétimo da família do A4 |
| **A4**, parcial | `ferramentas/autoteste.mjs`, cinco variantes de app com defeito conhecido. Cobre o comparador; **não** cobre o pipeline inteiro em Docker |
| **B2** esqueleto | resolvido: removido. Versões passaram a ser pedidas no enunciado |
| **D1, D2, D3** | `plano.md` alinhado à realidade em dezenove seções |

Ainda não feito, e agora com dado novo:

- **A1**, a rubrica, continua sendo o bloqueador do desfecho primário.
- ~~**B1** (P6)~~ **resolvido em 20/09/2026**, em 14.4a do plano: conta como Strategy, com 2 em C1/C2/C3 e **1 em C5**, porque variante nova exige editar o próprio `enum`. Tabela de dados com caso especial por identidade fica em C1=1 e C3=1. `switch` com lógica dentro é C1=0. **A1 está destravado.**
- ~~**B3** (`effort`)~~ **resolvido em 20/09/2026**: o D8 passou para `medium`, por custo e não por desfecho, ver 3.1 do plano. Scripts, README e CLAUDE.md atualizados.
- **B5** (`n` por célula) continua aberta.
- **B4** vira outra coisa sem esqueleto: não existe pom de partida, então
  `dependencias.acrescentadas` passa a ser a lista inteira do que o agente
  declarou. O que decidir agora é o que fazer com quem **desobedece as versões
  pedidas no enunciado**, não com quem acrescenta biblioteca.
- O `meta.json` não grava `repeticao`. Continua aberto, e precisa existir antes
  do lote. O paralelismo foi **resolvido**: fica, e o 13.2 foi reescrito.
- ~~H2~~ **resolvida em 20/09/2026**: não-direcional, com o mecanismo plausível
  declarado à parte em vez de assumido.

---

## Ordem sugerida

1. `git init` — antes de qualquer edição, porque tudo abaixo mexe em arquivo.
2. **A1**, a rubrica. Define o que A3 precisa medir.
3. **A2**, começando pela precedência dos erros.
4. **A3**, testes de extensão.
5. **A4**, casos de referência das ferramentas.
6. **C2** e **C3**, agregador e análise.
7. **B**, todas as decisões, com os dados na mão.
8. Pré-registro fechado, e só então o lote.

**A5** e **D** entram a qualquer momento.
