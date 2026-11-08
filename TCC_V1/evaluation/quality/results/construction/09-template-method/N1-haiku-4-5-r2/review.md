# Revisão: construção, cenário 09

## Visão geral

O projeto tem sete pacotes (`handler`, `service`, `repository`, `infrastructure`, `model`, `util` e a raiz). Uma
requisição chega numa rota do Javalin (`Application.java:22-36`) e passa para um handler. Os handlers de reserva chamam
o `ReservationService`, que usa os repositórios JDBC. O `ReportHandler`, por outro lado, fala direto com os
repositórios e monta os quatro CSV ali dentro. Os dados ficam no PostgreSQL, num esquema criado com
`CREATE TABLE IF NOT EXISTS` em `Database.java:33-72`.

Há dois defeitos graves:

- **Dados apagados ao subir.** Toda vez que o serviço sobe, ele roda `DELETE FROM reservations WHERE status != 'CANCELLED'`
  e depois `DELETE FROM rooms` (`Database.java:74-80`). Com isso, todas as reservas ativas somem num reinício. Se
  existir alguma reserva cancelada, o serviço nem sobe: o `DELETE FROM rooms` viola a chave estrangeira. Conferi isso à
  mão: a reserva confirmada foi apagada e o serviço caiu com `PSQLException ... reservations_room_number_fkey`.
- **A confirmação exige um campo que o texto não pede.** Ela só funciona se o corpo trouxer `amountPaid`
  (`ReservationHandler.java:85-86`). Sem esse campo, responde 500. Pelo texto, o valor pago é o total da reserva.
  Por isso, nada chega a ser pago no roteiro: não há e-mail, check-in, check-out nem relatório com linhas.

Além disso, os quatro relatórios estão escritos inteiros, um em cada método, sem nenhum esqueleto em comum.

## Arquitetura

**Números:** 7 pacotes, 14 dependências entre eles, nenhum ciclo. Só `handler` e a raiz usam o Javalin, e só `repository` e `infrastructure` usam o JDBC. As maiores classes são `ReservationHandler` (276 linhas) e `ReportHandler` (203).
**Nota:** com problemas

Pontos a favor:

- As regras do `ReservationService` não conhecem o Javalin nem o SQL.
- O acesso ao banco fica nos repositórios.
- As dependências não formam ciclo.

Defeitos:

- **Regra de negócio dentro dos handlers.** Os quatro relatórios (seleção, filtro de reserva paga, cálculo da taxa de
  turismo, agrupamento por plano, total) moram no handler HTTP:
  - o handler chama os repositórios direto (`ReportHandler.java:24-25`, `:45`, `:79`);
  - ele calcula a taxa com R$ 4,50 fixo no código (`ReportHandler.java:122-129`);
  - ele calcula o "dinheiro que ficou" (`ReportHandler.java:87`, `:177-178`).
  - Outras regras também ficaram no handler: a de quais situações permitem a segunda via, comparada por texto com
    regex (`ReservationHandler.java:155`); a de datas, saída depois da entrada (`ReservationHandler.java:45`); e a
    escolha do código HTTP pelo código de erro (`ReservationHandler.java:56-61`).
- **Cada classe cria as próprias dependências.** A montagem não fica num lugar só: o `main` passa o `Database` para
  todo mundo, e cada classe faz o seu `new`:
  - o handler cria o serviço e o repositório (`ReservationHandler.java:21-24`);
  - o serviço cria os três repositórios (`ReservationService.java:22-26`).
  - Com isso, o `RoomRepository` é instanciado em quatro lugares, e nada pode ser trocado num teste.

## Design de baixo nível

**Números:** 4 relatórios, 0 numa estrutura comum e 4 escritos inteiros. A linha de título está copiada 4 vezes (`ReportHandler.java:40`, `:82`, `:119`, `:166`), e a validação do parâmetro e o tratamento de erro também (`:31-34`, `:73-76`, `:110-113`, `:154-157`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede o Template Method, sem dar o nome: quatro relatórios com os mesmos passos (título,
cabeçalho, uma linha por reserva, total). Avisa também que virão outros e que "quando o jeito dos relatórios mudar,
quero que mude em todos de uma vez". O agente escreveu cada relatório como um método inteiro do `ReportHandler`
(`ReportHandler.java:28-68`, `70-105`, `107-149`, `151-197`). Cada um repete:

- a validação do parâmetro;
- o `StringBuilder`;
- a linha "Pousada Maré Alta,<nome>,<período>";
- o cabeçalho, o laço, a linha do total, o `contentType` e os mesmos `catch`.

Não há classe abstrata, nem escritor genérico, nem função que receba as partes que variam. Para pôr a linha com a data
em todos, como o dono avisou, seria preciso mexer em quatro lugares. Na definição da referência, é a **omissão**.

**O exagero.** Não há estrutura sem necessidade: nada de interfaces com uma implementação só, fábricas ou camadas que
só repassam. Por isso, **contenção**.

**O resto do código.**

- **Duplicação.** A montagem do comprovante em JSON está copiada, linha a linha, no check-out e na segunda via
  (`ReservationHandler.java:128-142` e `163-177`). Nos dois lugares, o comprovante em texto é calculado e jogado fora
  (`:126`, `:161`). A regra de "reserva paga" aparece em três formas: um regex (`ReservationHandler.java:155`), uma
  comparação por texto (`ReportHandler.java:199-202`) e um SQL (`ReservationRepository.java:148`). A conta de
  "pago menos devolvido" está repetida (`ReportHandler.java:87`, `:177-178`).
- **Erros.**
  - A falta de campo vira `0` ou `null` e depois um `if` com oito condições (`ReservationHandler.java:39-43`,
    `:210-225`). Uma data fora do formato vira `null` em `parseDate` (`:227-234`) e é recusada como "campo faltando".
  - O `RatePlan.valueOf` de um texto inválido dá 500 (`ReservationHandler.java:50`). Conferi: `ratePlan: "XYZ"` volta
    `INTERNAL_ERROR`.
  - O `body.get("amountPaid").toString()` dá `NullPointerException` (`:86`).
  - Os `catch (Exception)` devolvem 500 com a mensagem interna da exceção (`ReservationHandler.java:206-208`).
  - Todo `ValidationException` vira 409 na confirmação, no check-in e no cancelamento (`:91`, `:103`, `:115`). Uma
    reserva que não existe receberia, então, 409 e não 404.
  - Hóspedes negativos viram `CAPACITY_EXCEEDED` (`ReservationService.java:36`), e não `INVALID_REQUEST`.
- **Tipos.** O `BigDecimal` está certo no preço (`PriceCalculator.java:23-54`). Mas `BigDecimal.valueOf(1.20)` e
  `valueOf(0.15)` vêm de `double` (`PriceCalculator.java:9-10`). E `BigDecimal.ZERO` sai sem as 2 casas
  (`PriceCalculator.java:45`, `Reservation.java:42`, `ReservationService.java:89`): o JSON mostra `"discount":0`.
- **Estado.** A entidade `Reservation` tem setter público para tudo (`Reservation.java:106-172`). Os DTOs têm campos
  públicos do tipo `Object` (`ReservationHandler.java:236-252`).

## Testes

**Números:** 17 testes rodaram, 0 falharam; 17 métodos escritos. A cobertura é de 27 de 761 linhas (3,5%) e de 16 de 168 ramos (9,5%). Os pacotes `handler`, `service`, `repository` e `infrastructure` têm 0%. Nenhum teste passa pela API ou pelo banco.
**Nota:** ruim

Os testes só exercitam o `PriceCalculator`, com valores: a diária na alta temporada, 299,88
(`PriceCalculatorTest.java:22-27`); o desconto, 89,96 (`:30-34`); os limites da temporada
(`ReservationServiceTest.java:49-89`); e o meio para o par (`RoundingTest.java:11-28`).

- **O `ReservationServiceTest` não testa o serviço.** Apesar do nome, ele só chama o `PriceCalculator` e repete testes
  que já existem: o desconto aparece em `ReservationServiceTest.java:24-30` e em `PriceCalculatorTest.java:30-34`.
- **Nenhuma regra além do preço tem teste.** Ficaram sem nenhum teste: as transições de situação, o cancelamento com e
  sem devolução, a recusa de quarto ocupado, a capacidade, os códigos de erro, o e-mail, o comprovante e os quatro
  relatórios. Isso inclui toda a parte nova do cenário (a linha de título, a taxa de turismo, os planos).
- **A pergunta da mutação.** O teste não pegaria nenhuma destas mudanças:
  - trocar 4.50 por 5.40 (`ReportHandler.java:122`);
  - tirar `CANCELLED` do filtro do financeiro;
  - trocar `<=` por `<` na ocupação (`ReservationRepository.java:121`).
- **Os dois defeitos graves também passam.** A confirmação que quebra sem `amountPaid` e o `DELETE` ao subir passam
  sem nenhum teste acusar.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. Conferências que acusaram algo: a chave estrangeira `reservations(room_number)` não tem índice; não há CHECK nas colunas de situação, plano e tipo; não há nenhuma restrição UNIQUE, EXCLUDE ou CHECK; há 0 transações e 0 `FOR UPDATE`; e 2 SQL foram montados com `+`.
**Nota:** ruim

**O que está certo.** Os tipos estão certos, o pool é o Hikari (`Database.java:20-26`), as conexões são fechadas com
try-with-resources e a maioria do SQL usa `?`.

**O defeito grave: o esquema apaga dados a cada vez que sobe.**

- `Database.java:74-76` apaga todas as reservas não canceladas.
- `Database.java:78-80` apaga os quartos para inseri-los de novo. Com uma reserva cancelada no banco, essa linha viola
  a chave estrangeira, e o serviço não sobe mais.
- O roteiro mostra "o serviço sobe de novo sobre o mesmo banco: não subiu", e a minha chamada à mão reproduziu as duas
  coisas.

O texto pede o banco "para nada se perder quando o sistema reiniciar". Pelo critério da skill, dados que somem ao
reiniciar levam a coluna a `ruim`.

**Outros defeitos.**

- O esquema é criado no código, sem migração (`Database.java:33-72`), e não sabe evoluir.
- Nada no banco impede duas reservas do mesmo quarto na mesma noite. O serviço faz a consulta e depois o insert, sem
  transação e sem trava (`ReservationService.java:40-53`). Nas reservas simultâneas, 9 de 10 rodadas criaram as duas.
- `findById` e `findByNumber` montam o SQL com `+` (`ReservationRepository.java:60`, `RoomRepository.java:35`). Os
  valores vêm tipados (`Long`/`int`, já convertidos), então não dá injeção, mas fogem do padrão com `?` usado no resto.
- O relatório de ocupação faz uma consulta por quarto (`ReportHandler.java:44-45`). Com 5 quartos isso é pouco.

## Comportamento

**Números:** 35 de 62 conferências passaram. Por parte: parte comum 28 de 46; reinício 0 de 1; cenário 09, 6 de 13; reservas simultâneas 1 de 2.
**Nota:** ruim

**O que funciona.** Os quartos, o preço com a temporada e o meio para o par (599,76; desconto de 37,48), a maior parte
das recusas, o cancelamento de pendente e a linha de título dos quatro relatórios.

**A confirmação quebra.** `POST /reservations/{id}/confirmation` sem corpo responde 500, porque o código exige
`amountPaid` (`ReservationHandler.java:86`), um campo que o texto não pede (pelo texto, o pago é o total). Daí vêm, em
cascata:

- nenhum e-mail de confirmação;
- segunda via, check-in e check-out recusados com 409;
- o "cancelar depois do check-in" acaba cancelando de fato, e a STANDARD confirmada é cancelada sem devolução;
- ocupação, financeiro, taxa de turismo e planos voltam vazios (as 7 falhas do cenário).

**Mesmo pagando à mão, os relatórios erram.** Conferi à mão, mandando `amountPaid`:

- O sistema aceita qualquer valor como pago (gravou 663,34 numa reserva de 795,40).
- Ocupação, taxa (27,00) e planos ficam certos para as reservas pagas.
- Mas o financeiro e o de planos deixam de fora a NON_REFUNDABLE cancelada, porque reusam `findPaidByMonth`, que
  exclui `CANCELLED` (`ReservationRepository.java:148`). Assim, o dono veria menos dinheiro do que ficou com a pousada.

**Outras falhas.**

- O total dos relatórios de taxa e financeiro sai com vírgulas a mais (`total,,,,,0.00`, `ReportHandler.java:97`,
  `:141`), e não como "total" e o valor. O texto pede "total" e o valor na taxa de turismo, então isso é erro. No
  financeiro, a forma ficou em aberto.
- Corpo que não é JSON dá 500.
- O dinheiro sai sem 2 casas (`"discount":0`).
- Ao reiniciar, as reservas somem ou o serviço não sobe. Esse defeito pesa no banco; aqui ele só aparece como falha.
- Duas reservas simultâneas do mesmo quarto foram criadas em 9 de 10 rodadas.

**Para o dono:** hoje, o site não consegue confirmar uma reserva do jeito combinado, e cada reinício do sistema apaga as
reservas ativas.

## Dúvidas

- No relatório financeiro, o texto diz só "no fim uma linha 'total' com a soma". O `total,,,,599.76` do agente
  (com as colunas vazias) pode ser uma leitura razoável. Já na taxa de turismo, o texto diz "'total' e o valor".
- Não sei se o `amountPaid` no corpo da confirmação foi uma decisão consciente (o README mostra esse corpo, em
  `README.md:163-166`) ou um descuido. Nos dois casos, ele contraria o texto ("o que ele pagou é o total da reserva")
  e quebra o fluxo que o site combinou.
