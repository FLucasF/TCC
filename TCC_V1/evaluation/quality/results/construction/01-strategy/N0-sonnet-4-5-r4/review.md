# Revisão: construção, cenário 01

## Visão geral

O agente montou um serviço Javalin em camadas, separadas por pacote. `Application` faz a montagem e as rotas
(`Application.java:27-68`). Os controllers em `controller/` leem a requisição e chamam os serviços em `service/`. Os
planos de tarifa ficam em `service/rateplan/`, com uma classe por plano atrás de `RatePlanStrategy`. O acesso ao banco
fica em `repository/`, com JDBC puro e `DriverManager`. Os quartos não estão no banco: são uma lista fixa em memória
(`RoomRepository.java:10-16`). Reservas e mensagens ficam no PostgreSQL, em duas tabelas que o código cria com
`CREATE TABLE IF NOT EXISTS` (`Database.java:27-57`). Os cinco planos foram bem resolvidos, e todas as 21 conferências do
cenário passaram.

O defeito mais sério é que nada impede reservar o mesmo quarto duas vezes. Não há restrição no banco, nem transação,
nem trava (`ReservationService.java:72-81`, `ReservationRepository.java:121-151`). No roteiro, as duas reservas
simultâneas foram criadas nas 10 rodadas. Há também defeitos menores na parte comum:

- um corpo que não é JSON dá 500;
- dinheiro zerado sai no JSON como `0`;
- o e-mail tem um espaço não separável depois de "R$";
- a segunda via sai para reserva cancelada;
- o financeiro lista canceladas sem dinheiro retido.

## Arquitetura

**Números:** 8 pacotes, 19 dependências entre eles, 1 ciclo (`service` ↔ `service.rateplan`). O Javalin aparece só em
`com.pousadamarealta` e em `controller`, e o JDBC só em `repository` (`measures/dependencies.md`).
**Nota:** com problemas

As camadas existem e a direção é quase toda borda → regras → dados:

- **A montagem fica num lugar só,** `Application.java:27-45`, com as dependências passadas pelo construtor.
- **As regras não conhecem Javalin nem SQL.** `ReservationService` só recebe e devolve objetos de domínio
  (`ReservationService.java:33-55`).
- **O SQL fica todo nos repositórios.**

Os defeitos são localizados:

- **Uma regra de negócio mora no handler.** "A segunda via só existe para reserva paga" é decidida em
  `ReservationController.java:85-87`, e com o critério errado (`amountPaid == 0`, que deixa passar a cancelada). Já
  existe no serviço um `generateReceipt` com outra regra (`ReservationService.java:145-154`, status ≠ PENDING), que
  ninguém chama. A mesma regra está em dois lugares e com dois critérios.
- **Os controllers vão direto ao repositório.** `ReservationController.java:22` e `:77`/`:89` buscam o quarto para montar
  o comprovante. `RoomController` e `MessageController` leem o repositório sem passar pela camada de regras, o que
  numa leitura simples ainda é tolerável.
- **Há um ciclo entre pacotes vizinhos.** Os planos dependem de `service.PricingService`
  (`CorporateRatePlan.java:6`, `StandardRatePlan.java:5`), e `service` depende de `rateplan`
  (`ReservationService.java:7-8`). O ciclo é pequeno: o cálculo de diária e o arredondamento deveriam ficar junto dos
  planos, ou num pacote de preço que os dois usem.
- **O tamanho está equilibrado.** A maior classe é o `ReservationRepository`, com 237 linhas.

## Design de baixo nível

**Números:** 5 planos pedidos, 5 implementações de `RatePlanStrategy`, e 0 `switch` ou `if` de comportamento sobre o
plano fora delas. Há um único `switch` de seleção (`RatePlanFactory.java:14-20`). O serviço chama o plano em 3 pontos:
validação, preço e devolução (`ReservationService.java:39-49`, `:116-118`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern (Strategy).** A interface `RatePlanStrategy` (`RatePlanStrategy.java:9-16`) tem `validate`,
`calculateSubtotal`, `calculateDiscount` e `calculateRefund`. Os cinco planos, os dois do ponto de partida inclusive,
estão cada um numa classe com as suas três regras:

- `StandardRatePlan.java:22-43`;
- `NonRefundableRatePlan.java:18-35`;
- `CorporateRatePlan.java:12-50`, com os códigos e a validação do código;
- `WeekendPackageRatePlan.java:15-55`, com sexta a domingo, preço fechado e metade de devolução;
- `LongStayRatePlan.java:13-45`, com o mínimo de 7 noites.

O serviço cria e cancela a reserva sem nomear plano (`ReservationService.java:39-52`, `:116-118`). A seleção é um único
`switch` que só escolhe a implementação, o que a referência aceita. A única regra de plano fora do plano é guardar o
`companyCode` em qualquer plano (`ReservationController.java:43`), que é inofensiva.

Dois detalhes pequenos:

- Os planos que reaproveitam a diária criam um `StandardRatePlan` por dentro (`CorporateRatePlan.java:23`,
  `LongStayRatePlan.java:20`, `NonRefundableRatePlan.java:16`), em vez de usar um cálculo de diárias comum.
- A fábrica cria objetos novos a cada chamada.

Nada disso muda a classe.

**O exagero.** Não há interfaces de uma implementação só, nem camadas que só repassam. A fábrica é o ponto único de
seleção que o próprio pattern pede. Os DTOs são `record`s simples.

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` (`PricingService.java:12,41-43`),
`LocalDate`, e `enum` para status, plano e tipo de quarto. Há defeitos localizados:

- **Formatação de dinheiro.** `NotificationService.java:13` usa um `NumberFormat` `static` compartilhado entre as
  threads do Jetty, e `NumberFormat` não é thread-safe. Além disso, no Java 21 ele produz "R$ 779,80": o espaço
  depois de "R$" não é um espaço comum. Foi o que fez falhar a conferência do e-mail.
- **Código morto com regra divergente.** `ReservationService.generateReceipt` (`ReservationService.java:145-154`)
  ninguém chama, e tem uma regra de segunda via diferente da que o controller aplica.
- **Duplicação.** A busca `roomRepository.findByNumber(...).orElseThrow()` se repete em
  `ReservationService.java:100,152` e em `ReservationController.java:77,89`. O formato `yyyy-MM-dd` aparece em três
  classes.
- **Erros mal classificados.** Todo erro de leitura do corpo cai no `catch (Exception)` genérico, que responde 500
  (`Application.java:81-85`, com `bodyAsClass` em `ReservationController.java:30`). `guests` é `int` primitivo
  (`CreateReservationRequest.java:12`), então um campo ausente vira 0, e "falta de campo" acaba saindo como "menos de um
  hóspede".
- **Estado sem controle.** `Reservation` tem setter em tudo (`Reservation.java:30-165`). Qualquer camada pode mudar o
  status sem passar por uma regra.
- **Arredondamento duplicado e import morto.** A devolução é arredondada duas vezes (`WeekendPackageRatePlan.java:54` e
  `ReservationService.java:118`), e `RatePlanStrategy.java:7` importa `LocalDate` sem usar.

## Testes

**Números:** 16 testes rodaram, com 0 falhas, 0 erros e 0 pulados; 16 métodos escritos. A cobertura é de 16,2% das
linhas (117 de 724) e 19,7% dos ramos (23 de 117). `rateplan` tem 60 de 74 linhas; `controller`, `repository` e a
montagem têm 0 (`measures/tests.md`).
**Nota:** com problemas

O que existe é bom, mas cobre pouco do sistema. Há dois arquivos, ambos de unidade e sem mocks:

- **`PricingServiceTest`** confere com valores a temporada, incluindo a borda de 14/12 (`PricingServiceTest.java:50-54`),
  e o "meio para o par" (`:56-65`).
- **`RatePlanTest`** confere os exemplos do texto com valor: Tecnosul 779,80 / 93,58 / 686,22
  (`RatePlanTest.java:61-78`), o pacote 820,00 com devolução de 410,00 (`:93-115`), a estadia longa 1.749,30 / 174,93
  (`:129-147`) e o exemplo de 599,76 em julho (`:161-170`).

A pergunta da mutação deixa buracos:

- **Agrovale e Farmavida não têm teste.** Trocar 0.18 por 0.81 (`CorporateRatePlan.java:14-15`) não quebra nada.
- **O convênio sem código também não.** Só o código inválido é testado (`RatePlanTest.java:81-90`).
- **O pacote só tem o caso quarta → sexta testado** (`:118-126`). Apagar a conferência de 2 noites
  (`WeekendPackageRatePlan.java:30-32`) não quebra nenhum teste, e o preço sem temporada em julho não é testado.
- **As recusas só conferem o tipo da exceção** (`assertThrows(ApiException.class, ...)`), nunca o 400/INVALID_REQUEST.
- **Fora dos planos, nada é testado:** as transições de situação, o cancelamento pelo serviço, a disponibilidade, os
  relatórios, o comprovante e o e-mail, os códigos de erro da API e os repositórios.

O `pom.xml` declara Testcontainers (`pom.xml:61-73`), mas nenhum teste o usa.

## Banco

**Números:** 2 tabelas (`reservations` e `messages`), ambas com chave primária. O dinheiro está todo em
`numeric(10,2)`, e as datas em `date`. Há 0 chaves estrangeiras (não existe tabela de quartos), 0 CHECK e 0
UNIQUE/EXCLUDE, 0 transações, 0 `FOR UPDATE` e nenhum pool de conexões (`measures/database.md`). No roteiro de
comportamento, as duas reservas simultâneas foram criadas em 10 de 10 rodadas.
**Nota:** ruim

O que está certo:

- os tipos de dinheiro e de data;
- o SQL todo com `?` (nenhum candidato a SQL montado com `+`);
- as conexões fechadas com try-with-resources;
- os dados sobrevivem ao reinício (3 de 3 no roteiro).

O defeito sério é a regra "um quarto não pode ter duas reservas na mesma noite", que ninguém protege. O serviço
consulta os conflitos (`ReservationRepository.java:121-151`) e depois insere (`ReservationService.java:72-81` → `:54`),
cada passo numa conexão própria em autocommit (`Database.java:19-21`). Não há restrição `EXCLUDE`, nem transação, nem
trava; não há sequer um `synchronized` no processo. O resultado medido é que todas as 10 rodadas de pedidos
simultâneos criaram as duas reservas. Isso é dado errado no banco: quarto vendido duas vezes.

Outros defeitos:

- **O esquema não tem como evoluir.** Ele é criado no código com `CREATE TABLE IF NOT EXISTS` (`Database.java:27-57`),
  sem migrações, então não sabe acrescentar uma coluna a uma tabela que já existe.
- **`room_number` é um `VARCHAR` solto** (`Database.java:30`), sem chave estrangeira para quarto nenhum.
- **Status, plano e canal são `VARCHAR` sem CHECK** (`Database.java:37,43,52`).
- **Duas escritas que deviam andar juntas não estão numa transação.** A confirmação grava a reserva e depois a
  mensagem (`ReservationService.java:98,101`); se a segunda falhar, a reserva fica confirmada sem e-mail.
- **Cada chamada abre uma conexão nova** com `DriverManager`, sem pool.
- **O financeiro filtra com `EXTRACT(YEAR/MONTH FROM check_in)`** (`ReservationRepository.java:185-186`), uma consulta
  que não usa índice.

## Comportamento

**Números:** 66 de 72 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 41 de 46 |
| dados depois de reiniciar | 3 de 3 |
| cenário 01 | 21 de 21 |
| reservas simultâneas | 1 de 2 |

**Nota:** com problemas

O cenário está inteiro certo, conferido pela API:

- os três convênios (93,58, 140,36 e 194,95 de desconto);
- convênio sem código, ou com código inválido, recusado com 400;
- o pacote a 520,00, 820,00 e 1.200,00, inclusive em julho, com 410,00 de devolução e as datas erradas recusadas;
- a estadia longa a 1.574,37, com a temporada valendo, e 6 noites recusadas;
- o `companyCode` ignorado nos outros planos.

As falhas que pesam:

- **Reservas simultâneas:** nas 10 rodadas, os dois pedidos foram aceitos (201 e 201). Para o dono da pousada, isso
  quer dizer que dois hóspedes que reservarem o mesmo quarto pelo site no mesmo instante saem os dois com reserva. O
  defeito é do banco e pesa lá; aqui ele aparece porque o roteiro o pede.
- **Corpo que não é JSON:** responde 500 INTERNAL_ERROR em vez de 400 INVALID_REQUEST (`Application.java:81-85`). O
  site recebe "erro interno" por um erro dele.
- **Dinheiro com 2 casas:** `amountPaid` e `discount` saem como `0`, e não `0.00`, na reserva recém-criada. O motivo é
  `BigDecimal.ZERO` (`Reservation.java:27`, `StandardRatePlan.java:37`, `WeekendPackageRatePlan.java:49`). O texto pede
  número com 2 casas.
- **E-mail:** o comprovante sai como "Total: R$ 599,76", com o espaço não separável do `NumberFormat`
  (`NotificationService.java:13`; conferi os bytes, `302 240`). Para quem lê é igual, mas o texto não é o pedido, e
  qualquer comparação ou busca pelo texto falha.

Uma falha vem de um ponto que o texto deixava em aberto: "o 201 comporta 3" e "a diária do 201" falharam só porque
`number` sai como texto (`"201"`) e o roteiro procura um número (`RoomResponse`, `RoomRepository.java:11-15`). A
capacidade (3) e a diária (389.90) estão certas.

Achei mais dois defeitos fora do roteiro, chamando a API:

- **A segunda via sai para reserva cancelada.** Uma STANDARD confirmada e depois cancelada devolve 200 em
  `GET /reservations/1/document`, porque o controller só olha `amountPaid` (`ReservationController.java:85`). O texto
  diz que a segunda via só existe para reserva confirmada, com check-in ou com check-out.
- **O financeiro lista canceladas que não deixaram dinheiro.** Uma cancelada com devolução total, ou uma pendente
  cancelada, aparece como linha de 0.00 (`ReservationRepository.java:187`: `amount_paid > 0 OR refund IS NOT NULL`). O
  total fica certo, mas o texto pede só as reservas que deixaram dinheiro com a pousada.

## Dúvidas

- **O tipo de `number` e de `roomNumber` no JSON:** o texto não diz se é texto ou número. Tratei como ponto em aberto,
  e as duas falhas de `/rooms` não pesaram.
- **O espaço não separável no e-mail:** pesei como defeito, porque o texto do e-mail é dado literalmente. Um leitor
  mais brando pode vê-lo como cosmético.
- **A nota do banco:** o roteiro de banco põe "duplicação protegida só dentro do processo" em `com problemas`. Aqui não
  há proteção nenhuma, e a duplicação aconteceu em 10 de 10 rodadas, por isso dei `ruim` (risco de dado errado). Um
  leitor que siga só a lista do `ruim` da skill daria `com problemas`.
