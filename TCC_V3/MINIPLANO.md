# Miniplano: o que falta antes do V4

> Escrito em 06/10/2026, para retomar no dia seguinte. A base técnica está pronta
> (bancada, níveis N0 a N3, enunciados do V4, suíte, métricas). Falta conferir,
> calibrar e aprovar. O detalhe de cada item está na fase 1 do
> [`PLANO-IMPLEMENTACAO.md`](PLANO-IMPLEMENTACAO.md); as letras são as de lá.

## 1. A sua parte, nesta ordem

**c. Revisar as leituras do gabarito do Strategy (~15 min).** Trocou em 07/10: a
conta à mão saiu (as implementações independentes já verificam a aritmética), e
fica o que só você pode decidir. Roteiro em
[`evaluation/acceptance-prototype/README.md`](evaluation/acceptance-prototype/README.md),
seção "4. A revisão das leituras".

- [x] Para cada um dos **5 pontos** da tabela, ler o trecho no enunciado e marcar se
      concorda com a leitura da calculadora.
- [x] Preencher o "Resultado" (data, quem revisou, divergências). **Feito em 07/10,
      sem divergência.**

Em 07/10 o imposto virou **seguro por região** (mesma forma, o P5 continua sendo o
controle negativo); o ponto do FRETEGRATIS com o imposto saiu da lista.

**c. Conferir o gabarito do State (~15 min).**
- [ ] Cada valor esperado no `evaluation/acceptance-prototype/state.mjs` é o do
      `experiment/prompt/state.md`: os 8 exemplos e os erros.

**d. Calibrar a régua do Strategy.** Usa a [`evaluation/regua.md`](evaluation/regua.md) e o
[`evaluation/strategy/gabarito.md`](evaluation/strategy/gabarito.md), sobre os pacotes
SMOKE e TESTE-P4 (Parte 1 do plano).
- [ ] Pronto quando não houver caso em que você hesite entre dois valores; cada
      hesitação vira uma regra escrita. Depois, congelar (commit e hash).

**f5. Revisar os limites das regras de leitura.** No §4.1 do [`OBJETIVO.md`](OBJETIVO.md):
- [ ] aceitar ou ajustar: "no máximo 2 de 15 pares piores", "12 de 15 na mesma
      direção" e "pelo menos 4 pares não empatados".

## 2. Para a reunião com o professor

- [ ] **b. Confirmar as quatro trocas:**
  1. conferência humana do gabarito no lugar da implementação de referência escrita
     do zero (03/10);
  2. a escada N0 a N3: N3 e N4 trocaram de lugar, e a verificação automática foi
     descartada porque os agentes já rodam o build sozinhos (06/10);
  3. o enunciado do V4: o cliente "entende o básico", e as três inconsistências do
     Strategy foram corrigidas (06/10);
  4. o desenho da manutenção (Parte 5), que fica para depois do V4.
- [ ] **a. Congelar o OBJETIVO**, depois do f5.
- [ ] Decidir se entra um leitor de outro fabricante, como o Jev, ao lado do Claude.
      Só vale se for decidido **antes** de congelar. Recomendação: não agora, e sim
      registrar como trabalho futuro.

## 3. O que o Claude faz quando você pedir

- [ ] **f.** O `aceitacao.sh` e congelar a suíte (depois do c).
- [ ] **e.** Rascunho do ajuste da régua do State (depois do d); a calibração é sua.
- [ ] Rever se o enunciado do State tem fronteiras ("até", "acima de") sem caso no
      valor exato, antes do lote STATE.
- [ ] Plano de cota das 120 execuções (depois do f4).
- [ ] Atualizar o item **b** do plano com as quatro trocas. Pendente da sua
      resposta: anotar no §6 que "o Claude lê código do Claude" (a proteção é a sua
      leitura e o kappa) e, no README dos harnesses, a ideia de um sensor de desenho
      com o Jev.

## 4. Gasta cota

- [ ] **f4.** Teste real do `rodada-niveis.sh`, com a assinatura livre, já com o
      enunciado do V4. Um quarteto de Haiku mede quanto da janela de 5 h uma rodada
      consome; **um quarteto de Sonnet (ou Opus)** produz as implementações
      independentes que verificam a aritmética da calculadora nova (a suíte roda sobre
      elas). Depois, o hash no README. Comandos:
      `infra/scripts/rodada-niveis.sh TESTE-NIVEIS-01 1 HAIKU` e
      `infra/scripts/rodada-niveis.sh TESTE-NIVEIS-01 1 SONNET`.

## 5. Pode esperar (não impede começar a rodar o V4)

- o `ler-cego.sh`: só é preciso antes da **leitura** do V4;
- o `verificar.mjs` (fase 2);
- a manutenção (Parte 5);
- o piloto do Jev (opcional).

## A ordem

```
c ──→ f (suíte congelada)
d ──→ e (régua do State)
f5 + b ──→ a (OBJETIVO congelado)
f4 ──→ plano de cota
tudo acima ──→ montar o V4 (cabeçalho do gabarito do Strategy no enunciado novo) ──→ rodar
```
