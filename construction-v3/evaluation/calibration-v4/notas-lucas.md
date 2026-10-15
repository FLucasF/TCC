# Calibração da régua v4: notas do Lucas

Dúvidas e achados durante a calibração (09/10/2026), com a régua versão 4 e o guia.
Cada item vira um ajuste no texto antes de congelar, ou nada (com o motivo).

## D95X

| tipo | o que aconteceu | proposta |
|---|---|---|
| procedimento | "abri as classes mas não entendi como revisar e pontuar": não ficou claro que o leitor só escolhe o valor (a nota sai por script) | o guia diz no começo do §3: "você não pontua; escolhe a palavra de cada pergunta, com a linha que a prova" |
| procedimento | o Lucas leu "os níveis são texto, não enum" como o motivo do `espalhado`; o critério da régua é outro (uma linha que faz algo só para um nível, fora da casa dele) | o guia traz o exemplo de um código com texto e `isolado` (um mapa de texto para classes) |
| conduzido | para achar a evidência, o Claude listou os tipos de linha a procurar (zerar o frete, crédito, brinde); são do gabarito, mas conduziram a busca | o roteiro de **três testes por linha** entra no guia (nomeia? é lista de válidos? está fora da casa?), para o leitor validar sozinho |
| texto da régua | "se nem existe classe, como vai existir um método só dele?": a régua não diz o que acontece quando o caso não tem unidade própria | uma frase no §2.1 da régua: "se o caso não tem unidade própria, qualquer código específico dele está fora da casa" |

## XMF3

Sem hesitação nas respostas, mas feito acompanhado, a pedido do Lucas ("vamos fazendo
comigo"): o roteiro dos três testes ainda não foi testado com ele lendo sozinho.

| tipo | o que aconteceu | proposta |
|---|---|---|
| procedimento | num pacote grande, a busca pelos nomes só acha a casa de cada nível; o uso de fora aparece na segunda busca, que traz muito ruído (`import`, campos da entrada e da saída, "INDISPONIVEL", o cupom `FRETEGRATIS`). O Claude apontou as categorias de ruído; o Lucas aplicou | o guia traz, no roteiro, "primeiro limpe o ruído com o §6", antes dos três testes |
| texto da régua | a evidência de um `isolado` não é óbvia (não há "a linha que prova"); o Claude propôs a casa do nível e as linhas de uso de fora sem nomear, e o Lucas aceitou | a régua diz o que vai na evidência de cada valor: no `isolado`, a casa e o uso de fora; no `espalhado`, a linha que passou nos três testes |

## Comparação com o Semgrep (depois das duas leituras)

8 de 8: as 4 perguntas nos 2 pacotes. O resultado do Semgrep foi calculado antes da
leitura e só aberto depois dela.

## Encerramento (09/10/2026)

Os 6 achados viraram texto, sem mudar nenhuma regra: na régua, a frase do caso sem
unidade própria (§2.1) e a evidência de cada valor (§5); no guia, "você não pontua"
(§3), o roteiro com a limpeza do ruído antes, a casa de cada nível e os três testes por
linha (§3 e §4), e o exemplo de texto com `isolado` (§4). A régua continua coerente com
o Semgrep (os dois tratam o nível sem casa como `espalhado`).

Calibração encerrada por decisão do Lucas: o Claude ajudou só a entender o que
responder, e ele treina sozinho com pacotes do V3 antes da leitura do V4.
