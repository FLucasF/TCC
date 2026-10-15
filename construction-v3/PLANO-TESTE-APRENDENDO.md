# Plano da calibração da régua (item d)

> Combinado em 07/10/2026, numa sessão com o Claude. Serve para retomar a
> calibração exatamente de onde parou, sem depender da memória da conversa.
> O item d está no [`PLANO-IMPLEMENTACAO.md`](PLANO-IMPLEMENTACAO.md) (Parte 1) e
> no [`MINIPLANO.md`](MINIPLANO.md). Ficou para o fim, por decisão do Lucas: a
> única exigência é a régua estar congelada **antes** de alguém ler um pacote do V4.

## Para que serve

A régua ([`evaluation/regua.md`](evaluation/regua.md)) diz como ler um pacote de
código e registrar o desenho dele. Até hoje, só agentes do Claude a aplicaram (três
rodadas, até zero dúvidas e 100% de concordância). Isso mostra que ela é clara para
quem a segue à letra, não que uma pessoa leria igual.

Calibrar é o Lucas aplicar a régua em pacotes **fora da análise**. **Cada vez que
ele hesitar entre dois valores, a hesitação vira uma regra escrita na régua.**
Quando os pacotes passarem sem hesitação, a régua é congelada com hash no README.

## Os pacotes: 8, todos cegos e fora da análise

| pasta | pacotes | enunciado | pontos |
|---|---|---|---|
| `evaluation/calibration-strategy/` | `L3RG`, `L7MG` (do `TESTE-P4`) | o de 5 pontos, com imposto | P1 a P5 |
| `evaluation/calibration-pilot/` | `JHG3`, `WGW2`, `FX38`, `RDKS`, `Q84C`, `WM6L` | o de 3 pontos, do piloto | P1 a P3 |

*Por que estes:* o plano pedia os SMOKE e os TESTE-P4, mas os SMOKE estão misturados
com os BATCH na mesma pasta, e só o mapa de anonimização diria quais são quais
(abri-lo quebraria a cegueira). Ficaram os dois do TESTE-P4, que têm os 5 pontos
como o V4, e os seis que mais travaram nas rodadas dos agentes: se a régua aguenta
nesses, aguenta no resto. São 28 leituras de ponto (2 × 5 + 6 × 3).

**Regra de parada (08/10):** não é preciso ler os 8. O mínimo é o `L3RG` e o
`L7MG` inteiros; depois, pacotes do piloto **até dois seguidos passarem sem
hesitação**, com pelo menos um código que não seja `classes`. Deve dar entre 12 e
16 leituras de ponto. A calibração só serve para a régua ganhar regras: não entra
em estatística nenhuma (o kappa é da Parte 4, sobre os pacotes do V4). O motivo
está no [`DECISOES.md`](DECISOES.md), §6.

Cada pasta tem o **gabarito** daquele enunciado (`gabarito.md`), que é o que se usa
na leitura dos pacotes dela.

## O passo a passo, em cada ponto de cada pacote

1. **O Claude mostra o gabarito do ponto:** os casos, o que varia, a regra comum, o
   caso exigente e o caso hipotético.
2. **O Lucas acha os casos no código** com o comando de busca (abaixo) e abre os
   arquivos no editor.
3. **O Lucas decide cada propriedade, com o `arquivo:linha`**, uma de cada vez, na
   ordem da régua (§2): `forma`, `localizacao`, `selecao`, `assinatura`,
   `parte_comum`, `custo_caso_novo`, e `proporcao` só no controle negativo. O Claude
   explica a régua quando pedido, mas **não decide nem diz se acertou**.
4. **Toda hesitação é anotada:** em que propriedade, entre quais valores, e por quê.
   Ela vira regra nova ou esclarecimento na régua.
5. **O Claude registra as respostas** na planilha do Lucas, no formato da régua (§4):
   `leitura-lucas.csv` (uma linha por pacote × ponto) e `leitura-lucas-pacote.csv`
   (uma linha por pacote: `especulativa`, `pista_condicao`), na pasta do pacote.
6. **Ao terminar cada pacote**, comparam-se as respostas com as leituras dos agentes
   (`parts/`, `parts-r2/`, `parts-r3/` da mesma pasta), e cada diferença é discutida:
   ou a régua não está clara (vira regra), ou um dos dois leu errado.

**Regra de independência:** até terminar cada pacote, ninguém abre as planilhas dos
agentes (`parts*`) nem o mapa de anonimização.

## Os comandos de busca

Da raiz do `TCC_V3`, trocando `<pasta>` e `<pacote>`:

```bash
# P1 entrega
grep -rniE "economica|expressa|retirada|motoboy" evaluation/<pasta>/packages/<pacote> --include=*.java | grep src/main
# P2 cupom
grep -rniE "bemvindo|menos50|fretegratis|leve3" evaluation/<pasta>/packages/<pacote> --include=*.java | grep src/main
# P3 pagamento
grep -rniE "pix|cartao|boleto" evaluation/<pasta>/packages/<pacote> --include=*.java | grep src/main
# P4 clube (só calibration-strategy)
grep -rniE "bronze|prata|ouro" evaluation/<pasta>/packages/<pacote> --include=*.java | grep src/main
# P5 imposto (só calibration-strategy)
grep -rniE "sudeste|sul\b|centro_oeste|norte|nordeste" evaluation/<pasta>/packages/<pacote> --include=*.java | grep src/main
```

## Onde parou

| pacote | ponto | situação |
|---|---|---|
| `L3RG` | P1 | **feito** em 08/10: na planilha `evaluation/calibration-strategy/leitura-lucas.csv`; as notas (nenhuma hesitação, duas observações sobre o texto) em `notas-lucas.md`, na mesma pasta |
| `L3RG` | P2 (cupom) | **feito** em 08/10 |
| `L3RG` | P3 (pagamento) | **feito** em 08/10, com três hesitações (`selecao`, `assinatura`, `custo_caso_novo`) |
| `L3RG` | P4 (clube) | **feito** em 08/10, com o `custo_caso_novo` `indeterminado`; depois dele, o [`GUIA-DA-REGUA.md`](evaluation/GUIA-DA-REGUA.md) |
| `L3RG` | P5 (controle negativo) | **o próximo** |
| `L7MG` | P1 a P5 | a fazer |
| piloto | até dois pacotes seguidos sem hesitação | a fazer |

## Quando termina

- [ ] Os pacotes lidos até a regra de parada, com as planilhas do Lucas commitadas **antes** da comparação
      com os agentes (a data do commit prova a independência).
- [ ] Cada hesitação e cada divergência resolvida: regra nova na régua (§5) ou
      esclarecimento no texto, com entrada no [`DECISOES.md`](DECISOES.md).
- [ ] Uma última passada sem hesitação (como a rodada 3 dos agentes).
- [ ] A régua congelada: o aviso de RASCUNHO trocado, commit e hash no README.
- [ ] O gabarito do Strategy apontado para o enunciado do V4, com o P5 falando do
      seguro (item do plano, Parte 6).
