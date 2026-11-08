# Revisão: construção, cenário 01

## Visão geral

O agente montou um serviço Javalin em camadas por pacote: `controller` (rotas, DTOs e o mapa de exceções para os
códigos de erro), `service` (as regras da reserva e o e-mail), `domain` (entidades e enums, com os planos de tarifa
em `domain.rateplan`), `repository` (JDBC puro) e `database` (a conexão e o `CREATE TABLE`). Uma requisição entra
pela rota registrada em `Application.java:49-62`, passa pelo controller, que chama o `ReservationService`, e este
pede o plano à `RatePlanFactory` e grava pelo `ReservationRepository`. Os dados ficam em três tabelas do PostgreSQL,
criadas no código (`DatabaseInitializer.java:20-66`), e todos usam uma única `Connection` estática
(`Database.java:9`), sem transação. Os planos de tarifa ficaram numa interface com uma classe por plano, e os 21
valores do cenário conferem. O mais sério: nada impede duas reservas do mesmo quarto nas mesmas noites quando os
pedidos chegam juntos (em 6 de 10 rodadas as duas foram criadas). Há também alguns erros na borda: 500 em vez de 400
para corpo inválido ou campos faltando, o "R$" do comprovante com espaço não separável, e a segunda via sai para
reserva cancelada. O serviço usa a estrutura dos planos mas ainda pergunta qual é o plano no desconto do convênio
(`ReservationService.java:79`). Os testes são poucos: 8, só de unidade, só nos planos.

## Arquitetura

**Números:** 8 pacotes, 14 dependências entre eles, nenhum ciclo. Javalin só em `controller` e na raiz, JDBC só em
`repository`, `database` e na raiz. A maior classe é `ReservationService` (236 linhas), seguida de
`ReservationRepository` (225).
**Nota:** com problemas

As camadas existem e apontam na direção certa: as regras (`service`, `domain`) não conhecem Javalin nem JDBC, o SQL
mora só nos repositórios, e a montagem fica num lugar só (`Application.java:23-37`). Os defeitos estão localizados:

- Os controllers saltam o serviço e vão direto ao repositório (`controller -> repository` no jdeps):
  `ReservationController.java:70` e `:83`, `ReportController.java:41` e `:47`, `RoomController.java:18`.
- Uma regra de negócio mora no handler: a decisão de quando a segunda via existe está em
  `ReservationController.java:78-81`, e é feita pelo valor pago em vez da situação. A versão certa, pela situação, está
  no serviço (`ReservationService.java:159-168`) e ninguém a chama. Por isso a regra está em dois lugares, e o que vale
  é o errado (veja Comportamento).
- Os relatórios (o cálculo do que fica com a pousada, `ReportController.java:84-86`, e a montagem do CSV) estão no
  controller e não no serviço.
- A conexão é um `static` global e preguiçoso (`Database.java:9-26`): uma única `Connection` JDBC para todos os
  threads do Jetty, sem pool. Isso conta de novo no Banco.

## Design de baixo nível

**Números:** 5 planos, todos com uma classe que implementa `RatePlan` (`domain/rateplan/*.java`), escolhidos num único
`switch` de seleção (`RatePlanFactory.java:8-14`). Há 1 ponto de uso que pergunta qual é o plano
(`ReservationService.java:79`) e 4 cópias do mesmo laço de preço por noite.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que o aproxima do acerto: cada um dos cinco planos, os dois antigos inclusive, tem a sua validação
(`CorporateRatePlan.java:19-27`, `WeekendPackageRatePlan.java:19-31`, `LongStayRatePlan.java:12-16`), o seu preço e a
sua devolução (`WeekendPackageRatePlan.java:44-47`, `NonRefundableRatePlan.java:41-43`) num lugar só dele.
O cancelamento pede a devolução ao plano sem nomeá-lo (`ReservationService.java:130-131`), e a escolha é um único
`switch` que só seleciona (`RatePlanFactory.java:8`). O que o afasta: quem usa a estrutura ainda pergunta qual é o
caso. Em `ReservationService.java:79-83` está `if (ratePlanType == RatePlanType.CORPORATE && ratePlan instanceof
CorporateRatePlan corporatePlan)`, que chama uma sobrecarga própria `calculateDiscount(subtotal, companyCode)`
(`CorporateRatePlan.java:53-61`). O método da interface no convênio devolve zero (`CorporateRatePlan.java:48-51`), ou
seja, o contrato `RatePlan.calculateDiscount` está errado para esse plano, e o próximo plano que precisar de um dado da
reserva vai abrir outro `if` no serviço. A interface já recebe `companyCode` no `validate`
(`RatePlan.java:10`), mas não no desconto. Isso é o caso "a estrutura existe, mas convive com o jeito sem ela", e por
isso a classe é malfeito, não acerto.

**O exagero.** Nenhum: a fábrica é o ponto único de seleção que o pattern aceita, e não há interfaces de uma
implementação nem camadas que só repassam.

**O resto do código.**
- Duplicação: o laço da diária com a alta temporada está copiado igual em `StandardRatePlan.java:16-32`,
  `NonRefundableRatePlan.java:16-32`, `CorporateRatePlan.java:30-46` e `LongStayRatePlan.java:19-35`, com o acréscimo
  `new BigDecimal("1.20")` repetido. Uma mudança na temporada tem de ser feita em quatro lugares. A regra da temporada
  é um método `default` da interface (`RatePlan.java:18-35`), o que mistura regra do calendário com o contrato do plano.
- Os tipos estão certos: `BigDecimal` com `HALF_EVEN` a cada passo, `LocalDate`, enums para situação, plano e quarto
  (`RoomType.java:5-8`). Mas o zero de partida não tem escala (`Reservation.java:28`, `StandardRatePlan.java:36`), e
  por isso o JSON sai `0` e não `0.00`.
- O estado: a `Reservation` é um saco de setters (`Reservation.java:39-169`). As transições de situação ficam no
  serviço com `setStatus` (`ReservationService.java:107-156`), e nada no objeto impede uma transição inválida.
- Os erros: o `null` não é tratado. `guests` é `Integer` e é desembrulhado direto (`ReservationController.java:35`),
  `RatePlanType.valueOf(null)` (`:26`) lança NPE, e um `catch` vazio engole a porta inválida
  (`Application.java:77-78`) e outro engole o erro ao fechar (`Database.java:33`). O `catch (Exception)` genérico
  (`ExceptionHandler.java:48-51`) transforma esses NPEs em 500.
- Código morto: `generateDocument` (`ReservationService.java:159`) nunca é chamado, e a variável `room` em
  `ReservationService.java:171` não é usada. O comprovante é montado com `NumberFormat.getCurrencyInstance`
  (`ReservationService.java:26`), que no Java 21 põe um espaço não separável depois do "R$".

## Testes

**Números:** 8 testes rodaram, 0 falhas, 0 erros, 0 pulados; 8 métodos escritos, num arquivo só
(`RatePlanTest.java`). Cobertura de linhas 95 de 794 (12,0%), de ramos 31 de 145 (21,4%). `domain.rateplan` tem 86 de
105 linhas cobertas; `service`, `repository`, `controller` e `database` têm 0.
**Nota:** com problemas

Na construção o texto não pedia testes, e o agente escreveu alguns que conferem a parte nova com os valores do texto:
o STANDARD em julho 599,76 (`RatePlanTest.java:22`), o NON_REFUNDABLE 116,97 (`:42`), Tecnosul 93,58 (`:61`), o pacote
820,00 e a devolução de 410,00 (`:77`, `:83`), e a estadia longa 1.749,30 e 174,93 (`:96`, `:99`). Também conferem as
recusas do convênio com código inválido, do pacote fora de sexta e da estadia curta (`:112`, `:124`, `:136`).

O que ficou sem rede:
- A pergunta da mutação: trocar 0.18 da Agrovale ou 0.25 da Farmavida (`CorporateRatePlan.java:14-15`) não quebra
  nenhum teste, e o convênio sem código (`CorporateRatePlan.java:20`) também não é testado. A temporada nas bordas (14 e
  15/12, fim de fevereiro) também não; trocar `day >= 15` por `day > 15` (`RatePlan.java:26`) passa. O pacote de sexta
  a segunda (3 noites, mesma sexta) não é testado.
- O teste do convênio chama a sobrecarga direto (`RatePlanTest.java:60`). O caminho que o sistema usa de verdade, o
  `if` em `ReservationService.java:79`, fica fora; se alguém apagasse esse `if`, o desconto do convênio viraria zero e
  todos os testes passariam.
- Nada testa as situações da reserva, o cancelamento pelo serviço, o comprovante, o e-mail, os relatórios, os códigos
  de erro da API ou o banco: 0 testes pela API ou contra o banco. Os defeitos que o roteiro de comportamento achou (500
  para corpo inválido, o espaço no "R$", a reserva duplicada) passariam despercebidos.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)`
nas 5 colunas, datas em `date`. Conferências que acusaram: a chave estrangeira `reservations(room_number) -> rooms`
sem índice, nenhum CHECK nas colunas de situação, plano, tipo e canal, nenhuma restrição UNIQUE/EXCLUDE. Esquema criado
por `CREATE TABLE` no código, sem migração. 0 pool, 0 transações, 0 `FOR UPDATE`. No comportamento, 6 de 10 rodadas
simultâneas criaram as duas reservas.
**Nota:** ruim

O que está certo: os tipos (`DatabaseInitializer.java:35-46`), a chave estrangeira para o quarto (`:47`), `NOT NULL`
no obrigatório, todo SQL com `?` e try-with-resources (`ReservationRepository.java:41`, `:99`, `:126`), e os dados
sobrevivem ao reinício (3 de 3 no roteiro).

O que pesa:
- A regra central do banco, um quarto por noite, não tem proteção nenhuma. A verificação é um `SELECT`
  (`ReservationRepository.java:114-142`) seguido de um `INSERT` separado (`ReservationService.java:89-96`), em
  autocommit, sem transação, trava ou restrição (por exemplo, um `EXCLUDE USING gist` sobre o quarto e o intervalo).
  Não há nem um `synchronized` no processo. Medido: 6 de 10 pares simultâneos geraram reservas duplicadas. É dado
  errado no banco: duas famílias no mesmo quarto na mesma noite.
- Uma única `Connection` estática compartilhada por todos os threads do Jetty (`Database.java:9-26`,
  `Application.java:23`), sem pool. Se a conexão cair, o serviço não reconecta e todas as requisições passam a falhar
  até reiniciar.
- O esquema no código com `CREATE TABLE IF NOT EXISTS` (`DatabaseInitializer.java:22`, `:29`, `:52`) não sabe evoluir:
  a próxima coluna (por exemplo, para um plano novo) não chega a um banco que já existe.
- O índice da chave estrangeira falta, e as colunas `status` e `rate_plan` são `varchar` sem CHECK
  (`DatabaseInitializer.java:38`, `:44`).
- `findByMonth` usa `(amount_paid > 0 OR refund > 0)` (`ReservationRepository.java:179`): uma reserva paga e
  devolvida por inteiro entra na lista com valor 0,00. O texto pede só as que deixaram dinheiro.

## Comportamento

**Números:** 65 de 72 conferências passaram. A parte comum: 40 de 46. Depois de reiniciar: 3 de 3. O cenário 01: 21 de
21. Reservas simultâneas: 1 de 2.
**Nota:** com problemas

O cenário passa inteiro: os três convênios com os descontos certos (93,58, 140,36, 194,95), as recusas com 400, o
pacote com preço fechado inclusive em julho e a devolução de 410,00, a estadia longa com 1.574,37 e a temporada valendo,
o `companyCode` ignorado nos outros planos. As falhas:

- **Duas reservas ao mesmo tempo (6 de 10 duplicadas).** Para o dono, é o problema mais sério: o site pode vender o
  mesmo quarto duas vezes para a mesma noite quando dois hóspedes reservam juntos. A causa está no Banco.
- **O e-mail e o comprovante.** O texto não sai como o dono escreveu: "Total: R$ 779,80" sai com um espaço não
  separável (bytes `C2 A0`) depois do "R$", conferido à mão em `GET /messages`. A causa é
  `ReservationService.java:26`. Na tela pode parecer igual, mas é outro texto, e quem comparar ou processar o e-mail vai
  ver a diferença.
- **Corpo que não é JSON e campos faltando: 500 INTERNAL_ERROR** em vez de 400 INVALID_REQUEST. A
  `JsonParseException` e o NPE de `guests` ou `ratePlan` nulos caem no `catch (Exception)`
  (`ExceptionHandler.java:48`). Conferido também: sem `roomNumber` volta 404 ROOM_NOT_FOUND
  (`ReservationService.java:40-41`), e sem `guestName` volta 500 (é o `NOT NULL` do banco que barra). O site recebe
  "erro interno" onde devia receber "dado inválido".
- **`amountPaid` e `discount` como `0` em vez de `0.00`** na reserva recém-criada (`Reservation.java:28`,
  `StandardRatePlan.java:36`). O texto pede 2 casas. É um defeito pequeno, mas é do agente.
- **O 201 comporta 3 e a diária de 389.90 (2 falhas).** Não pesam: a resposta está certa
  (`"number":"201","capacity":3,"nightlyRate":389.90`, conferido com `curl`), mas o número do quarto vem como texto e
  o roteiro procura um número. O texto não fixa o tipo, e texto é uma escolha razoável.

Fora do roteiro, conferido com `curl`: a segunda via de uma reserva CANCELADA responde 200 com o comprovante. O texto
diz que ela só existe para reserva confirmada, com check-in ou com check-out. A causa é
`ReservationController.java:78`, que olha o valor pago e não a situação. A recepção pode tirar comprovante de uma
reserva que não vale mais.

## Dúvidas

- A nota do Banco ficou entre `com problemas` e `ruim`. A skill põe como `com problemas` a duplicação "protegida só
  dentro do processo". Aqui não há proteção nenhuma, e a duplicação foi medida (6 de 10). Por isso dei `ruim`, pelo
  risco de dado errado, mas nenhum item da lista de `ruim` da skill (ponto flutuante, SQL montado, falta de chave, perda
  ao reiniciar) se aplica ao pé da letra.
- A classe do pattern ficou entre `malfeito` e `acerto`. A estrutura cobre os cinco planos no preço, na validação e na
  devolução, e a fuga é um só ponto (`ReservationService.java:79`). Mas é uma regra de preço, o desconto, decidida pelo
  serviço perguntando qual é o plano, e isso é o que a referência chama de malfeito.
- Os Testes ficaram entre `com problemas` e `ruim`. São poucos (12% das linhas) e só de unidade, mas conferem com
  valores a maior parte da parte nova do cenário, e o texto não pedia testes.
