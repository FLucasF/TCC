# Revisão: manutenção, cenário 01

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas em `HttpApi`, que leem o JSON e chamam o serviço),
`hotel.reservations` (as regras em `ReservationService` e `PriceCalculator`, o acesso ao banco em `ReservationStore`),
`hotel.rooms`, `hotel.messages`, `hotel.documents`, `hotel.reports` e a montagem em `hotel.Hotel`. Os dados ficam no
PostgreSQL, e o esquema vem de migrações do Flyway. Uma reserva nova entra por `HttpApi.java:130`, passa por
`ReservationService.create` (`ReservationService.java:47`), que pede o preço ao `PriceCalculator` e grava com
`ReservationStore.add`. Para atender ao pedido, o agente acrescentou três constantes ao `enum` `RatePlan`
(`RatePlan.java:8-13`), aumentou o `switch` de preço de `PriceCalculator.calculate` para cinco casos
(`PriceCalculator.java:38-44`) e o `switch` de devolução de `ReservationService.cancel` também para cinco
(`ReservationService.java:84-91`), passou o `companyCode` da requisição até o banco e criou uma migração nova (`V2`).
O sistema faz tudo o que o texto pede (72 de 72 conferências do roteiro), e os 50 testes passam. O mais sério é de
design: o texto avisa que vão chegar outros planos, e a mudança não preparou o código para isso. As regras de cada
plano ficaram espalhadas em dois `switch` de comportamento, em duas classes, que vão precisar de um caso novo a cada
plano. O README também não foi atualizado: ainda descreve só os dois planos antigos e o corpo sem `companyCode`
(`README.md:32`, `README.md:38`, `README.md:70`).

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem
os mesmos pacotes, as mesmas dependências e o mesmo ciclo (`measures/dependencies-base.md`). O que mudou foi o
tamanho de `hotel.reservations`, de 508 para 603 linhas: `PriceCalculator` foi de 53 para 133 linhas.
**Nota:** bom

O agente não mexeu na estrutura. A borda HTTP continua fina e só repassa o campo novo (`HttpApi.java:128-130`,
`CreateReservationRequest.java:16`). O SQL novo ficou dentro do `ReservationStore` (`ReservationStore.java:19`, `:32`,
`:44`, `:133`). As regras novas não conhecem o Javalin nem o JDBC: estão em `PriceCalculator.java:59-95` e
`ReservationService.java:84-91`. O ciclo `documents` ↔ `reservations` (`ReservationService.java:3` importa
`ReservationReceipt`, que importa `Reservation`) veio da base, e o agente não o piorou. A única dependência nova é a
de `PriceCalculator` em `hotel.rooms.RoomType` (`PriceCalculator.java:4`), e `reservations -> rooms` já existia.
Nenhuma classe ficou grande demais (a maior continua sendo `HttpApi`, com 171 linhas).

## Design de baixo nível

**Números:** dos 5 planos (2 da base e 3 novos), nenhum está numa estrutura própria. O comportamento é decidido por
2 `switch` sobre o plano: o preço e a validação em `PriceCalculator.java:38-44`, com um método privado por plano, e
a devolução em `ReservationService.java:84-91`. As regras de quem pode usar cada plano ficam dentro dos métodos de
preço (`PriceCalculator.java:60`, `:71-82`, `:88`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto descreve uma variação que já tem cinco casos e vai crescer ("o comercial vive inventando
plano… Cada plano chega com o seu jeito de calcular o preço, com as suas regras de quem pode usar e com a sua regra de
devolução"). A resposta esperada era um Strategy: cada plano com preço, validação e devolução num lugar só dele. O
agente fez o que a referência chama de omissão: os dois `switch` que já existiam na base cresceram para os cinco
casos. `RatePlan` continua sendo um `enum` sem comportamento (`RatePlan.java:3-14`). O preço é escolhido por um
`switch` de comportamento que chama `calculateStandard`, `calculateCorporate` e os outros
(`PriceCalculator.java:38-44`). A devolução de cada plano continua em outro `switch`, em outra classe
(`ReservationService.java:84-91`). As constantes de cada plano também estão espalhadas: os descontos e as tabelas
ficam no calculador (`PriceCalculator.java:20-33`), e o "metade" do pacote está escrito à mão no serviço
(`ReservationService.java:88-89`). Para criar o plano "melhor idade", seria preciso mudar o `enum`, os dois `switch`
e acrescentar constantes e um método no `PriceCalculator`. Separar um método privado por plano melhora a leitura,
mas não é uma estrutura: o `switch` continua decidindo o comportamento.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface, fábrica ou camada nova. Há contenção.

**O resto do código.** Os tipos são os certos: `BigDecimal` com `HALF_EVEN` (`PriceCalculator.java:130-132`; a
metade em `ReservationService.java:88-89` mantém a escala 2), `LocalDate`, `DayOfWeek` e um `Map<RoomType, BigDecimal>`
para o preço fechado (`PriceCalculator.java:29-33`). Os defeitos são pontuais:
- a mesma montagem `new Price(nights, subtotal, discount, subtotal.subtract(discount))` aparece quatro vezes
  (`PriceCalculator.java:50`, `:56`, `:67`, `:94`), e os métodos de STANDARD, NON_REFUNDABLE e LONG_STAY só mudam a taxa;
- `countNights` repete o laço de `calculateSubtotal` só para contar os dias (`PriceCalculator.java:105-111` e
  `:97-103`), quando `ChronoUnit.DAYS.between` resolveria;
- `calculate` recebe um `String companyCode` que só serve a um dos planos (`PriceCalculator.java:35`). O parâmetro de
  um caso vazou para a assinatura de todos, e o próximo plano com um dado próprio vai aumentar a lista de novo;
- a conferência do domingo (`PriceCalculator.java:79-82`) nunca dispara: depois de exigir 2 noites e entrada na
  sexta, a saída é sempre um domingo;
- o código da empresa é gravado em qualquer plano (`ReservationStore.java:44`). Um STANDARD que manda `companyCode`
  fica com o código no banco, quando o texto dizia que ele podia ser ignorado.

## Testes

**Números:** 50 testes rodaram, nenhum falhou nem foi pulado, e há 50 métodos de teste escritos. A base tinha 26, e
o agente acrescentou 24 sem apagar nem enfraquecer nenhum (as mudanças nos testes antigos só acrescentam o argumento
`null`). A cobertura é de 94,1% das linhas (514 de 546; a base tinha 93,8%) e 88,2% dos ramos (120 de 136; a base
tinha 87,1%). Em `hotel.reservations`, 251 de 265 linhas e 68 de 72 ramos estão cobertos.
**Nota:** bom

Os testes conferem a parte nova com valores e em três níveis:
- no calculador, os três convênios com diárias, desconto e total, inclusive o exemplo do financeiro (779,80, 93,58
  e 686,22) e os arredondamentos de 134,946 e 62,475 (`PriceCalculatorTest.java:79-111`); o código ausente e o
  inválido (`:113`); o pacote nos três quartos (`:126-156`); as recusas por dia e por número de noites (`:158`,
  `:167`); a estadia longa com o exemplo de 1.574,37 (`:176`), com alta temporada (`:189`) e com 6 noites (`:201`);
- no serviço, contra o banco, as devoluções: tudo no convênio e na estadia longa, e 260,00 no pacote standard
  (`ReservationServiceTest.java:179`, `:193`, `:208`);
- pela API, o 400 com `INVALID_REQUEST` e o 201 com o total (`HttpApiTest.java:124-195`).

A pergunta da mutação: trocar 0.12 por 0.21, `nights < 7` por `nights <= 7`, a metade do pacote por outro valor ou
a tabela de 520,00 faz algum teste falhar; apagar a conferência de `null` do código também, porque o `Map.of` lança
NPE em `containsKey(null)`. Ficam sem rede:
- a conferência do domingo, que é código morto (`PriceCalculator.java:79-82`);
- o pacote em alta temporada, cujo preço deve continuar fechado. A tabela fixa torna o defeito improvável, e o
  roteiro do comportamento conferiu esse caso;
- a devolução do pacote deluxe (410,00), que só é conferida pelo roteiro, embora o mesmo cálculo esteja testado no
  standard.

Uma fraqueza pequena: os testes pela API comparam dinheiro com `asDouble()` (`HttpApiTest.java:145`, `:170`,
`:195`), mas o padrão já vinha da base (`HttpApiTest.java:77`).

## Banco

**Números:** 3 tabelas, todas com chave primária. Todo o dinheiro está em `numeric(10,2)`, as datas estão em `date`,
nenhuma chave estrangeira está sem índice, e a restrição `EXCLUDE` impede reserva duplicada. As conferências acusaram
só as colunas de situação e de plano em `text` sem `CHECK` (`reservations.rate_plan`, `reservations.status`,
`rooms.type`, `messages.channel`), que vieram da base. Migrações: `V1__create_tables.sql` igual à da base e
`V2__add_company_code.sql` nova.
**Nota:** bom

O agente fez a mudança do jeito certo: uma migração nova, sem editar a que já existia
(`src/main/resources/db/migration/V2__add_company_code.sql:2`, `ALTER TABLE reservations ADD COLUMN company_code
text`). A coluna aceita nulo, o que é certo, já que só o convênio usa o código, e a migração sobe sobre um banco que
já tem reservas. O SQL alterado continua com parâmetros (`ReservationStore.java:31-33`, `:44`), e a leitura devolve o
campo (`ReservationStore.java:133`). Um detalhe que não muda a nota: o código é gravado mesmo nos planos que não são
o convênio (`ReservationStore.java:44`), e nada no banco amarra `company_code` ao `rate_plan`. É um dado sem uso, não
um dado errado. A falta de `CHECK` em `rate_plan` veio da base. Quando o agente acrescentou três valores ao plano, ele
poderia ter criado essa restrição, mas não criar não é defeito novo.

## Comportamento

**Números:** 72 de 72 conferências passaram. A parte comum passou em 46 de 46, os dados depois de reiniciar em 3 de
3, o cenário 01 em 21 de 21 e as duas reservas ao mesmo tempo em 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API, com os valores do financeiro:
- o convênio da Tecnosul dá 779,80, 93,58 e 686,22, e Agrovale e Farmavida aplicam 18% e 25%;
- o convênio sem código, ou com um código desconhecido, é recusado com 400 `INVALID_REQUEST`;
- o pacote deluxe sai 820,00, e o cancelamento depois do pagamento devolve 410,00; o pacote custa 520,00 no
  standard e 1.200,00 na suíte, e continua com o mesmo preço em julho;
- o pacote fora de sexta a domingo é recusado;
- a estadia longa dá 1.574,37, a temporada continua valendo, e a reserva com 6 noites é recusada;
- o convênio e a estadia longa devolvem tudo;
- o `companyCode` é ignorado no STANDARD, e o NON_REFUNDABLE continua com 15%.

A parte comum não regrediu, os dados continuam depois de reiniciar, e a reserva simultânea continua protegida.

## Dúvidas

- O código da empresa só é aceito em maiúsculas exatas (`PriceCalculator.java:60`): um `tecnosul` é recusado. O texto
  diz que o código "é o nome dela em maiúsculas", então a escolha é defensável, mas o roteiro não confere esse caso.
- Gravar o `companyCode` também nos planos que não são o convênio (`ReservationStore.java:44`) cumpre o "pode
  ignorar" no preço, mas não no dado guardado. Pelo critério da referência, é uma regra pequena de plano fora do
  plano, e não muda a classe, que já é omissão pelos dois `switch`.
