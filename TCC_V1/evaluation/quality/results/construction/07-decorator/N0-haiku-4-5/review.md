# Revisão: construção, cenário 07

## Visão geral

O agente montou um serviço Javalin em 8 pacotes (`api`, `api.dto`, `services`, `repositories`, `models`, `database`,
`utils` e a raiz com o `main`), com JSON pelo Gson e JDBC puro. Uma requisição entra num handler de
`ApiController` (324 linhas, a maior classe), que valida, consulta os repositórios diretamente para checar a
situação da reserva e só então chama um serviço (`ReservationService`, `MessageService`, `ReceiptService`,
`ReportService`), que usa os repositórios; os dados ficam em três tabelas (`rooms`, `reservations`, `messages`)
criadas por `CREATE TABLE IF NOT EXISTS` em `Database.java`. A parte comum quase toda funciona (45 de 46 no
roteiro). Os problemas mais sérios: (1) nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos
chegam juntos, e o roteiro criou as duas em 10 de 10 rodadas; (2) o relatório financeiro deixa de fora a reserva
NON_REFUNDABLE cancelada, que o texto manda contar (`ReservationRepository.java:154`); (3) as três regras de e-mail,
que são a parte do cenário, ficaram em `if`s dentro de um único método (`MessageService.java:29-50`), e a cópia
para o arquivo é gravada *antes* da original; (4) os testes só cobrem utilitários (4,2% das linhas).

## Arquitetura

**Números:** 8 pacotes, 14 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em
`com.pousada` e `com.pousada.api`; JDBC só em `database` e `repositories`; `services` não usa nem Javalin nem JDBC.
Maior classe: `ApiController` com 324 linhas.
**Nota:** com problemas

O desenho de camadas existe e as dependências apontam no sentido certo (api → services → repositories → database),
sem ciclo. A montagem fica num lugar só, `Application.java:32-63`, com injeção por construtor. Os serviços não
conhecem o `Context` nem montam SQL.

O defeito é a borda HTTP grossa. `ApiController` depende de `repositories` diretamente e repete regras de negócio
que também estão nos serviços:
- a validação de criação (datas, hóspedes, capacidade, disponibilidade) em `ApiController.java:91-113`, duplicada em
  `ReservationService.java:24-38`;
- a regra de transição de situação em cada handler (`ApiController.java:161`, `193`, `218`, `246`, `270-272`),
  duplicada em `ReservationService.java:71`, `88`, `104`, `121`;
- a orquestração da confirmação (pagar, gerar comprovante, mandar e-mail) dentro do handler
  (`ApiController.java:167-173`), e não num serviço.

Cada nova regra de situação vai ter de ser mexida em dois lugares. É uma mistura localizada na borda, não a ausência
de camadas.

## Design de baixo nível

**Números:** dos 3 comportamentos de envio que o texto pede (bloqueio, cópia, modo de teste), 0 numa estrutura
própria e 3 em `if`s dentro de `MessageService.sendMessage` (`MessageService.java:32-47`). Não há interface de envio
nem embrulho.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern (Decorator).** O texto pede três regras em volta do mesmo envio, que se combinam conforme o modo, e
avisa que mais vão chegar ("de madrugada", "limite por dia", "cada regra pode valer sempre, só no teste ou só fora
dele", "a segunda pousada não quer a cópia"). O agente pôs tudo num só método: `if (!EmailValidator.isValid(...))`
(`MessageService.java:32`), `if (testMode)` (`:38`) e `else if (archiveEnabled)` (`:43`). A combinação por modo
está escrita dentro do envio (a cópia é o `else` do modo de teste), e não decidida na montagem. Para acrescentar
"não mandar de madrugada só fora do teste", alguém precisa reescrever esse encadeamento. Pela referência, é
**omissão**. O `boolean archiveEnabled` passado no construtor (`Application.java:40`) até aponta para a configuração
por instalação, mas como um flag lido dentro do método, não como uma peça. Além disso, a ordem está errada: a cópia
é gravada antes da original (`MessageService.java:44-46` antes de `:49-50`), e o texto pede "logo depois da original".
O flag de desligar a cópia está invertido: `Application.java:30` só desliga a cópia com
`HOTEL_ARCHIVE_DISABLED=false`. Com `=true`, que é o que o README e o `.env.example` mandam usar, a cópia continua
saindo (conferi pela API).

**O exagero:** não há interface com uma implementação só, nem fábrica ou camada que só repassa. Há duplicação de
modelos (`Receipt` × `ReceiptResponse`, `Reservation` × `ReservationResponse`, com o `ApiMapper` copiando campo a
campo), mas isso é a separação comum entre o domínio e o JSON, não estrutura sem necessidade. **contenção**.

**O resto do código:**
- Texto solto no lugar de tipos: a situação e o plano são `String` em todo lugar (`ReservationService.java:59`,
  `75`, `127-133`; `ApiController.java:161`, `270-272`), sem `enum`. Por isso `ratePlan: "XYZ"` é aceito com 201 e
  cobrado como STANDARD (conferi pela API), e um erro de digitação numa comparação passaria em silêncio.
- Duplicação das regras entre o controller e o serviço (descrita na arquitetura).
- Erros: `catch (Exception e)` em `ApiController.java:131-134` transforma qualquer falha (até um erro de banco) em
  400 "Invalid date format"; os serviços devolvem `null` quando não acham o quarto ou a reserva
  (`ReservationService.java:26`, `68`, `85`, `101`, `117`); `throws Exception` em todas as assinaturas; `Long.valueOf` sem
  tratamento (`ApiController.java:138`, `152` etc.), e um id não numérico dá 500.
- Positivo: dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`Money.java:7-8`, `ReservationService.java:140-161`),
  datas em `LocalDate`, métodos curtos e nomes claros.

São defeitos localizados num código pequeno e legível, por isso `com problemas`, e não `ruim`.

## Testes

**Números:** 20 testes rodaram, 0 falharam, 0 pulados; 20 métodos de teste escritos. Cobertura: linhas 39 de 933
(4,2%), ramos 34 de 192 (17,7%). Só `com.pousada.utils` tem cobertura (39/60 linhas); `services`, `repositories`,
`api` e `database` têm 0% (`measures/tests.md`). Nenhum teste pela API, contra o banco ou com mocks.
**Nota:** ruim

Os quatro arquivos testam só funções utilitárias, com valores: a temporada (`DateUtilTest.java:9-33`), o
arredondamento e os 15% (`MoneyTest.java:9-32`, com 299.88 e 89.96), a formatação do CPF e o validador de e-mail
(`EmailValidatorTest.java:9-49`, que cobre "ana@gmail", "@gmail.com", dois @). Mas nenhuma regra de negócio é
exercitada onde ela mora: o preço da reserva somado noite a noite, o desconto aplicado na reserva, as transições de
situação, o cancelamento e a devolução, os dois relatórios, os códigos de erro e, sobretudo, a parte do cenário (o
bloqueio gravado como BLOCKED, a cópia, o modo de teste) não têm teste algum. A pergunta da mutação na parte nova:
apagar o prefixo "[TESTE] " (`MessageService.java:42`), trocar a ordem da cópia, tirar o `else` que impede a cópia no
modo de teste (`:43`) ou deixar a mensagem inválida passar com SENT (`:33`): nenhum teste falharia. Dois defeitos
reais que o roteiro achou (a ordem da cópia, a NON_REFUNDABLE cancelada fora do financeiro) passariam por esta
suíte. Com 0% nas camadas de regra, a coluna está quase ausente.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária; dinheiro em `numeric(10,2)`
nas 6 colunas; datas em `date`. Conferências que acusaram: 1 chave estrangeira sem índice
(`reservations.room_number`); 5 colunas de situação ou tipo sem CHECK; 0 restrições que protegem uma regra
(UNIQUE/EXCLUDE/CHECK); esquema por `CREATE TABLE` no código, 0 migrações; 0 pool; 0 transações explícitas; 0
`FOR UPDATE`; 1 SQL montado com `+` (`measures/database.md`).
**Nota:** ruim

O sério é que nada protege a regra "um quarto não pode ter duas reservas na mesma noite". A disponibilidade é
consultada (`ReservationRepository.java:95-115`, chamada em `ApiController.java:109` e de novo em
`ReservationService.java:37`) e o `INSERT` vem depois, em outra conexão (`ReservationRepository.java:17-53`), sem
transação, sem trava, sem `synchronized` e sem restrição `EXCLUDE` no esquema (`Database.java:34-53`). O roteiro
mediu o efeito: em 10 de 10 rodadas de dois pedidos simultâneos, as duas reservas foram criadas. Isso é quarto
vendido duas vezes, um risco concreto de dado errado.

O resto pesa menos, mas soma:
- o esquema por `CREATE TABLE IF NOT EXISTS` (`Database.java:23-69`) não sabe evoluir uma tabela que já existe;
- não há pool, e cada chamada abre uma conexão nova com `DriverManager`, repetindo `Class.forName`
  (`Database.java:18-21`);
- a confirmação grava a reserva e depois as mensagens em conexões separadas (`ApiController.java:168-173` →
  `ReservationRepository.update` e `MessageRepository.create`): se o segundo passo falha, a reserva fica CONFIRMED
  sem o e-mail registrado;
- não há CHECK em `status` e `rate_plan`, e por isso "XYZ" foi gravado;
- o FK `room_number` não tem índice.

O SQL concatenado (`RoomRepository.java:40`) recebe um `int`, então não é injeção, só um mau hábito ao lado dos
outros repositórios, que usam `?`. Os dados sobrevivem ao reinício (3 de 3 no roteiro).

## Comportamento

**Números:** 60 de 65 conferências passaram. Parte comum: 45 de 46. Dados depois de reiniciar: 3 de 3. Cenário no
modo normal: 6 de 9. Cenário no modo de teste: 5 de 5. Reservas simultâneas: 1 de 2 (`measures/behavior.md`).
**Nota:** com problemas

O que falhou e o que quer dizer para o dono:
- **Financeiro sem a NON_REFUNDABLE cancelada** (parte comum): o relatório de março/2029 veio `total,0.00` em vez de
  trazer a linha `...,CANCELLED,212.42`. A consulta descarta toda reserva cancelada
  (`ReservationRepository.java:154`, `status != 'CANCELLED'`), mas o texto diz que a não reembolsável cancelada
  "continua contando". O contador vê menos dinheiro do que a pousada guardou.
- **A cópia vem antes da original** (3 conferências do cenário, um defeito só): a lista mostra a mensagem para
  `arquivo@...` com "[Cópia]" e só depois a do hóspede (`MessageService.java:44-50`). O texto e o pessoal do site
  pediram "logo depois da original". O conteúdo da cópia está certo (mesmo texto, SENT). O bloqueio dos 4 endereços
  inválidos e o modo de teste passaram por inteiro.
- **Reservas simultâneas:** as duas criadas em 10 de 10 rodadas. O defeito pesa na coluna do banco.

Achados fora do roteiro, conferidos chamando a API:
- `ratePlan` desconhecido ("XYZ") é aceito com 201;
- um id não numérico em `/reservations/abc/confirmation` dá 500, e não um erro no formato `{error, message}`;
- o `discount` de uma reserva STANDARD sai `0` e o `refund` do cancelamento sai `0`, e não `0.00` como o texto pede
  (só na resposta da criação e do cancelamento; lido do banco vem com 2 casas);
- `refund` some do JSON enquanto é nulo, em vez de vir `null` (o Gson omite nulos);
- `HOTEL_ARCHIVE_DISABLED=true` não desliga a cópia.

A parte comum funciona, com um erro de valor no financeiro; o cenário funciona em parte.

## Dúvidas

- Na nota do design de baixo nível, fiquei entre `com problemas` e `ruim`: os defeitos (status em `String`,
  duplicação controller × serviço, `catch (Exception)` genérico, `null` como retorno) aparecem em vários arquivos,
  mas o código é pequeno e legível. Fiquei com `com problemas`.
- As reservas simultâneas aparecem no roteiro do comportamento, mas eu pesei a falha só na coluna do banco, como a
  skill de comportamento orienta. Se ela pesasse também no comportamento, a nota de lá continuaria
  `com problemas` (a skill admite essa nota quando as reservas simultâneas falham).
