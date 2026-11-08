# Revisão: construção, cenário 05

## Visão geral

O agente montou o projeto em oito pacotes (`controller`, `service`, `repository`, `model`, `dto`, `exception`, `database` e a raiz com `Application`), ligados no `main` (`Application.java:26-55`). Uma requisição entra pelo Javalin, cai num controller que valida o JSON e converte os campos (`ReservationController.java:33-59`), chama o `ReservationService` (regras de situação, preço via `PricingService`, documento via `DocumentService`, e-mail via `EmailService`) e o `ReservationRepository` grava com JDBC num PostgreSQL cujo esquema é criado por `CREATE TABLE IF NOT EXISTS` ao subir (`DatabaseInitializer.java:14-54`). A identificação do hóspede é uma interface selada com três records (`GuestIdentification.java:3-13`) e a escolha do documento mora num `switch` só (`DocumentService.java:23-29`), que os três lugares usam. O mais sério: as colunas `guest_cpf VARCHAR(11)` e `company_cnpj VARCHAR(14)` (`DatabaseInitializer.java:27-28`) não cabem o CPF `111.222.333-44` nem o CNPJ `12.345.678/0001-90` que o próprio enunciado usa como exemplo, então toda reserva de pessoa com CPF ou de empresa responde 500 (`measures/behavior-service.log:67,123`); só o estrangeiro consegue reservar. Além disso nada impede duas reservas simultâneas do mesmo quarto (sem transação, trava ou restrição), e os testes não exercitam a parte nova do cenário.

## Arquitetura

**Números:** 8 pacotes, 16 dependências entre pacotes, nenhum ciclo (`measures/dependencies.md`). Javalin só na raiz e em `controller`; JDBC só em `repository` e `database`; `service` não usa nenhum dos dois. Maior classe: `ReservationRepository` (243 linhas).
**Nota:** com problemas

As camadas existem e apontam na direção certa: as regras ficam em `service`, que depende só de `model`, `repository` e `exception` (`measures/dependencies.md`); o SQL fica todo em `repository`; a montagem é num lugar só (`Application.java:26-55`), com injeção por construtor. O que pesa:

- Os controllers pulam a camada de regras e falam direto com os repositórios: `ReservationController.java:21,88,101` (`RoomRepository`), `RoomController.java:10` e `MessageController.java:10`. A dependência `controller -> repository` aparece no jdeps.
- Uma regra de negócio mora no handler: a segunda via "só existe para reserva paga" é decidida em `ReservationController.java:97-99` com `getAmountPaid().doubleValue() == 0`, e está errada (uma reserva cancelada continua com `amountPaid` > 0 e ganha segunda via; devolve 400 em vez de 409). A montagem do documento no check-out e na segunda via (buscar o quarto, gerar o texto, montar o DTO) está duplicada no controller (`ReservationController.java:88-90` e `101-103`) em vez de ficar no serviço, que já faz o mesmo na confirmação (`ReservationService.java:108-110`).
- A montagem inteira é copiada no teste (`PousadaSystemIT.java:36-98` repete `Application.java:26-88` linha a linha), sinal de que ela não está num método reutilizável.

## Design de baixo nível

**Números:** 3 tipos de hóspede, os 3 dentro do `switch` da fábrica (`DocumentService.java:24-28`); 3 lugares pedem o documento à fábrica (`ReservationService.java:109`, `ReservationController.java:89` e `102`); 0 lugares criam o documento à mão. Fora da fábrica, o tipo de hóspede é consultado de novo em 5 lugares (`DocumentDTO.java:29-33` e `39-43`, `ReservationDTO.java:36-40`, `ReservationRepository.java:187-191` e `217-225`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

O pattern: a forma escolhida é a fábrica concentrada. `DocumentService.generateDocument` faz um `switch` exaustivo sobre a interface selada `GuestIdentification` (`DocumentService.java:23-29`, `GuestIdentification.java:3-13`) e devolve o texto do documento certo; a confirmação (`ReservationService.java:109`), o check-out (`ReservationController.java:89`) e a segunda via (`ReservationController.java:102`) pedem à fábrica, sem perguntar o tipo. Para a agência de turismo, o compilador apontaria o `switch` a completar. O que afasta do acerto pleno, sem derrubá-lo: a fábrica devolve só uma `String`; o título do documento é decidido por um segundo `switch` fora dela (`DocumentDTO.java:29-33`), então o "documento" está partido em dois lugares. E `generateComprovante` ainda faz um `instanceof` para saber o que o `switch` já sabia (`DocumentService.java:36`).

Exagero: não há. Sem interfaces de uma implementação só, sem camadas que só repassam; os DTOs em `record` e a hierarquia de exceções (`PousadaException.java`) têm uso.

O resto do design:

- Duplicação: os três geradores repetem as linhas de quarto, datas, diárias, desconto e total (`DocumentService.java:41-53`, `64-76`, `90-102`), e a cadeia de `instanceof` que achata a identificação em cinco campos aparece três vezes (`ReservationDTO.java:36-40`, `DocumentDTO.java:39-43`, `ReservationRepository.java:187-191`).
- Tipos: o dinheiro é `BigDecimal` com `HALF_EVEN` a cada passo (`PricingService.java:18,31,36,44`), certo; mas os DTOs convertem para `double` (`ReservationDTO.java:46-51`, `DocumentDTO.java:49-51`, `RoomDTO.java:16`), e o relatório financeiro usa `stripTrailingZeros()` (`ReportService.java:74`), que imprime `397.7` onde o texto pede `397.70`. A regra "só reserva paga" compara `BigDecimal` por `doubleValue() == 0` (`ReservationController.java:97`).
- O formato do CPF e do CNPJ depende do comprimento do texto que veio (`DocumentService.java:123-129`, `131-138`): com 11 dígitos formata, com qualquer outra coisa devolve como veio; a reserva mostra `guestCpf` cru (`11122233344`) e o documento mostra formatado.
- Estado: `Reservation` é um bean mutável com 15 setters (`Reservation.java:24-156`), sem invariantes; o serviço monta o objeto campo a campo (`ReservationService.java:66-89`).
- Erros: o `catch (Exception)` global devolve 500 para o JSON inválido, que o texto pede como 400 (`Application.java:84-88`, `ReservationController.java:34`).
- Nomes claros, métodos curtos; `PricingService` e `EmailService` são limpos.

## Testes

**Números:** 7 testes rodaram, 0 falharam, 0 pulados; 7 métodos de teste; cobertura de 59,8% das linhas (496/829) e 33,8% dos ramos (73/216); 1 arquivo, pela API com RestAssured contra o banco de `DB_URL` (`measures/tests.md`). Pacote `service`: 121 de 230 linhas.
**Nota:** com problemas

Os testes que existem conferem com valores: a lista de quartos (`PousadaSystemIT.java:118-122`), o preço de duas noites STANDARD (499,80, `:152-154`), a alta temporada (599,76, `:181`), o desconto NON_REFUNDABLE (116,97 e 662,83, `:206-208`), a confirmação com o pago igual ao total e o e-mail com assunto e saudação (`:241-254`), `ROOM_UNAVAILABLE` (`:307-308`) e `CAPACITY_EXCEEDED` (`:332-333`).

O que ficou sem rede: a parte nova do cenário inteira. Os sete testes usam só `guestCpf` com 11 dígitos (`:132,166,191,218,264,293,318`); nenhum cria reserva de empresa nem de estrangeiro, nenhum olha a fatura, o ISS, o booking receipt, o campo `text`, o check-out, a segunda via ou as recusas de identificação (nenhuma, duas, incompleta). Pela pergunta da mutação: trocar o ISS de 0.05 para 0.50 (`PricingService.java:44`), trocar `BRL` por `R$` no receipt, ou apagar a regra "exatamente um jeito" (`ReservationController.java:132-138`) não derruba teste nenhum. Também não há teste de cancelamento e devolução, de check-in/check-out, dos relatórios, dos erros 400 nem dos dados depois de reiniciar. Como nenhum teste manda o CPF formatado do enunciado, o defeito das colunas curtas passou despercebido. Detalhes: porta fixa 7777 (`:32`) e banco padrão `localhost/pousada_test` quando `DB_URL` não vem (`:337-339`).

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)` (5 colunas, ok); datas em `date`; 1 chave estrangeira (`reservations.room_number -> rooms`) sem índice; 0 CHECK, 0 UNIQUE/EXCLUDE; esquema por `CREATE TABLE` no código, sem migrações; 0 transações explícitas, 0 `FOR UPDATE`; SQL todo com parâmetros; pool HikariCP (`measures/database.md`).
**Nota:** ruim

O que está certo: tipos de dinheiro e data (`DatabaseInitializer.java:32-33,37-42`), chaves primárias, a chave estrangeira para o quarto (`:43`), SQL parametrizado e conexões em try-with-resources em todos os repositórios (`ReservationRepository.java:32-33`, `MessageRepository.java:27-28`), pool configurado (`Application.java:104-110`), os quartos semeados com `ON CONFLICT DO NOTHING` (`RoomRepository.java:64`).

O que derruba a coluna:

- `guest_cpf VARCHAR(11)` e `company_cnpj VARCHAR(14)` (`DatabaseInitializer.java:27-28`) guardam só dígitos, mas o texto mostra o CPF como `111.222.333-44` e o CNPJ como `12.345.678/0001-90`, e o sistema não normaliza o que recebe (`ReservationRepository.java:187-188` grava como veio). O INSERT falha com `value too long for type character varying(11)` (`measures/behavior-service.log:67`) e `(14)` (`:123`), e a reserva de pessoa com CPF ou de empresa responde 500. É um erro que quebra o caminho principal.
- Nada protege "um quarto não pode ter duas reservas na mesma noite": a conferência é um SELECT seguido de INSERT sem transação, trava ou restrição de exclusão (`ReservationService.java:60,91`, `ReservationRepository.java:93-121`). Chamando a API à mão com duas requisições simultâneas, 4 de 5 rodadas criaram as duas reservas (201 e 201); o roteiro só não viu isso porque as duas receberam 500 pelo CPF.
- O esquema é `CREATE TABLE IF NOT EXISTS` no código (`DatabaseInitializer.java:14,22,48`): não evolui; acrescentar uma coluna (a agência de turismo, por exemplo) num banco que já tem dados exige mexer à mão.
- Menor: chave estrangeira sem índice; `status`, `rate_plan`, `type` e `channel` sem CHECK; `findByMonth` filtra com `EXTRACT` sobre a coluna (`ReservationRepository.java:155-156`), que não usa índice; o relatório de ocupação consulta o quarto de cada reserva uma a uma (`ReportService.java:31`).

## Comportamento

**Números:** 30 de 83 conferências passaram. Parte comum 13 de 46; dados depois de reiniciar 1 de 3; cenário 05 15 de 32; duas reservas ao mesmo tempo 1 de 2 (`measures/behavior.md`).
**Nota:** ruim

A raiz de quase tudo é uma só: a reserva com CPF `111.222.333-44` e a de empresa com CNPJ `12.345.678/0001-90` respondem 500 (`measures/behavior-service.log:67,123`), e por isso falham em cascata a criação, o preço, a confirmação, o e-mail, o check-in, o check-out, os cancelamentos, os dois relatórios, os dados depois de reiniciar e os 17 pontos de CPF e de empresa do cenário. Para o dono da pousada: nenhum brasileiro e nenhuma empresa consegue reservar; só o estrangeiro. Esse caminho (passaporte e país) passou inteiro: booking receipt em inglês, `BRL 2,783.52`, datas ISO, "1 night", linha Discount, e as quatro recusas de identificação (15 de 32).

Chamando a API à mão com um CPF só de dígitos, o resto funciona: 599,76 na alta temporada, pago igual ao total, e-mail com o comprovante, check-out com o comprovante, devolução integral no STANDARD e zero no NON_REFUNDABLE, ocupação `total,1/5`, financeiro contando a não reembolsável cancelada. Mesmo assim ficaram defeitos que não dependem do CPF:

- Corpo que não é JSON responde 500 em vez de 400 `INVALID_REQUEST` (`Application.java:84-88`).
- Segunda via de reserva pendente responde 400 `INVALID_REQUEST` em vez de 409 `OPERATION_NOT_ALLOWED` (`ReservationController.java:97-99`), e uma reserva cancelada continua ganhando segunda via, porque o `amountPaid` não zera no cancelamento.
- O financeiro imprime `397.7` em vez de `397.70` (`ReportService.java:74`); a conferência do roteiro com 599.76 não pegaria isso, mas qualquer valor terminado em zero sai errado.
- Duas reservas ao mesmo tempo: no roteiro as duas deram 500 (pelo CPF); à mão, 4 de 5 rodadas criaram as duas. Defeito do banco, contado lá.
- O dinheiro no JSON vai como `0.0` e `397.7` (double), não com 2 casas como o texto exemplifica (`ReservationDTO.java:46-51`); é um ponto fraco, mas um número JSON não guarda casas, e não o pesei.

Não pesou: o número do quarto vai como texto (`"101"`) e o roteiro esperava número; o texto não fechou isso.

## Dúvidas

- A forma do Factory Method usada foi a fábrica concentrada (um `switch` exaustivo sobre a interface selada em `DocumentService.java:23-29`), não a forma clássica com subclasses nem o método polimórfico no tipo de hóspede. Como a fábrica devolve só o texto e o título fica num segundo `switch` (`DocumentDTO.java:29-33`), há uma leitura em que a escolha está parcialmente fora dela; dei `acerto` porque os três lugares pedem o documento à fábrica e nenhum decide o tipo por conta própria.
- O texto não diz se o CPF e o CNPJ chegam formatados ou só com dígitos; os exemplos dele vêm formatados, e o sistema aceita qualquer formato até bater na coluna. Se o site mandasse só dígitos, o sistema funcionaria, mas devolveria `guestCpf` cru e o documento formatado.
- `number` do quarto como texto ou número no JSON: o texto não fecha.
