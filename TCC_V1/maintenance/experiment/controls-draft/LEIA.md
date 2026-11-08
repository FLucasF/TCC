# Controles negativos da manutenção: rascunho para revisão

Os cinco enunciados de controle da `maintenance-v1`, copiados sem mudança de texto para revisão antes de rodar no
TCC_V1. Em cada um, o pedido toca a mesma parte do código que o cenário positivo correspondente, mas a regra não vai
crescer, e o *pattern* seria exagero. A última frase de cada um descarta a variação "por enquanto".

**Por que o texto ficou igual.** Entre a `maintenance-v1` e o TCC_V1, os enunciados positivos só ganharam uma frase
em três cenários (03, 07 e 09), e a moldura (`frame.md`) não mudou. Nenhum controle fala de onde os dados ficam, então a
troca da memória pelo PostgreSQL não muda o texto. O 06 (crianças na reserva) exige uma coluna nova no banco.

**O que ainda falta antes de rodar.**
- A revisão destes textos pelo Lucas.
- A orientação do verificador para controle: no controle, a resposta esperada é não aplicar o *pattern*, então a
  classe certa é a contenção, e não o acerto. A `maintenance-v1` tinha essa regra (`evaluation/answer-key/GABARITO.md`);
  o verificador do TCC_V1 só foi calibrado nos cenários positivos.
- Colocar os arquivos aprovados em `experiment/scenarios/` e conferir que os scripts das rodadas os aceitam.
