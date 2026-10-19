# Cenário 01

## Visão geral

O agente fez os quatro relatórios funcionarem, mas não deu a eles um desenho comum. Em `ReportService` há quatro métodos, e cada um repete do começo ao fim a mesma sequência de passos. A linha de título "Pousada Maré Alta,<nome>,<período>" aparece escrita à mão quatro vezes (`src/main/java/com/pousadamarealta/service/ReportService.java:27`, `:56`, `:89`, `:123`), e o cabeçalho, o laço das linhas e a linha "total" se repetem do mesmo jeito. Não existe esqueleto, nem por herança nem por composição. O relatório de cancelamentos que o contador já anunciou vai ser um quinto bloco copiado. O ponto forte é o comportamento dos relatórios: os quatro saíram exatamente como o enunciado pede. O resto do sistema funciona, com três desvios pequenos na parte comum: um corpo que não é JSON devolve 500, os valores zero vão no JSON como `0`, e o dinheiro do e-mail usa um espaço especial (não separável) entre "R$" e o valor.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote é único, `com.pousadamarealta`, dividido em quatro partes:
- `controller`: um controlador por recurso, `RoomController`, `ReservationController`, `MessageController` e `ReportController`, mais o registro `ErrorResponse`;
- `service`: `RoomService` (os cinco quartos fixos numa lista, `src/main/java/com/pousadamarealta/service/RoomService.java:20-24`), `ReservationService` (as reservas num `HashMap` com `AtomicLong`, as transições de estado, o comprovante e o e-mail), `MessageService` (a lista de mensagens enviadas) e `ReportService`;
- `model`: as entidades e os enums;
- `util`: `PricingCalculator` (as noites, a alta temporada, o desconto e o arredondamento HALF_EVEN) e `Formatter` (moeda, data e CPF).

Tudo é montado à mão em `src/main/java/com/pousadamarealta/App.java:18-26`, e as rotas são registradas em `App.java:36-51`. Uma requisição segue este caminho: a rota chama o método do controlador, que lê e valida a entrada (`ReservationController.java:23-32`) e chama o serviço; o serviço aplica a regra, lança exceções internas, e o controlador traduz essas exceções em `{error, message}` (`ReservationController.java:40-50`). Os dados ficam só na memória. Os testes do agente são 19 e testam os serviços direto, sem HTTP: 8 em `ReservationServiceTest`, 6 em `PricingCalculatorTest` e 5 em `ReportServiceTest`, que cobrem os quatro relatórios e o plano sem reservas.

**A parte do fim do enunciado.** Os quatro relatórios são quatro métodos públicos de `ReportService`:
- `generateOccupancyReport` (`ReportService.java:24-51`);
- `generateRevenueReport` (`:53-84`);
- `generateTourismTaxReport` (`:86-118`);
- `generateRatePlansReport` (`:120-163`).

Cada método faz tudo sozinho, na mesma ordem: cria um `StringBuilder`, escreve o título com o nome do relatório fixo no texto (`:27`, `:56`, `:89`, `:123`), escreve o cabeçalho (`:28`, `:57`, `:90`, `:124`), filtra e ordena as reservas (`:30-35`, `:59-64`, `:92-97`, `:126-129`), percorre as reservas com um `String.format` próprio e escreve a linha "total" (`:48`, `:81`, `:115`, `:160`). As partes que mudam (seleção, ordem, linha, total) não estão separadas da sequência: estão escritas dentro de cada método. O que é compartilhado são só predicados e cálculos privados: `startsInMonth`, `isPaid`, `hasRevenue`, `calculateRevenue`, `calculateTourismTax` e `formatDecimal` (`:165-198`). A taxa de turismo é uma constante, `TOURISM_TAX_PER_GUEST_PER_NIGHT = 4.50` (`:14`), e a ordem "entrada e depois número" aparece copiada no financeiro e na taxa (`:62-63` e `:95-96`). No controlador acontece o mesmo: quatro métodos quase iguais, cada um com o parse do parâmetro, o `contentType` e o tratamento de erro (`src/main/java/com/pousadamarealta/controller/ReportController.java:17-33`, `:35-51`, `:53-69`, `:71-87`).

**O julgamento.** O Template Method pede o esqueleto (título, cabeçalho, linhas, total) num lugar só, com cada relatório preenchendo só os passos que mudam. A alternativa aceita é um escritor genérico que recebe essas partes. O projeto não tem nenhuma das duas: não há classe abstrata, nem interface de relatório, nem escritor que receba colunas, seleção e linha. A sequência dos passos está escrita quatro vezes, e a linha de título também. Também não é `malfeito`, porque não existe uma estrutura parcial convivendo com o jeito sem ela: os quatro relatórios, a ocupação e o financeiro inclusive, estão escritos inteiros. Os auxiliares privados são cálculos e filtros, não o esqueleto. Isso é exatamente o caso que as instruções chamam de `omissão`.

**Exagero colateral.** Não encontrei interfaces com uma implementação só, fábricas, builders ou camadas que só repassam. Os serviços são classes concretas, montadas direto em `App`. A única sobra é a constante `CPF_PATTERN` em `src/main/java/com/pousadamarealta/util/Formatter.java:13`, que ninguém usa, mas isso é código morto, não estrutura.

**Comportamento.** Os quatro relatórios conferiram em tudo: título, colunas, ordem, filtros, a não reembolsável cancelada contando, as linhas dos planos mesmo vazias e os totais. Na parte comum, preço, recusas, comprovante e e-mail funcionam, com três desvios:
- um corpo que não é JSON devolve 500, quando o enunciado pede 400 `INVALID_REQUEST`, porque o erro do `bodyAsClass` em `ReservationController.java:23` não é tratado;
- os valores zero (`discount`, `amountPaid`, `refund`) vão no JSON como `0`, e não com 2 casas (`0.00`);
- o dinheiro do e-mail sai como "R$\u00a0779,80", com um espaço não separável, porque `NumberFormat.getCurrencyInstance(pt-BR)` é usado em `Formatter.java:11` e `:15`; o texto pedido usa um espaço comum.

Por isso o comportamento fica `em parte`, e o problema está todo na parte comum, não nos relatórios.

## Item a item

**O enunciado (pede o pattern).** Os quatro relatórios são métodos independentes em `src/main/java/com/pousadamarealta/service/ReportService.java:24`, `:53`, `:86` e `:120`. Cada um escreve a própria linha de título (`:27`, `:56`, `:89`, `:123`), o próprio cabeçalho (`:28`, `:57`, `:90`, `:124`), o próprio laço de linhas e a própria linha total (`:48`, `:81`, `:115`, `:160`). Não existe método-modelo, classe abstrata nem escritor genérico que receba as partes que variam. Só os filtros e cálculos (`:165-198`) são compartilhados. O controlador repete a mesma estrutura quatro vezes (`src/main/java/com/pousadamarealta/controller/ReportController.java:17-87`). Os relatórios ficaram escritos inteiros, cada um com a sua linha de título, que é a definição de omissão nas instruções do pattern.
Conclusão: omissão

**O exagero colateral.** Não há. Os serviços e controladores são classes concretas montadas direto em `src/main/java/com/pousadamarealta/App.java:18-26`, sem interfaces, fábricas ou builders. `PricingCalculator` e `Formatter` são utilitários estáticos simples. Só a constante `CPF_PATTERN`, que ninguém usa (`src/main/java/com/pousadamarealta/util/Formatter.java:13`), sobra como código morto.
Conclusão: contenção

**O comportamento.** O build da cópia funcionou, e o `mvn verify` passou com 19 testes, 0 falhas. Com o serviço na porta 7070, criei reservas em julho de 2027 e confirmei, cancelei e fiz check-in em algumas delas.

Nos relatórios, tudo conferiu:
- **Ocupação** (`GET /reports/occupancy?date=2027-07-10`): veio o título `Pousada Maré Alta,occupancy,2027-07-10`, o cabeçalho, as linhas 101 e 301 em ordem de quarto e `total,2/5`. A reserva pendente e as canceladas ficaram de fora, como deviam.
- **Financeiro** (`GET /reports/revenue?month=2027-07`): veio a não reembolsável cancelada (1193.09), a reserva com check-in (1183.00) e a confirmada (599.76), em ordem de entrada, com `total,2975.85`. A STANDARD cancelada, que foi reembolsada, não aparece.
- **Taxa de turismo** (`GET /reports/tourism-tax?month=2027-07`): vieram as colunas pedidas e só as reservas pagas: `4,301,...,4,2,36.00`, `1,101,...,2,2,18.00` e `total,54.00`. A cancelada não reembolsável ficou de fora, como devia.
- **Planos** (`GET /reports/rate-plans?month=2027-07`): veio `STANDARD,1,599.76`, `NON_REFUNDABLE,2,2376.09` e `total,3,2975.85`, que bate com o financeiro. Em agosto, sem reservas, veio `STANDARD,0,0.00`, `NON_REFUNDABLE,0,0.00` e `total,0,0.00`.
- **Formato e erros:** todos os relatórios vieram em `text/csv; charset=utf-8`. Um mês inválido (`2027-13`, `x`) e uma data ausente deram 400 `INVALID_REQUEST`.

Na parte comum:
- **O que conferiu:** o STANDARD de 10 a 12/07 deu 599.76; a não reembolsável de 3 noites no 201 deu desconto de 210.55 e total de 1193.09. As recusas vieram certas: 409 `ROOM_UNAVAILABLE`, 422 `CAPACITY_EXCEEDED`, 404 `ROOM_NOT_FOUND` e `RESERVATION_NOT_FOUND`, 409 `OPERATION_NOT_ALLOWED` no check-in de reserva pendente e na segunda via de reserva pendente, e 400 quando falta campo. O comprovante JSON vem completo, e o check-out devolve o comprovante. O e-mail tem o assunto e o texto certos.
- **O que não conferiu:** um corpo que não é JSON (`xx`) devolveu **500**, e não 400. `discount`, `amountPaid` e `refund` vêm como `0`, e não `0.00`. No e-mail, o dinheiro sai com um espaço não separável depois de "R$" (U+00A0).

No fim, o serviço foi derrubado.
Conclusão: em parte

## Dúvidas

Nenhuma sobre a classe: não há estrutura do pattern em lugar nenhum. A única dúvida é sobre o peso dos desvios no comportamento. Os quatro relatórios estão 100% certos, e os desvios ficam todos na parte comum: o 500 com corpo que não é JSON, o zero sem as 2 casas e o espaço especial no valor do e-mail. Escolhi "em parte" porque o 400 para corpo que não é JSON está escrito com todas as letras no enunciado.
