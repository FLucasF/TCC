# Revisão: manutenção, cenário 01

## Visão geral

O agente partiu da base: Javalin em `hotel.web`, regras e acesso a dados em `hotel.reservations`, PostgreSQL com
migrações do Flyway. Fez a menor mudança possível para os três planos novos (`CORPORATE`, `WEEKEND_PACKAGE`,
`LONG_STAY`). O pedido segue este caminho: `HttpApi` lê o novo `companyCode` e o repassa no `NewReservation`
(`src/main/java/hotel/web/HttpApi.java:128-135`). `ReservationService.create` valida o plano num `switch`
(`src/main/java/hotel/reservations/ReservationService.java:54-89`). `PriceCalculator.calculate` calcula o preço com um
`if` para o pacote e um `switch` para os outros planos (`src/main/java/hotel/reservations/PriceCalculator.java:44-60`).
`ReservationStore` grava a reserva numa coluna nova, `company_code`, criada pela migração nova `V2__add_company_code.sql`.
Os dois `switch` que a base já tinha cresceram para cinco casos, e a validação ganhou um terceiro: o Strategy que o
texto pede ("o comercial vive inventando plano") não foi feito. O defeito de comportamento mais sério está no pacote
de fim de semana. A reserva guarda as diárias calculadas (R$ 779,80) e um desconto negativo (R$ -40,20), quando o
texto diz que as diárias são o próprio preço do pacote, sem desconto. Na alta temporada, o desconto fica positivo e
aparece no comprovante. O README não foi atualizado: `README.md:32-38` ainda descreve só dois planos.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números
são os mesmos da base (`measures/dependencies-base.md`), e o ciclo já estava lá. Só os tamanhos mudaram:
`hotel.reservations` passou de 508 para 596 linhas, `hotel.web` de 236 para 243, e `ReservationService` de 138 para 180.
**Nota:** bom

A mudança respeitou as camadas da base. A borda HTTP só ganhou o campo novo, no registro
(`src/main/java/hotel/web/CreateReservationRequest.java:16`) e no repasse (`src/main/java/hotel/web/HttpApi.java:135`),
sem nenhuma regra de plano. As regras novas ficaram no serviço e no cálculo, sem Javalin e sem SQL
(`ReservationService.java:54-89`, `PriceCalculator.java:23-60`). O SQL novo ficou só em `ReservationStore`
(`src/main/java/hotel/reservations/ReservationStore.java:19,31-33,44,133`). A montagem continua num lugar só
(`hotel.Hotel`). O ciclo `documents` ↔ `reservations` veio da base, e o agente não o piorou. Nenhuma dependência nova
entre pacotes.

## Design de baixo nível

**Números:** dos 5 planos, 0 têm o comportamento num lugar só deles. O comportamento dos planos está em 3 decisões
sobre o plano: a validação (`ReservationService.java:55`, um `switch` de 3 casos mais `default`), o preço
(`PriceCalculator.java:44`, um `if` para o pacote, e `:49`, um `switch` de 4 casos mais `default`) e a devolução
(`ReservationService.java:123`, um `switch` de 5 casos).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto descreve uma variação que vai crescer (cada plano "com o seu jeito de calcular o preço, com as
suas regras de quem pode usar e com a sua regra de devolução", e já há mais dois planos a caminho). O agente não criou
nenhuma estrutura para isso. O `enum` `RatePlan` continua sem comportamento
(`src/main/java/hotel/reservations/RatePlan.java:3-14`, só comentários). O `switch` de desconto que a base tinha cresceu
(`PriceCalculator.java:49-58`). O pacote ficou num `if (ratePlan == RatePlan.WEEKEND_PACKAGE)` antes dele
(`PriceCalculator.java:44-47`). O `switch` de devolução também cresceu (`ReservationService.java:123-127`). A validação
de cada plano (código da empresa, sexta a domingo, 7 noites) entrou num terceiro `switch` no serviço
(`ReservationService.java:54-89`). Um plano novo vai mexer em três lugares de duas classes, além do `enum`. A referência
chama isso de omissão: `switch` de comportamento crescendo para os cinco casos e `if (ratePlan == …)` no cálculo.

**O exagero.** Nenhuma estrutura sem necessidade: só constantes, um campo novo e os ramos novos.

**O resto do código.**
- Os dois `switch` novos ganharam um `default` (`PriceCalculator.java:57`, `ReservationService.java:85-87`), que tira
  a conferência de exaustividade que o `switch` sobre `enum` dava na base. Um sexto plano esquecido compila e sai com
  desconto zero e sem validação.
- A regra do convênio está repartida entre duas classes. O serviço consulta a tabela de descontos do cálculo
  (`ReservationService.java:58`, `PriceCalculator.CORPORATE_DISCOUNTS`). O cálculo faz `CORPORATE_DISCOUNTS.get(companyCode)`
  sem checar nada (`PriceCalculator.java:53-54`) e depende de o serviço ter validado antes: chamado direto com um
  código inválido, quebra com `NullPointerException`.
- A conta das noites com `ChronoUnit.DAYS.between` está copiada duas vezes, com nomes totalmente qualificados
  (`ReservationService.java:65,79`), e o laço de `calculate` já conta as noites (`PriceCalculator.java:34-39`).
  `import java.time.DayOfWeek` em `PriceCalculator.java:7` não é usado.
- O pacote é modelado errado no domínio: as diárias calculadas ficam no subtotal e a diferença vira um "desconto" que
  pode ser negativo (`PriceCalculator.java:45-47`), ao contrário do texto ("as diárias e o total da reserva são o
  próprio preço do pacote, sem desconto").
- Os tipos continuam certos: `BigDecimal` com `cents` (HALF_EVEN), `Map<RoomType, BigDecimal>` para os preços do pacote
  (`PriceCalculator.java:28-31`). O `companyCode` é um `String` solto, o que é aceitável aqui.

## Testes

**Números:** 39 testes rodaram, 0 falhas, 0 erros, 0 pulados; 39 métodos escritos (a base tinha 26, todos mantidos;
13 novos). Cobertura de linhas: 94,3% (513 de 544; base 93,8%). Cobertura de ramos: 87,7% (121 de 138; base 87,1%).
`hotel.reservations` cobre 246 de 259 linhas e 69 de 74 ramos.
**Nota:** com problemas

**O que está bem.** Os testes da base continuam passando, e só a assinatura das chamadas mudou (um `null` a mais). As
regras novas estão testadas com valores:
- os três convênios, com diárias, desconto e total (`src/test/java/hotel/reservations/PriceCalculatorTest.java:78-112`),
  incluindo o exemplo do texto (R$ 686,22);
- o pacote nos três tipos de quarto (`:115-143`);
- a estadia longa com o exemplo do texto (`:145-155`);
- as recusas do convênio sem código e com código inválido
  (`src/test/java/hotel/reservations/ReservationServiceTest.java:169-177`), do pacote fora de sexta a domingo e com 3
  noites (`:189-199`) e da estadia com 6 noites (`:211-217`);
- as devoluções, com R$ 410,00 no pacote (`:202-208`) e tudo de volta no convênio e na estadia longa (`:180-186`,
  `:220-226`).

**A pergunta da mutação.**
- Trocar 0.12 por 0.21 faz `PriceCalculatorTest.java:78` falhar.
- Trocar `nights < 7` por `nights <= 7` faz `ReservationServiceTest.java:211` falhar, porque o teste cria uma estadia
  de 7 noites.
- Apagar a conferência da sexta faz `:189` falhar (o teste usa sábado a segunda).

**O que ficou sem rede.**
- O teste do pacote consagra o defeito: ele exige `-40.20` de desconto (`PriceCalculatorTest.java:124`), em vez de
  diárias R$ 820,00 e desconto zero.
- Nenhum teste confere a estadia longa com noites de alta temporada, nem o pacote na alta temporada. Se alguém fizesse
  a estadia longa ignorar a temporada, nenhum teste falharia.
- Nenhum teste pela API cobre o campo `companyCode` nem a resposta 400 das regras de plano: `HttpApiTest` continua com
  os 4 testes da base (`src/test/java/hotel/web/HttpApiTest.java:59-109`).

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` em todas as colunas. Nenhuma chave
estrangeira sem índice e nenhuma data em texto. O quarto é protegido por `EXCLUDE USING gist`. Sem CHECK em
`rate_plan`, `status`, `type` e `channel`, como já era na base. 2 migrações: `V1__create_tables.sql` igual à da base e
`V2__add_company_code.sql` nova.
**Nota:** bom

O agente evoluiu o esquema do jeito certo, com uma migração nova (`src/main/resources/db/migration/V2__add_company_code.sql:1`,
`ALTER TABLE reservations ADD COLUMN company_code text`), sem tocar na V1. Um banco que já roda o sistema sobe sem
problema. O SQL continua com parâmetros (`ReservationStore.java:31-50`, 15 `?`), e a leitura recupera o campo novo
(`ReservationStore.java:133`). Os candidatos a SQL com `+` que o script lista só concatenam a constante `COLUMNS`.

Pontos menores:
- O código da empresa é gravado em qualquer plano (`ReservationStore.java:44`). O texto diz que, fora do convênio, ele
  "pode ignorar". A API de fato o ignora no preço, mas a coluna guarda o que o cliente mandou.
- A coluna `rate_plan` continua `text` sem CHECK, como na base. Agora são cinco valores possíveis.
- O desconto negativo do pacote é gravado como calculado. É um defeito do cálculo, que pesa no design e no
  comportamento, não aqui.

## Comportamento

**Números:** 71 de 72 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01: 20 de 21;
- duas reservas ao mesmo tempo: 2 de 2.
**Nota:** com problemas

Os três convênios, as recusas (400 `INVALID_REQUEST`), as devoluções e os exemplos do texto passam: R$ 686,22,
R$ 820,00 de total, R$ 410,00 de devolução e R$ 1.574,37. Também passam a estadia longa com temporada (R$ 1.754,30), o
pacote em julho com total de R$ 820,00 e o `companyCode` ignorado no `STANDARD`.

A conferência que falhou: "pacote deluxe: diárias 820.00, desconto 0.00, total 820.00". O que veio foi diárias 779.80,
desconto -40.20 e total 820.00 (`PriceCalculator.java:45-47`). O texto não deixa isso em aberto ("as diárias e o total
da reserva são o próprio preço do pacote, sem desconto"). Para o dono da pousada, isso quer dizer duas coisas. A API e o
comprovante mostram ao hóspede diárias de R$ 779,80 num pacote de R$ 820,00. Na alta temporada, a conta se inverte: um
deluxe em julho dá diárias de R$ 935,76 e "desconto" de R$ 115,76. Como o comprovante imprime a linha de desconto
quando ela é positiva (`src/main/java/hotel/documents/ReservationReceipt.java:48-50`), o hóspede do pacote recebe um
comprovante com um desconto que não existe. O que o hóspede paga e o que volta no cancelamento estão certos, e o
relatório financeiro usa o valor pago e o devolvido (`src/main/java/hotel/reports/RevenueReport.java:26,43-44`). O
dinheiro não fica errado, mas o documento entregue ao hóspede e os dados de diárias e desconto ficam.

## Dúvidas

- A validação do plano roda depois da verificação de disponibilidade (`ReservationService.java:42-47`). Um convênio
  sem código num quarto ocupado recebe 409 `ROOM_UNAVAILABLE`, e não 400. O texto não fixa a ordem, por isso não pesou.
- Gravar o `companyCode` também nos planos que não são convênio (`ReservationStore.java:44`) pode ser lido como "não
  ignorar". Contei como ponto menor do banco, sem mudar nenhuma nota.
