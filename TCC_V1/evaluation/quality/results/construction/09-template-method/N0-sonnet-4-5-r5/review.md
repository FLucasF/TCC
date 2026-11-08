# Revisão: construção, cenário 09

## Visão geral

O projeto tem cinco pacotes: `api` (um controlador só), `service` (preço, reservas, relatórios e exceções),
`repository` (interfaces e implementações JDBC), `domain` (classes de dados e enums) e a raiz, com a `App`, que monta
as peças. Uma requisição entra pela rota registrada em `App.java:48-60`, passa pelo método do `ReservationController`,
que valida a entrada e chama o serviço, e o serviço usa os repositórios JDBC, que guardam tudo no PostgreSQL em três
tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código (`repository/DatabaseInitializer.java:19-58`). As camadas
estão separadas e o fluxo comum funciona: 42 de 46 conferências da parte comum e 13 de 13 do cenário. Os problemas
mais sérios são três. Nada impede que o mesmo quarto seja reservado duas vezes para as mesmas noites: não há
transação, trava nem restrição, e em 9 de 10 rodadas simultâneas as duas reservas foram criadas. Os quatro relatórios
estão escritos inteiros, um depois do outro, sem o esqueleto comum que o texto pede (`service/ReportService.java:30-143`).
E os testes são só 8, todos do cálculo de preço, com 5,7% de cobertura de linhas.

## Arquitetura

**Números:** 5 pacotes, 9 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só
em `api` e na raiz, o JDBC só em `repository` e na raiz, e `service` e `domain` não usam nenhuma biblioteca. A maior
classe é `ReservationController`, com 299 linhas.
**Nota:** bom

As dependências seguem a direção certa: `api → service → repository → domain`, sem nada voltando. As regras ficam em
classes que não conhecem nem o HTTP nem o SQL: a temporada e os descontos em `service/PricingService.java:15-66`,
as situações da reserva e a devolução em `service/ReservationService.java:81-149`. O SQL fica todo nos
`Jdbc*Repository`, com parâmetros `?`. As peças são criadas e ligadas num lugar só (`App.java:25-42`), por construtor,
sem `static` mutável. Os handlers só leem a requisição, chamam o serviço e traduzem a exceção em código HTTP
(`api/ReservationController.java:52-162`).

Há um atalho pequeno: o controlador chama os repositórios direto em `GET /rooms` (`api/ReservationController.java:38`)
e `GET /messages` (`:165`). Em `toMap`, ele também busca o quarto no banco a cada resposta e não usa o resultado
(`:259`). Os 299 linhas do controlador são, na maior parte, os mesmos blocos `try/catch` repetidos, e não regra
misturada. Isso fica no design, não aqui.

## Design de baixo nível

**Números:** os 4 relatórios estão escritos inteiros, cada um com a sua sequência título → cabeçalho → laço de linhas
→ total. Nenhum passa por um esqueleto comum: 0 de 4 na estrutura e 4 de 4 fora. O que se compartilha são só dois
utilitários de formatação, `formatTitle` e `formatCsvMoney` (`service/ReportService.java:163-169`). São 3 interfaces de
repositório, com 1 implementação cada.
**Classe:** omissão
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** O texto pede que o jeito dos relatórios mude "em todos de uma vez" e avisa que vêm mais dois. Em
`service/ReportService.java`, os quatro métodos repetem a mesma sequência de passos, cada um com o seu
`StringBuilder`, o seu `append(formatTitle(...))`, o cabeçalho escrito na mão, o laço e o `append("total,...")`:

- ocupação: `:30-50`;
- financeiro: `:52-79`;
- taxa de turismo: `:81-109`;
- planos de tarifa: `:111-143`.

Não há classe abstrata com um método-modelo nem um escritor genérico que receba as partes que variam. A linha de data
que vem no ano que vem teria de ser escrita nos quatro métodos, e o próximo relatório seria uma quinta cópia. Pela
referência, isso é omissão. O único passo comum que foi extraído é a linha de título, num utilitário, e a referência
chama isso de "malfeito". Mas não existe nenhuma estrutura que algum relatório use, então fica a classe omissão (ver
Dúvidas).

**O exagero.** `ReservationRepository`, `RoomRepository` e `MessageRepository` são interfaces com uma implementação
cada (`repository/*Repository.java`). Nenhum teste nem outra implementação as substitui: o único teste nem toca os
repositórios. `RoomRepository` ainda expõe `initialize()` (`repository/RoomRepository.java:11`), que é coisa da
montagem e não do acesso a dados. É pouco, mas é estrutura sem uso.

**O resto do código.**

- **O mesmo conjunto "pago" em três lugares:** `domain/Reservation.java:157-161`,
  `service/ReportService.java:151-155` e o SQL em `repository/JdbcReservationRepository.java:153`.
- **Formatação de dinheiro que depende do idioma da máquina:** `service/ReservationService.java:215-217` usa
  `String.format("R$ %,.2f")` com o idioma padrão do Java e troca vírgula por ponto depois. Com `en_US` sai
  `R$ 1.234,56`; com `pt_BR` (testei com `-Duser.language=pt -Duser.country=BR`) sai `R$ 1,234.56`, o comprovante e
  o e-mail errados num servidor configurado em português.
- **Código repetido no controlador:** o `Long.parseLong(ctx.pathParam("id"))` aparece seis vezes
  (`api/ReservationController.java:94,104,116,128,140,153`) sem tratar um id que não é número (vira 500). Os mesmos
  `catch` se repetem em todos os handlers, e não há um tratador de exceção central.
- **Desconto sem as 2 casas:** o plano normal devolve `BigDecimal.ZERO`, sem escala (`service/PricingService.java:42`).
  Assim o JSON sai com `discount: 0` em vez de `0.00`.
- **`Reservation` sem proteção:** a classe só tem setters (`domain/Reservation.java:24-155`), e as mudanças de
  situação ficam espalhadas no serviço com `if` (`service/ReservationService.java:84,100-107,121,134`).
- **O preço e a capacidade em dois lugares:** estão gravados no banco (`repository/JdbcRoomRepository.java:93-97`),
  mas o código nunca os lê. Tudo vem do `enum` (`domain/RoomType.java:6-8`, `repository/JdbcRoomRepository.java:22`).

Os tipos estão bem escolhidos: `BigDecimal` com `HALF_EVEN` a cada passo, `LocalDate`, `YearMonth` e `enum` para
situação e plano.

## Testes

**Números:** 8 testes rodaram, 0 falharam, 0 foram pulados; 8 métodos escritos, num arquivo só
(`PricingServiceTest`). A cobertura é de 47 de 828 linhas (5,7%) e 14 de 148 ramos (9,5%). Os pacotes `api` e
`repository` estão em 0%, e `service` cobre 30 de 237 linhas (`measures/tests.md`). Não há testes pela API nem contra
o banco.
**Nota:** ruim

Os testes que existem conferem valores reais, e isso é bom:

- os 299,88 da alta temporada em julho, dezembro e janeiro (`src/test/java/com/pousadamarealta/service/PricingServiceTest.java:32-50`);
- os 599,76 do exemplo do enunciado (`:53-58`);
- o desconto de 89,96 com meio para o par (`:61-65`) e o total (`:75-80`).

Mas é só o preço. Não há nenhum teste para:

- as situações da reserva e as recusas (`OPERATION_NOT_ALLOWED`);
- o cancelamento e a devolução;
- o quarto ocupado e a capacidade;
- o comprovante e o e-mail;
- os códigos de erro da API;
- os quatro relatórios, que são justamente a parte nova do cenário.

Pela pergunta da mutação: se a taxa `4.50` (`service/ReportService.java:20`) virasse `5.40`, se o filtro
`compareTo(BigDecimal.ZERO) > 0` do financeiro e dos planos (`:64`, `:115`) virasse `>= 0`, ou se a linha de título
fosse apagada de um relatório, nenhum teste falharia. Os limites da temporada também ficam sem rede: 14/12 contra
15/12, e o 1º de março. O `test-integration.sh` é um roteiro manual com Docker, fora do `mvn verify`, e não confere
valores.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; o dinheiro em `numeric(10,2)`
e as datas em `date`. As conferências acusaram (`measures/database.md`):

- a chave estrangeira `reservations(room_number) → rooms` sem índice;
- nenhum `CHECK` nas colunas de situação, plano, tipo e canal;
- nenhuma restrição `UNIQUE`/`EXCLUDE`;
- nenhuma transação explícita e nenhum `FOR UPDATE`;
- o esquema criado por `CREATE TABLE` no código, sem migrações.

**Nota:** ruim

O que está certo: os tipos, o `NOT NULL` no que é obrigatório, a chave estrangeira da reserva para o quarto
(`repository/DatabaseInitializer.java:46`), SQL sempre com `?`, conexões em try-with-resources e um pool Hikari
(`App.java:66-73`). Os dados continuam depois de reiniciar o serviço.

O defeito sério é a regra principal da pousada, que nada protege: um quarto não pode ter duas reservas na mesma noite.
`ReservationService.createReservation` consulta os conflitos (`service/ReservationService.java:47`) e só depois insere
(`:73`), em conexões separadas e com o commit automático. Não há transação serializável, trava, `synchronized` nem
restrição `EXCLUDE` no banco. O roteiro do comportamento mostrou o resultado: em 9 de 10 rodadas, dois pedidos
simultâneos criaram as duas reservas para o mesmo quarto e as mesmas noites. É dado errado no banco: o mesmo quarto
vendido duas vezes.

Há também defeitos menores:

- A confirmação grava a situação e depois o e-mail, em duas escritas sem transação
  (`service/ReservationService.java:90-92`). Se a segunda falhar, a reserva fica confirmada sem mensagem.
- As mudanças de situação leem e regravam a linha sem trava (`repository/JdbcReservationRepository.java:89-112`), e
  duas confirmações ao mesmo tempo podem passar as duas.
- O esquema não tem como evoluir: com `CREATE TABLE IF NOT EXISTS`, uma coluna nova não chega a um banco que já existe.
- O relatório por mês usa `EXTRACT(YEAR/MONTH FROM check_in)` (`repository/JdbcReservationRepository.java:185-186`),
  que não aproveita índice. Mas não há índice em `check_in` de qualquer forma.

## Comportamento

**Números:** 59 de 64 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 42 de 46 |
| dados depois de reiniciar | 3 de 3 |
| cenário 09 (relatórios) | 13 de 13 |
| reservas simultâneas | 1 de 2 |

**Nota:** com problemas

A parte do cenário está toda certa:

- os quatro relatórios começam com `Pousada Maré Alta,<nome>,<período>`;
- a taxa de turismo dá R$ 4,50 por hóspede por noite, só das reservas pagas e na ordem do financeiro;
- o relatório de planos traz `STANDARD` e `NON_REFUNDABLE` mesmo sem reserva no mês e conta a não reembolsável
  cancelada;
- a ocupação e o financeiro estão como o texto descreve.

Preço, temporada, arredondamento, confirmação, e-mail, comprovante, check-in, check-out e devoluções também passaram.

O que falhou, e o que quer dizer para o dono:

- **Duas reservas do mesmo quarto ao mesmo tempo:** em 9 de 10 rodadas, as duas foram criadas. Num fim de semana
  cheio, dois hóspedes que reservam juntos pelo site podem ficar com o mesmo quarto. É o defeito mais grave, e a causa
  está na coluna do banco.
- **Corpo que não é JSON vira 500, não 400 `INVALID_REQUEST`:** `ctx.bodyAsClass` (`api/ReservationController.java:54`)
  lança uma exceção que nenhum `catch` trata. Fora do roteiro, um id que não é número (`GET /reservations/abc`)
  também dá 500 (testei com `curl`).
- **Dinheiro sem as 2 casas no JSON:** `amountPaid` e `discount` saem como `0`, não `0.00`. O texto diz "número com
  2 casas" (vem de `domain/Reservation.java:26` e `service/PricingService.java:42`).
- **`GET /rooms` com `number` como texto (`"201"`):** as duas conferências do 201 (capacidade e diária) falharam por
  isso. Os valores estão certos: `capacity: 3`, `nightlyRate: 389.90`. O texto não fecha o tipo do número do quarto,
  então essa é uma escolha razoável e não pesa como erro.

À parte, a formatação do dinheiro no comprovante dá certo nesta máquina (`en_US`), mas sairia `R$ 1,234.56` num
servidor em `pt_BR` (contado no design).

## Dúvidas

- **A classe do pattern:** entre omissão e malfeito. Fica um utilitário compartilhado para a linha de título
  (`service/ReportService.java:163`), e a referência chama de "malfeito" o "esqueleto" que é só uma função utilitária.
  Dei omissão porque não existe estrutura nenhuma: os quatro relatórios continuam escritos inteiros, cada um com a
  sequência toda.
- **O exagero:** as três interfaces de repositório com uma implementação são um padrão comum e de custo baixo. Marquei
  exagero porque nada as substitui, nem nos testes, mas o peso é pequeno.
- **O banco:** dei `ruim`, e não `com problemas`, porque não há proteção nenhuma contra a reserva duplicada, nem dentro
  do processo, e ela foi reproduzida. Se o critério for que a falta de restrição só pesa como "com problemas", a nota
  cairia para esta.
