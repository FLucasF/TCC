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

### A1. `avaliacao/rubrica-strategy.md`

A nota de design é o desfecho primário e não existe escala escrita.

Precisa de: escala por ponto (entrega, cupons, pagamento), com exemplo-âncora
de cada nível tirado das 24 execuções que já existem — elas dão material real
em vez de exemplo inventado.

Bloqueia A3, porque os testes de extensão medem justamente o nível 2.

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

### A3. `avaliacao/testes-extensao/`

Um caso novo por ponto — `DRONE`, `DEZOFF`, `CARTEIRA_DIGITAL`. Mede
diretamente "acrescentar um caso toca código existente?", que é o nível 2 da
rubrica. Hoje isso seria julgado lendo código, o que é lento e subjetivo.

### A4. Casos de referência para testar as ferramentas

**Nenhum teste testa o testador.** Em 19/09 apareceram cinco defeitos nas
ferramentas de medição, três achados por acaso:

| defeito | o que teria reportado |
|---|---|
| auditoria marcava `curl localhost` como web | acesso externo em quase toda run |
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
