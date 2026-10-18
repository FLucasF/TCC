# Gabarito dos 10 cenários (rascunho)

> Só para a avaliação: o agente nunca vê este arquivo. Cada execução recebe uma cópia limpa da base
> (`experiment/base/hotel/`) e o texto de **um** cenário dentro da moldura (`experiment/scenarios/frame.md`).



**Os controles negativos saíram da análise (decisão do Lucas, 18/10/2026).** A pesquisa passa a olhar só os cenários que pedem o pattern (01, 03, 05, 07 e 09), como a `construction-v7`. Os pedidos pares, as execuções deles (02, 04, 06, 08 e 10, dos três modelos) e as avaliações ficam guardados como registro, sem apagar, mas não entram nas tabelas nem nas conclusões.

## A regra que separa os dois tipos de cenário

| | o cenário que pede o pattern | o controle negativo |
|---|---|---|
| o que o texto traz | **variação concreta agora** (três ou mais casos, ou dois lugares, ou duas situações) **e** uma frequência de mudança dita ("vive inventando", "vai ter mais") | **uma mudança pontual** (um número, um texto, um campo, uma coluna) e uma frase de futuro vaga ("talvez um dia… mas por enquanto") |
| a resposta certa | o pattern, cobrindo também os casos que já existem na base | a mudança pequena, no lugar onde a coisa já mora |
| a armadilha | o `if` a mais (ou a cópia a mais) | levar a sério o "talvez um dia" e construir a estrutura para ele (YAGNI) |

A frase vaga dos negativos é proposital: é ela que tenta o exagero. Nenhum texto nomeia pattern nem
usa palavras como "flexível" ou "extensível".

## A classificação de cada execução

| | aplicou o pattern certo | aplicou mal | não aplicou |
|---|---|---|---|
| **cenário que pede** | **acerto** | **malfeito** | **omissão** |
| **controle negativo** | **exagero** | **exagero** | **contenção** (acerto) |

Mais duas classes, para o que não cabe no quadro:
- **alternativa equivalente:** resolveu a variação com outra estrutura que este gabarito aceita de
  antemão (listada em cada cenário). Não é omissão nem acerto do pattern-alvo; fica registrada à parte.
- **exagero colateral:** em qualquer cenário, criou um pattern (ou refatorou para um) numa parte da base
  que o pedido não tocava. É contado à parte da classe principal.

E duas condições que valem para qualquer classe:
- **o comportamento está certo:** o que a base já fazia continua igual, e o que o pedido pediu funciona.
  Um pattern correto com o comportamento errado conta como **falha de correção**, registrada junto.
- **o `mvn verify` do agente passa.**

### "Aplicou mal" (malfeito), em geral
- a estrutura existe, mas convive com o código velho: o `switch` antigo continua decidindo, ou só o caso
  novo foi para a estrutura nova (meio pattern);
- a estrutura existe, mas quem a usa ainda nomeia os casos concretos (o serviço pergunta qual é o tipo);
- um dos lugares que deveriam passar pela estrutura continua fazendo à mão.

---

## 01 · Planos de tarifa novos · **Strategy**, pede

- **Sinais no texto:** três planos novos agora, além dos dois de hoje; "o comercial vive inventando
  plano"; cada plano com o seu preço, a sua regra de uso e a sua devolução.
- **Na base:** o `switch (ratePlan)` em dois lugares (`PriceCalculator.calculate` e
  `ReservationService.cancel`).
- **Acerto:** cada plano (os 5) mora num lugar seu, com o preço, a validação e a devolução; o cálculo e o
  cancelamento pedem ao plano, sem nomear os planos. A escolha do plano a partir do `RatePlan` (um mapa,
  um campo do `enum`, ou um único `switch` de seleção) é aceita: o que não pode é haver `switch` de
  **comportamento** espalhado.
- **Alternativa equivalente:** o `enum RatePlan` com métodos abstratos por constante (o `enum` como
  estratégia) conta como acerto, porque é o Strategy na forma idiomática do Java.
- **Omissão:** os `switch` crescem para 5 casos nos dois lugares (e a validação ganha mais um bloco de `if`).
- **Malfeito:** interface criada só para os três planos novos, com `STANDARD` e `NON_REFUNDABLE` ainda no
  `switch`; ou a validação (sexta a domingo, 7 noites, código da empresa) fora do plano, em `if` no serviço.
- **O comportamento novo:** os três exemplos do texto; código de empresa ausente ou desconhecido → `400`;
  pacote fora de sexta→domingo e com uma ou três noites → `400`; estadia longa com 6 noites → `400` e com 7
  aceita; pacote cancelado depois de pago devolve a metade (meio para o par); convênio e estadia longa
  cancelados depois de pagos devolvem tudo;
  `companyCode` ignorado nos outros planos; os dois planos antigos inalterados.
- **Atenção:** a ordem entre as recusas não foi dita; não cobrar uma ordem.

## 02 · A alta temporada mudou · Strategy, **negativo**

- **A frase que tenta:** "já pensamos em ter um acréscimo diferente para cada temporada".
- **Contenção:** a constante vira 0,25 e as datas da Páscoa entram como dado (uma lista de períodos, ou duas
  condições a mais no `isHighSeason`).
- **Exagero:** uma classe ou estratégia por temporada; uma interface de "regra de temporada"; um acréscimo
  por temporada parametrizado sem que ninguém peça.
- **O comportamento novo:** standard em alta temporada R$ 312,38 (299,88 × 1,25 = 312,375 → 312,38, meio
  para o par); deluxe 487,38; suíte 724,88; 20/03/2027 baixa, 21/03 e 28/03 alta, 29/03 baixa; idem
  08/04/2028, 09/04, 16/04, 17/04; o resto das datas inalterado; o desconto do não reembolsável sobre o novo
  valor.

## 03 · Avisar a equipe · **Observer**, pede

- **Sinais no texto:** três interessados na confirmação e dois no cancelamento, agora; "vai ter mais gente
  querendo ser avisada, e de mais coisas" (restaurante, transfer).
- **Na base:** o `confirm` chama o `EmailSender` direto.
- **Acerto:** o serviço de reservas publica que a reserva foi confirmada (ou cancelada) e não conhece quem
  é avisado; cada interessado (hóspede, governança, financeiro) é um ouvinte registrado na montagem
  (`Hotel`).
- **Alternativa equivalente:** nenhuma; um "notificador" único que conhece todos os destinos é omissão.
- **Omissão:** o `confirm` e o `cancel` mandam os e-mails em sequência, um depois do outro.
- **Malfeito:** existe a lista de ouvintes, mas o serviço ainda monta os e-mails da governança e do
  financeiro; ou só a confirmação passou a ter ouvintes, e o cancelamento manda à mão.
- **O comportamento novo:** os destinatários, os assuntos exatos e o conteúdo (número do quarto, datas
  `dd/MM/yyyy`, valor `R$ 1.234,56`) na confirmação; no cancelamento de reserva confirmada, a governança
  sempre e o financeiro só com reembolso maior que zero (`NON_REFUNDABLE` → sem e-mail ao financeiro);
  cancelamento de reserva pendente → nenhuma mensagem; a ordem não é conferida.

## 04 · O e-mail de confirmação · Observer, **negativo**

- **A frase que tenta:** "talvez um dia a gente mande por WhatsApp também".
- **Contenção:** muda o assunto e o texto em `confirmationEmailBody` (ou onde o texto estiver).
- **Exagero:** criar um canal de mensagem abstrato, um ouvinte de confirmação, ou um modelo de mensagem
  com várias implementações.
- **O comportamento novo:** o assunto novo exato; as três linhas novas no lugar da antiga; saudação,
  comprovante e "Até breve!" iguais.

## 05 · Comprovante para empresa e estrangeiro · **Factory** (Factory Method), pede

- **Sinais no texto:** três tipos de hóspede agora, cada um com o seu documento; o documento sai em três
  lugares; "agência de turismo… vai precisar de um documento próprio".
- **Na base:** `new ReservationReceipt(...)` em `confirm`, `checkOut` e `receiptCopy`.
- **Acerto:** a escolha de qual documento criar mora num lugar só, e os três lugares pedem o documento a
  esse lugar. Forma GoF (um criador com o método de fábrica sobrescrito) ou um método de fábrica
  concentrado (uma fábrica, ou um método polimórfico no tipo de hóspede, como `guest.issueDocument(...)`):
  as duas contam como acerto, e a forma fica registrada.
- **Omissão:** o `if`/`switch` sobre o tipo de hóspede repetido nos três lugares.
- **Malfeito:** a fábrica existe, mas um dos três lugares ainda cria o documento à mão; ou a fábrica
  devolve sempre o comprovante, e a escolha acontece depois, fora dela.
- **O comportamento novo:** os três jeitos de identificar; nenhum, dois ou incompletos (CNPJ sem razão
  social, passaporte sem país) → `400`; o título e o `text` de cada documento nos três lugares (o e-mail
  de confirmação, o check-out e a segunda via); o ISS de 5% meio para o par (779,80 → 38,99); `BRL 1,234.56`
  e datas ISO no *booking receipt*; `1 night` × `2 nights`; o hóspede com CPF exatamente como hoje.

## 06 · Crianças na reserva · Factory, **negativo**

- **A frase que tenta:** "já pensamos em cobrar alguma coisa das crianças maiores no futuro".
- **Contenção:** um campo a mais na reserva (e no pedido), a soma na conferência da capacidade e a linha
  no comprovante; a reserva continua criada direto.
- **Exagero:** um *builder* ou uma fábrica para a reserva ou para o pedido de reserva; uma "política de
  preço de criança" sem preço nenhum.
- **O comportamento novo:** sem `children` → 0; negativo → `400`; adultos + crianças acima da capacidade →
  `422`; o preço não muda; "Crianças: 2" logo depois da linha do quarto; sem crianças, comprovante igual.

## 07 · Regras para as mensagens · **Decorator**, pede

- **Sinais no texto:** três comportamentos novos agora sobre o mesmo envio, que se combinam de jeitos
  diferentes conforme o modo (o arquivo só fora do teste; o bloqueio sempre); "não mandar de madrugada" e
  "limite por dia" já anunciados, "cada regra pode valer sempre, só no teste ou só fora dele".
- **Na base:** o `EmailSender`, sem nada além de guardar a mensagem.
- **Acerto:** cada regra é um embrulho do envio, com a mesma cara do envio (a mesma interface), e a
  montagem (`Hotel`) empilha os embrulhos conforme o modo.
- **Alternativa equivalente:** uma corrente de regras (Chain of Responsibility) ou uma lista de filtros
  aplicada pelo envio, desde que cada regra seja uma peça própria e a combinação seja decidida na montagem.
- **Omissão:** os três `if` (o endereço, o modo de teste, a cópia) dentro do `send`.
- **Malfeito:** os embrulhos existem, mas o envio de dentro ainda pergunta pelo modo de teste; ou a
  combinação por modo está escrita dentro de um dos embrulhos.
- **O comportamento novo:** com e sem `HOTEL_TEST_MODE=true`. Fora do teste: endereço válido → original `SENT` + cópia `[Cópia] ` para o arquivo, logo depois; inválido
  (`ana@gmail`, `ana.gmail.com`, `@x.com`, `a@@b.com`) → só a `BLOCKED`, sem cópia. No teste: tudo para
  `testes@…`, assunto `[TESTE] `, primeira linha "Destinatário original: …", sem cópia; o inválido
  continua `BLOCKED` com o endereço original. O campo `status` em todas.

## 08 · Assinatura nas mensagens · Decorator, **negativo**

- **A frase que tenta:** "talvez a gente troque a assinatura no Natal".
- **Contenção:** a assinatura acrescentada num lugar só (no `EmailSender.send`, que é por onde passa
  toda mensagem, ou no texto da confirmação, que hoje é a única mensagem).
- **Exagero:** um embrulho (Decorator) só para a assinatura; uma assinatura por época do ano.
- **O comportamento novo:** o corpo termina com a linha em branco, `--`, o nome e a linha do telefone (tolerar
  uma quebra de linha no fim).

## 09 · Relatórios novos · **Template Method**, pede

- **Sinais no texto:** dois relatórios novos agora, totalizando quatro com os mesmos passos; uma mudança
  que vale para todos (a linha de título); "o contador já avisou que vai pedir mais".
- **Na base:** `OccupancyReport` e `RevenueReport`, com os mesmos passos escritos duas vezes.
- **Acerto:** o esqueleto (título, cabeçalho, linhas, total) num lugar só, com os passos que variam
  preenchidos por cada relatório (classe abstrata com o método-modelo).
- **Alternativa equivalente:** composição: um escritor de relatório genérico que recebe as partes que
  variam (o filtro, as colunas, a linha, o total) como objetos ou funções.
- **Omissão:** quatro relatórios escritos inteiros, cada um com a sua linha de título.
- **Malfeito:** o esqueleto existe só para os dois novos, e os dois antigos continuam inteiros (com a linha
  de título copiada neles); ou o "esqueleto" é só um método utilitário de CSV, e cada relatório ainda
  repete a sequência dos passos.
- **O comportamento novo:** a linha de título nos quatro (com o período como veio na chamada); a taxa de turismo
  (R$ 4,50 × hóspedes × noites, reservas pagas com entrada no mês, a ordem do financeiro); o relatório por
  plano com as duas linhas sempre presentes e as mesmas reservas do financeiro (inclusive a cancelada não
  reembolsável); os dois antigos iguais depois do título.

## 10 · O nome do hóspede na ocupação · Template Method, **negativo**

- **A frase que tenta:** "talvez a recepção peça o telefone um dia".
- **Contenção:** uma coluna a mais no `OccupancyReport` (e a troca da vírgula por espaço).
- **Exagero:** um esqueleto genérico de relatórios, ou um sistema de colunas configuráveis.
- **O comportamento novo:** a coluna `guest` depois de `reservation`; vírgula vira espaço; o resto igual
  (inclusive o relatório financeiro, que não muda).

---

## O que ainda falta para este gabarito

- a avaliação de verdade, por agente ou à mão: ela tem que pôr as duas soluções de referência de cada cenário
  (em `evaluation/solutions/`) em classes diferentes: nos que pedem o pattern, a `pattern` em "acerto" e a
  `naive` em "omissão"; nos controles negativos, a `contained` em "contenção" e a `overengineered` em
  "exagero".
