# Revisão: construção, cenário 05

## Visão geral

O projeto (Java 21, Javalin 6.7.0, JDBC com HikariCP, sem Flyway) tem sete pacotes: `api` (controladores Javalin), `service` (regras, documentos, e-mail, relatórios), `repository` (JDBC), `domain`, `exception`, `util` (preço e temporada) e a raiz com a montagem (`Application.java:24-88`). Uma requisição entra numa rota de `Application.java:73-83`, passa pelo controlador (`ReservationController`), que valida o corpo e monta a identificação do hóspede (`ReservationController.java:76-116`), segue para o `ReservationService` e chega aos repositórios, que criam as próprias tabelas com `CREATE TABLE IF NOT EXISTS` no construtor. A escolha do documento por tipo de hóspede está concentrada num lugar só (`DocumentService.java:17-23`), e os três pontos que emitem o documento usam esse lugar. Os problemas sérios estão nos dados e no comportamento. A coluna do CPF é `VARCHAR(11)` e a do CNPJ é `VARCHAR(14)` (`ReservationRepository.java:29-30`). Quando o CPF ou o CNPJ chega escrito como o texto mostra (111.222.333-44, 12.345.678/0001-90), a criação da reserva cai em 500. Por isso o roteiro passou em 29 de 83 conferências. Nada impede duas reservas simultâneas do mesmo quarto: num teste à mão, 15 de 15 rodadas criaram reserva dupla. E o valor em reais sai como "R$" + espaço não separável (U+00A0), e não com o espaço comum que o texto mostra.

## Arquitetura

**Números:** 7 pacotes, 14 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Só a raiz e o `api` usam Javalin; só o `repository` usa JDBC; `service`, `domain` e `util` não usam nenhuma biblioteca de fora. Maior classe: `ReservationRepository` (311 linhas).
**Nota:** bom

- **Borda HTTP fina:** os handlers leem a requisição, chamam o serviço e escrevem a resposta (`ReservationController.java:18-74`). Os erros viram HTTP num lugar só, com `app.exception` (`Application.java:52-71`). No controlador ficam a validação do corpo e as regras de "só um jeito de identificar" (`ReservationController.java:76-106`), o que é validação de entrada e não regra de negócio.
- **Regras sem Javalin e sem SQL:** preço em `PriceCalculator`/`SeasonUtil`, situações da reserva em `Reservation.java:51-97`, cancelamento e devolução em `Reservation.java:56-63`, orquestração em `ReservationService`.
- **Dados isolados** nos três repositórios.
- **Montagem num lugar só:** `Application.java:25-40`, com injeção pelo construtor.
- **Pontos menores:**
  - `RoomController` e `MessageController` vão direto ao repositório (`RoomController.java:19`, `MessageController.java:19`), o que dá a dependência `api -> repository`. São leituras simples e não pesam.
  - Os repositórios criam o esquema no construtor (`ReservationRepository.java:16-55`), o que mistura montagem do banco com acesso. Isso conta na coluna do banco.

## Design de baixo nível

**Números:** 3 tipos de documento; 1 lugar decide qual criar (`DocumentService.java:17-23`); 3 lugares pedem o documento a ele (`ReservationService.java:86`, `:125`, `:136`); 0 lugares criam o documento à mão. O `switch` sobre o tipo de hóspede aparece em mais 3 lugares, mas para outra coisa (montar o JSON e gravar/ler colunas): `ReservationController.java:125`, `ReservationRepository.java:119`, `ReservationRepository.java:284`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A forma usada é a fábrica concentrada.
- `GuestIdentification` é uma `sealed interface` com três `record`s (`GuestIdentification.java:3`).
- `DocumentService.generateDocument` escolhe o documento com um `switch` exaustivo sobre esse tipo (`DocumentService.java:17-23`).
- Confirmação (e-mail), check-out e segunda via chamam esse método (`ReservationService.java:86`, `:125`, `:136`). O e-mail recebe o `text` do documento pronto (`NotificationService.java:14-16`).

Para uma agência de turismo bastaria um novo `record` e um novo `case`, e o compilador apontaria os `switch` que faltam. O que afasta, sem mudar a classe:
- os outros `switch` sobre o tipo de hóspede (no JSON e na gravação) também teriam de mudar;
- o documento é um `record` único com os cinco campos de identificação passados por posição, com `null` nos que não se aplicam (`DocumentService.java:40-58`, `:80-98`, `:117-135`).

**O exagero.** Não há estrutura sobrando: a interface tem três implementações de verdade e não há fábrica, builder ou camada que só repassa. O `getType()` da interface (`GuestIdentification.java:4`) nunca é chamado. É código morto, não estrutura a mais.

**O resto do código.**
- **Duplicação nos documentos:** `buildComprovante` e `buildFatura` são quase a mesma função copiada (`DocumentService.java:138-157` e `:159-181`). Os três `build*` recebem de 11 a 13 parâmetros soltos em vez da reserva (`DocumentService.java:138-140`, `:159-162`, `:183-186`).
- **Formato do dinheiro:** é feito com `NumberFormat.getCurrencyInstance(pt_BR)` (`DocumentService.java:206-209`). No Java 21 isso gera "R$" + espaço não separável (U+00A0), e não o "R$ 779,80" do texto. Os testes do agente usam `contains("779,80")` e não pegam isso.
- **Escala do zero:** o `BigDecimal.ZERO` sem escala vai para o JSON como `0` em vez de `0.00`. Acontece no `amountPaid` da reserva recém-criada (`ReservationService.java:64`), no `refund` da cancelada sem devolução (`Reservation.java:61`) e no total vazio do financeiro (`ReportService.java:54`).
- **Erros:**
  - As falhas de SQL viram `RuntimeException` genérica (`ReservationRepository.java:87` e semelhantes), ou seja, 500.
  - O corpo que não é JSON não é tratado (`ReservationController.java:19`) e responde 500.
  - O tratador genérico de `IllegalArgumentException` (`Application.java:70`) cobre `RatePlan.valueOf` e `Long.parseLong`, mas de forma acidental.
- **Repetição no repositório:** a lista de colunas do `SELECT` aparece 4 vezes (`ReservationRepository.java:158`, `:184`, `:217`, `:249`).
- **Código morto:** `isActive` e `occupiesNight` nunca são usados (`Reservation.java:89`, `:99`).
- **O que está bem:** `BigDecimal` com `HALF_EVEN` em cada passo (`PriceCalculator.java:16-49`), `LocalDate`, `enum`s para situação, plano e tipo de quarto, e transições de situação com guarda.

## Testes

**Números:** 16 testes rodaram, 0 falhas, 0 erros, 0 pulados; 16 métodos escritos em 2 arquivos (`PriceCalculatorTest`, 11; `DocumentServiceTest`, 5). Cobertura: 24,3% das linhas (217/892) e 14,3% dos ramos (29/203). Por pacote: `api`, `repository` e a raiz com 0%; `util` com 94%; `service` com 55%. Nenhum teste pela API ou contra o banco. O `pom.xml` declara Testcontainers, mas nada o usa.
**Nota:** com problemas

**O que está testado com valores:**
- a temporada nas bordas (14/12 fora, 15/12 dentro, 01/03 fora; `PriceCalculatorTest.java:25-32`);
- a noite de alta temporada a 299,88 e as duas noites a 599,76 (`PriceCalculatorTest.java:42-72`);
- o desconto 89,96 com `HALF_EVEN` (`:75-81`);
- o ISS de 38,99 (`:101-107`);
- os três documentos, com título, campos nulos e trechos do texto (`DocumentServiceTest.java:15-94`), mais "1 diária" e a linha de desconto (`:96-136`).

**Pela pergunta da mutação na parte nova:**
- trocar o ISS de 0.05 por outro valor quebra um teste;
- mudar o separador de milhar do BRL não quebra nenhum (os valores testados não passam de mil);
- tirar a linha "Discount:" do booking receipt não quebra nenhum (não há teste de desconto em inglês);
- tirar a regra de "exatamente um jeito de identificar" (`ReservationController.java:84-105`) não quebra nenhum;
- o formato exato do texto também fica sem rede: as asserções são `contains` de pedaços (`DocumentServiceTest.java:36-40`), o que deixou passar o espaço não separável no "R$" e não confere a ordem das linhas.

**Sem nenhum teste:** as situações da reserva e as recusas 409, o cancelamento com devolução, a disponibilidade do quarto, os dois relatórios, os códigos de erro, o e-mail e a persistência. Por isso nenhum teste pegou o 500 da coluna `VARCHAR(11)`.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. Nenhuma chave estrangeira, nenhum CHECK, nenhum UNIQUE ou EXCLUDE. O esquema é criado por `CREATE TABLE` no código Java, sem migrações. Não há transação explícita nem `FOR UPDATE`. Não há SQL montado com `+` (`measures/database.md`).
**Nota:** ruim

- **Colunas estreitas demais para o dado do texto:** o CPF é `VARCHAR(11)` e o CNPJ é `VARCHAR(14)` (`ReservationRepository.java:29-30`). Com o CPF como o texto o escreve (111.222.333-44, 14 caracteres) ou o CNPJ (12.345.678/0001-90, 18 caracteres), o `INSERT` falha com "value too long for type character varying(11)" (`measures/behavior-service.log`), e a exceção vira 500. Só o passaporte passa. É o defeito que derruba a maior parte do roteiro do comportamento.
- **Nada protege contra a reserva dupla:** a disponibilidade é uma consulta seguida de um `INSERT` em conexões separadas, sem transação nem trava (`ReservationService.java:49-55` e `:67`; `ReservationRepository.java:182-213`). Não há restrição de exclusão no banco. Com CPF só de dígitos, 15 de 15 rodadas de dois pedidos simultâneos para o 301 nas mesmas noites gravaram as duas reservas. É risco direto de dado errado (dois hóspedes no mesmo quarto).
- **Esquema que não evolui:** `CREATE TABLE IF NOT EXISTS` no construtor de cada repositório (`ReservationRepository.java:22-45`, `RoomRepository.java:23-46`, `MessageRepository.java:18-36`), sem versionamento. Um campo novo (por exemplo, o da agência) não chega a um banco que já existe.
- **Sem chave estrangeira** de `reservations.room_number` para `rooms.number` (`ReservationRepository.java:26`), e sem CHECK em `status`, `rate_plan` e `type`.
- **Escritas soltas:** a confirmação grava a reserva e depois a mensagem em duas conexões independentes (`ReservationService.java:83-87`). Se a segunda falhar, a reserva fica confirmada sem e-mail.
- **O que está bem:** SQL sempre com `?`, try-with-resources, pool HikariCP, dados que sobrevivem ao reinício (conferido à mão com CPF só de dígitos).

## Comportamento

**Números:** 29 de 83 conferências passaram (`measures/behavior.md`):
- a parte comum: 12 de 46;
- os dados depois de reiniciar: 1 de 3;
- o cenário 05: 15 de 32;
- duas reservas ao mesmo tempo: 1 de 2 (nenhuma rodada criou as duas, mas as 10 deram 500/500 em vez de 201/409).

**Nota:** ruim

- **Criar uma reserva com CPF ou CNPJ no formato do texto responde 500,** por causa das colunas `VARCHAR(11)`/`VARCHAR(14)`. Daí falham, em cascata:
  - quase toda a parte comum: preço, confirmação, e-mail, check-in, check-out, cancelamentos, relatórios com dados;
  - a conferência do reinício;
  - toda a parte de CPF e de empresa do cenário (fatura, ISS, e-mail com a fatura).

  Para o dono da pousada: um hóspede brasileiro ou uma empresa não consegue reservar pelo site. Só o estrangeiro com passaporte consegue.
- **O que passou:**
  - todas as conferências do booking receipt (datas, BRL, "1 night", Discount, milhar com vírgula);
  - as quatro recusas de identificação (nenhuma, CPF e CNPJ juntos, CNPJ sem razão social, passaporte sem país);
  - as recusas de capacidade, quarto inexistente, datas, hóspedes e campos faltando.
- **Com CPF só de dígitos,** confirmei à mão que:
  - preço, confirmação, ocupação, financeiro, devolução e persistência funcionam;
  - a fatura traz "ISS retido (5%)" com 38,99.

  Ainda assim, todo valor em reais no texto (comprovante, fatura, e-mail) sai como "R$ 599,76", com espaço não separável. Uma comparação exata com o texto pedido falha.
- **Reservas simultâneas:** com CPF só de dígitos, a reserva dupla acontece sempre (15 de 15 rodadas, teste à mão). O sistema não impede dois hóspedes no mesmo quarto. O defeito pesa na coluna do banco e é citado aqui.
- **Outras falhas próprias:**
  - corpo que não é JSON responde 500, não 400;
  - `amountPaid` vem `0` (e não `0.00`) na resposta da criação, e `refund` vem `0` no cancelamento sem devolução;
  - `month=27-07` é aceito como o ano 27 (200 em vez de 400).
- **Ponto em aberto, que não pesou:** o `number` do quarto vem como texto ("201"), e o roteiro procura um número. Por isso falharam "o 201 comporta 3" e "a diária do 201". O texto não diz o tipo.

## Dúvidas

- O texto não diz se o CPF e o CNPJ chegam com ou sem pontuação. O agente supôs só dígitos: a formatação em `DocumentService.java:215-229` só formata quando o tamanho é 11 ou 14. Mas o texto mostra os dois sempre com pontuação, e a coluna estreita transforma a outra leitura num 500, não numa recusa. Por isso tratei como defeito.
- No design, o pattern é a fábrica concentrada, que a referência aceita como acerto. Pesou a favor que o `switch` é exaustivo sobre um tipo selado. Fica a ressalva de que o tipo de hóspede também é decidido por `switch` no JSON e no repositório.
