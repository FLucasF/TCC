# Revisão: construção, cenário 05

## Visão geral

O projeto tem seis pacotes: `handlers` (as rotas do Javalin), `repositories` (o JDBC), `models` (objetos de dados
com campos públicos), `documents` (os três documentos e a fábrica), `utils` (o `PriceCalculator`) e a raiz (`Main` e
um `Database` estático com o pool Hikari e os `CREATE TABLE`). Uma requisição entra num método de handler. Ele lê o
corpo como `Map`, valida, calcula o preço, aplica a regra da situação da reserva e chama o repositório, que pega a
conexão do `Database` estático. Os dados ficam no PostgreSQL (tabelas `rooms`, `reservations` e `messages`). **O
problema mais sério é que o sistema não sobe.** As rotas usam a sintaxe `/reservations/:id` (`Main.java:20-25`), que
o Javalin 6.7.0 recusa ao registrar a rota: `IllegalArgumentException: Path '/reservations/:id' invalid - Javalin 4
switched from ':param' to '{param}'` em `Main.java:20`. O processo morre antes do `start`, então nenhuma rota atende,
nem `GET /rooms`. Os testes do agente não pegam isso porque nenhum deles sobe o servidor. Lendo o código, aparecem
ainda outros defeitos que o roteiro não chegou a exercitar: o dinheiro em `double`, o comprovante que escreveria
"R$ 779,8" e o *booking receipt* sem separador de milhar.

## Arquitetura

**Números:** 6 pacotes, 9 dependências entre eles, 1 ciclo (`com.pousadamareaalta` → `handlers` → `repositories` →
`com.pousadamareaalta`). `ReservationHandler` tem 428 linhas, mais que o dobro da segunda maior classe (o
`ReservationRepository`, com 195).
**Nota:** com problemas

- **O acesso ao banco está isolado nos repositórios.** O SQL fica em `repositories/*` e os handlers não têm JDBC (o
  jdeps confirma). Os documentos não conhecem o Javalin nem o JDBC.
- **Não há camada de regras.** As regras de negócio moram dentro do handler HTTP:
  - a validação da identificação: `handlers/ReservationHandler.java:23-57`;
  - a capacidade e a disponibilidade: `ReservationHandler.java:101-120`;
  - o cálculo do preço: `ReservationHandler.java:123-126`;
  - as transições de situação: `ReservationHandler.java:185-193`, `:222-245`, `:271-280`, `:304-313`;
  - a regra da devolução: `ReservationHandler.java:241-245`;
  - o e-mail de confirmação: `ReservationHandler.java:384-402`.

  O relatório de ocupação também decide no handler quais situações contam como pagas
  (`handlers/ReportHandler.java:37`). Assim, nenhuma regra pode ser usada nem testada sem um `Context` do Javalin.
- **Não há montagem num lugar só.** Cada handler cria os seus repositórios por dentro
  (`ReservationHandler.java:14-16`, `ReportHandler.java:11-12`). Os repositórios usam o `Database` estático global
  (`Database.java:10`, `:98`; `ReservationRepository.java:20`). O `Main` cria uma instância nova de
  `ReservationHandler` para cada rota (`Main.java:19-25`).
- **O ciclo vem desse `static`.** `repositories` depende da raiz por causa do `Database`, e a raiz depende de
  `handlers`.

As camadas existem só pela metade: os dados estão separados, as regras não.

## Design de baixo nível

**Números:** 3 tipos de documento, os 3 na estrutura. A escolha fica num só lugar
(`documents/DocumentGeneratorFactory.java:6-15`). Os 3 lugares que emitem o documento (o e-mail, o check-out e a
segunda via) pedem o documento à fábrica, por 2 chamadas (`ReservationHandler.java:361` e `:386`). Nenhum lugar
monta um documento à mão.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O agente usou uma fábrica concentrada. `DocumentGeneratorFactory.create` escolhe pelo tipo de
identificação e devolve `CpfDocument`, `CnpjDocument` ou `PassportDocument`, todos implementações de
`DocumentGenerator` (`documents/DocumentGenerator.java:6-9`).
- **O que aproxima do pattern:**
  - o check-out e a segunda via passam por `generateDocument` (`ReservationHandler.java:316`, `:349`, `:360-378`);
  - o e-mail passa por `sendConfirmationEmail` (`ReservationHandler.java:384-387`);
  - nenhum desses lugares pergunta qual é o tipo de hóspede;
  - uma agência de turismo seria uma classe nova e um ramo novo na fábrica.
- **O que afasta:**
  - a fábrica decide por campos nulos (`DocumentGeneratorFactory.java:7-13`) e não por um tipo de hóspede;
  - o JSON do documento é montado fora dele, com todos os campos de identificação (`ReservationHandler.java:365-376`);
  - `generateDocument` e `sendConfirmationEmail` repetem as mesmas duas linhas de criação e geração.

  Nada disso desfaz o acerto.

**O exagero.** A interface tem três implementações de verdade, e não há builder nem camada que só repasse. É
contenção.

**O resto do código.** Os defeitos estão espalhados:
- **Dinheiro em `double` em todo o sistema.** Isso vale para `models/Reservation.java:20-24` e
  `models/Room.java:7`. Também vale para `PriceCalculator`, que soma em `double` (`utils/PriceCalculator.java:37-46`)
  e arredonda com `new BigDecimal(double)` (`:56`), ou seja, com o erro binário, quando o texto pede "meio para o
  par" exato. O repositório grava `new BigDecimal(reservation.subtotal)` (`ReservationRepository.java:36-40`). O
  relatório financeiro soma em `double` e formata com `String.format("%.2f")`, que depende da localidade
  (`ReportHandler.java:96-108`).
- **A formatação do dinheiro está errada nos documentos.** `NumberFormat.getInstance(pt-BR)` não fixa 2 casas: 779,80
  sai "779,8" (conferido no jshell). Isso aparece em `documents/CpfDocument.java:49-52` e
  `documents/CnpjDocument.java:53-56`. Em `PassportDocument.java:43-45`, `%.2f` sai "1234.56" e não "1,234.56".
- **Muita duplicação.** O objeto anônimo de erro aparece repetido cerca de 25 vezes nos handlers (por exemplo,
  `ReservationHandler.java:36-39`, `:156-159`, `:178-181`, ...). `getNightsLabel`, `formatDate` e `formatMoney` estão
  copiados nas três classes de documento.
- **Situações e planos em texto solto.** Não há `enum` para a situação nem para o plano (`"PENDING"`, `"CONFIRMED"`
  e outros em `ReservationHandler.java:185`, `:222`, `:241`). Nada valida o `ratePlan`: um valor qualquer vira
  STANDARD (`PriceCalculator.java:49-53`).
- **Métodos longos.** `create` tem cerca de 130 linhas (`ReservationHandler.java:19-148`).
- **Estado sem controle.** Os modelos têm campos públicos mutáveis, e o handler muda a reserva por fora
  (`ReservationHandler.java:193-194`, `:239`).
- **Erros mal tratados.** Há `catch (Exception)` genérico devolvendo 500 com `e.getMessage()` em todas as rotas
  (`ReservationHandler.java:141-147`). Um corpo que não é JSON e a falta de `guestName` acabam em 500, e não em 400.
- **Código morto.** `formatDateForJson` não faz nada além de `toString()` (`ReservationHandler.java:380-382`).

## Testes

**Números:** 9 testes rodaram, 0 falharam, 0 pulados; 9 métodos escritos. Cobertura de linhas de 3,8% (29 de 770) e
de ramos de 6,9% (12 de 174). Os pacotes `handlers`, `repositories` e a raiz têm 0% de linhas.
**Nota:** ruim

**O que os testes conferem.** Só o `PriceCalculator` tem testes com valores
(`src/test/java/com/pousadamareaalta/PriceCalculatorTest.java:11-52`): a alta temporada de julho (599,76), a baixa
(499,80), 15 de dezembro e o desconto de 15% (116,97). Mesmo esses usam tolerância de 0,01 (`:17`, `:42`), então um
erro de um centavo no arredondamento passaria.

**Os testes que não conferem nada.** `IntegrationTest` não é de integração:
- `:8-15` só fazem `assertNotNull(new ...)`;
- `:18-27` conferem só o título dos três documentos;
- `:30-35` testam uma conta local, sem chamar o código do projeto.

**O que ficou sem teste:**
- a parte nova do cenário inteira: o texto da fatura, o ISS de 5% (38,99), o *booking receipt* em inglês, a
  escolha da fábrica e a recusa de nenhuma, duas ou uma identificação incompleta;
- as situações da reserva, o cancelamento e a devolução, os códigos de erro, o e-mail e os relatórios.

**A pergunta da mutação.** Trocar 0.05 por 0.5 em `CnpjDocument.java:15`, trocar a ordem dos ramos da fábrica ou
apagar a linha "Discount:" do `PassportDocument` não derrubaria nenhum teste.

**O que isso deixou passar.** Nenhum teste sobe a API. Por isso o `mvn verify` passa, e o serviço morre ao subir.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. As 6 colunas de dinheiro são
`numeric(10,2)` e as datas são `date`. As conferências acusaram:
- a chave estrangeira `reservations(room_number) → rooms` sem índice;
- `status`, `rate_plan`, `type` e `channel` sem CHECK;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- 0 transações explícitas e 0 `FOR UPDATE`;
- **o serviço não subiu contra o banco vazio.**

**Nota:** ruim

**O serviço não sobe.** O esquema chega a ser criado, porque `Database.init` roda antes das rotas (`Main.java:15`),
mas o processo morre logo depois, no registro de `/reservations/:id` (`Main.java:20`). Pelo critério da coluna, isso
é sério: nada é gravado nem lido.

**O resto, lido no código:**
- **O esquema não tem migrações.** Ele é feito com `CREATE TABLE IF NOT EXISTS` no Java (`Database.java:29-72`), que
  não sabe acrescentar uma coluna numa tabela existente.
- **Os quartos são semeados só se a tabela estiver vazia** (`Database.java:80-92`).
- **Nada no banco impede duas reservas no mesmo quarto e na mesma noite.** O handler consulta os conflitos
  (`ReservationRepository.java:119-141`) e depois insere (`ReservationHandler.java:137`). As duas operações rodam em
  conexões separadas, com autocommit, sem transação, sem trava e sem restrição EXCLUDE. Duas reservas simultâneas
  podem passar as duas.
- **Os valores passam por `double`.** O banco tem o tipo certo, mas o código lê e grava em `double`
  (`ReservationRepository.java:36-40`, `:184-191`) e usa `new BigDecimal(double)`. O `numeric(10,2)` arredonda na
  gravação, mas o valor já passou por ponto flutuante.
- **O que está certo:** o SQL é todo com parâmetros (`?`), as conexões são devolvidas por try-with-resources e há
  pool (Hikari, `Database.java:13-20`).

## Comportamento

**Números:** 0 conferências rodaram. O roteiro `check_05.sh` registrou "não deu para testar (o serviço não subiu
contra um banco vazio)". A exceção foi lançada em `Main.java:20` (`Path '/reservations/:id' invalid`).
**Nota:** ruim

Para o dono da pousada, o sistema não funciona: o `java -jar` termina com erro antes de abrir a porta. O site não
consegue listar quartos, criar, confirmar nem cancelar reservas. A recepção não faz check-in nem check-out e o
contador não tira relatórios. Nenhum documento (comprovante, fatura ou *booking receipt*) chega a ser emitido.

Se as rotas fossem corrigidas, a leitura do código mostra que outras conferências ainda falhariam. Elas não foram
medidas e não mudam a nota, que já é `ruim`:
- **O valor dos documentos.** O comprovante e a fatura escreveriam "R$ 779,8" em vez de "R$ 779,80"
  (`CpfDocument.java:49-52`). O *booking receipt* escreveria "BRL 1234.56" em vez de "BRL 1,234.56"
  (`PassportDocument.java:43-45`). O JSON mandaria 779.8, sem as 2 casas pedidas.
- **A segunda via de reserva cancelada.** Ela seria emitida, porque só PENDING é recusado
  (`ReservationHandler.java:340`), e o texto diz que a segunda via só existe para reserva confirmada, com check-in ou
  com check-out.
- **As recusas que viram 500.** Um corpo que não é JSON e a falta de `guestName`, `guestEmail` ou `ratePlan` dariam
  500 (ou nenhuma recusa, no caso do `ratePlan`), e não 400 com INVALID_REQUEST.
- **As reservas simultâneas.** Duas reservas ao mesmo tempo poderiam ocupar o mesmo quarto, como visto na coluna do
  banco.

## Dúvidas

O roteiro não chegou a rodar nenhuma conferência, porque o serviço não sobe. Por isso, os defeitos listados para depois de
um eventual conserto das rotas (a formatação do dinheiro, a segunda via de reserva cancelada, as recusas que viram
500, a corrida entre reservas simultâneas) vêm só da leitura do código e de uma conferência pontual no jshell da
formatação `NumberFormat`/`%.2f`, e não de chamadas à API. Quanto ao pattern, a forma usada é a fábrica concentrada
(uma classe com um método estático que escolhe pelo campo de identificação preenchido), e não o Factory Method
clássico com criador sobrescrito; a referência aceita as duas como acerto.
