# Revisão: construção, cenário 01

## Visão geral

O projeto tem dois pacotes: `br.com.marealta`, com 15 classes e 801 linhas (a borda HTTP, o serviço, o acesso ao banco, o dinheiro, o comprovante), e `br.com.marealta.plan`, com 8 classes e 199 linhas (os planos de tarifa). Uma requisição entra pelas rotas do Javalin em `App.java:39-66`. O corpo é lido por `RequestParser.java:15-42` e as rotas chamam `ReservationService`, que abre uma transação por operação (`Db.tx`, `Db.java:58-72`), escreve o SQL ele mesmo e monta o JSON de volta em `App.java:97-152`. Os dados ficam no PostgreSQL, em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código (`Db.java:79-125`), com HikariCP. Os planos estão num Strategy feito como o pedido: uma interface, uma implementação por plano e um registro por código. O comportamento passou em todas as 72 conferências, inclusive as das reservas simultâneas. Os pontos fracos são três. O serviço mistura regra de negócio com SQL e há um ciclo entre os dois pacotes. Os testes cobrem só os planos (16,7% das linhas). O esquema é criado no código, sem migrações e sem restrições no banco.

## Arquitetura

**Números:** 2 pacotes (`br.com.marealta`, com 15 classes e 801 linhas; `br.com.marealta.plan`, com 8 classes e 199 linhas), 2 dependências entre eles e 1 ciclo (`br.com.marealta` ↔ `br.com.marealta.plan`). Javalin, JDBC, Jackson e o pool ficam todos no pacote raiz; o pacote `plan` não usa biblioteca nenhuma. Maior classe: `ReservationService`, com 282 linhas.
**Nota:** com problemas

- **A borda HTTP é fina.** As rotas só leem o pedido, chamam o serviço e escrevem a resposta (`App.java:39-66`). A única exceção é a regra da segunda via, decidida dentro do handler (`App.java:55-57`, "só há comprovante para reserva paga") e não no serviço.
- **A montagem fica num lugar só.** Tudo é criado em `App.main` (`App.java:18-22`) e o `Db` é injetado no serviço pelo construtor (`ReservationService.java:28-30`).
- **As regras e o SQL moram na mesma classe.** `ReservationService` decide a situação da reserva (`ReservationService.java:127, 145, 155, 164, 228-235`), calcula a devolução e gera o e-mail. Na mesma classe estão o SQL de todas as operações (`ReservationService.java:85-120, 131-137, 176-197, 206-221, 237-281`) e a montagem dos dois CSVs (`ReservationService.java:172-224`). Não há uma classe de acesso a dados; os relatórios e o mapeamento das linhas também ficam no serviço. Esta é a classe que mais vai crescer a cada pedido.
- **O ciclo entre os pacotes.** O ciclo existe porque os planos usam classes do pacote raiz: `ApiException` (`CorporatePlan.java:3`, `LongStayPlan.java:3`, `WeekendPackagePlan.java:3`, `RatePlans.java:3`), `Money` (`PercentagePlan.java:3`, `WeekendPackagePlan.java:4`) e `RoomType` (`RatePlan.java:3`). Ao mesmo tempo, o pacote raiz usa os planos (`ReservationService.java:3-4`, `Pricing.java:3`). Por isso a regra dos planos depende de uma exceção que carrega o status HTTP (`ApiException.java:4-5, 13-15`).
- **Fora do pacote `plan`, não há separação em pacotes.** Todo o resto está num pacote só.

O ciclo é pequeno, entre dois pacotes vizinhos, e a borda é fina. Mesmo assim, regra e SQL juntos numa classe e o ciclo novo põem a coluna em "com problemas".

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (`STANDARD`, `NON_REFUNDABLE`, `CORPORATE`, `WEEKEND_PACKAGE` e `LONG_STAY`) e 0 fora dela. Não há nenhum `switch` nem `if` sobre o plano no serviço ou no cálculo do preço.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A interface `RatePlan` (`RatePlan.java:13-40`) reúne as três coisas que variam por plano: quem pode usar (`validate`, linha 28), o preço (`price`, linha 31) e a devolução (`refund`, linha 34). Cada plano tem a sua implementação:

- `PercentagePlan`, configurado para `STANDARD` (0%, reembolsável) e `NON_REFUNDABLE` (15%, sem reembolso), em `RatePlans.java:13-14`, com o desconto e a devolução em `PercentagePlan.java:29-36`;
- `CorporatePlan`, com a tabela das empresas e a validação do código (`CorporatePlan.java:9-23`);
- `WeekendPackagePlan`, com a regra de sexta a domingo, o preço fechado e a devolução de metade (`WeekendPackagePlan.java:24-38`);
- `LongStayPlan`, com o mínimo de 7 noites (`LongStayPlan.java:14-18`).

O plano é escolhido num único ponto, um mapa indexado pelo código (`RatePlans.java:12-28`). O serviço só pede as coisas ao plano: `plan.validate` e `Pricing.price` na criação (`ReservationService.java:96-98`) e `refund` no cancelamento (`ReservationService.java:146`), sem nomear nenhum plano. Até o código da empresa, a única regra de plano pequena que poderia ter ficado no serviço, foi para dentro do plano (`companyCodeToStore`, `RatePlan.java:37`, `CorporatePlan.java:32`). Um plano novo é uma classe nova e uma linha no registro.

Há duas imperfeições pequenas na estrutura:

- `CorporatePlan` estende `PercentagePlan` passando um percentual 0 que nunca é usado (`CorporatePlan.java:15`), porque o desconto real vem de outro método (`CorporatePlan.java:27-29`).
- `Pricing.price` calcula a soma das noites com temporada mesmo para o pacote, que a ignora (`Pricing.java:31-33`, `WeekendPackagePlan.java:31-33`).

Nenhuma das duas muda a classe.

**O exagero.** Não há estrutura sem necessidade. A única interface fora dos planos é a funcional `Db.Work` (`Db.java:11-13`), usada em todo o código pelo `tx`. Não há fábricas, builders nem camadas que só repassam.

**O resto do código.**

- **Os tipos estão certos.** O dinheiro é `BigDecimal`, sempre arredondado com `HALF_EVEN` (`Money.java:15-25`), e a soma é arredondada a cada noite (`Pricing.java:23-28`). As datas são `LocalDate`, e a situação e o tipo do quarto são `enum` (`Status.java`, `RoomType.java`).
- **A duplicação é pouca.** As operações de estado usam o mesmo `requireStatus` e o mesmo `update` (`ReservationService.java:228-246`).
- **O tratamento de erros é claro.** A exceção genérica é registrada no log, e não engolida (`App.java:29-32`).

Pontos menores, que não pedem mudança antes de aceitar:

- `Reservation.ratePlan` guarda texto em vez do plano (`Reservation.java:8`), e o cancelamento busca o plano de novo pelo código (`ReservationService.java:146`).
- `ApiException` tem campos públicos e o status HTTP é decidido dentro dos planos (`ApiException.java:4-5`).
- `create` tem uns 50 linhas e mistura validação, preço, conferência de conflito e o `INSERT` (`ReservationService.java:72-122`).

## Testes

**Números:** 8 testes rodaram, 0 falharam e 0 foram pulados; há 8 métodos de teste num único arquivo (`PricingTest.java`). Cobertura: 71 de 425 linhas (16,7%) e 27 de 114 ramos (23,7%). No pacote `plan`, 45 de 48 linhas e 14 de 16 ramos; no pacote raiz, 26 de 377 linhas. Nenhum teste passa pela API nem usa o banco.
**Nota:** com problemas

**O que os testes conferem bem.** Os testes que existem conferem a parte nova com os valores do texto:

- o convênio Tecnosul, com 779,80, 93,58 e 686,22 (`PricingTest.java:42-47`);
- a recusa do convênio sem código e com código desconhecido (`PricingTest.java:50-53`);
- o pacote deluxe a 820,00, a devolução de 410,00, o preço fechado em julho e as recusas fora de sexta a domingo (`PricingTest.java:56-64`);
- a estadia longa, com 1.749,30, 174,93 e 1.574,37, e a recusa com 6 noites (`PricingTest.java:67-73`);
- as devoluções de quatro planos (`PricingTest.java:76-82`);
- os limites da alta temporada (`PricingTest.java:26-33`), o STANDARD de 599,76 (`PricingTest.java:36-39`) e o formato R$ (`PricingTest.java:85-89`).

**A pergunta da mutação na parte nova.** Trocar 12 por 21 na Tecnosul derruba um teste. Trocar os percentuais da Agrovale ou da Farmavida (`CorporatePlan.java:11-12`), o preço do pacote na suíte (`WeekendPackagePlan.java:15`) ou os 15% do `NON_REFUNDABLE` (`RatePlans.java:14`) não derruba nada. Os `assertThrows` só conferem o tipo da exceção, não o código `INVALID_REQUEST` nem o status 400 (`PricingTest.java:51-52, 62-63, 72`).

**O que ficou sem teste.** Nada do resto do sistema tem teste:

- as transições de situação e as recusas `OPERATION_NOT_ALLOWED`;
- a capacidade e o conflito de datas (`ROOM_UNAVAILABLE`, `CAPACITY_EXCEEDED`);
- o comprovante e o e-mail (`Receipt.java`);
- os dois relatórios CSV;
- a validação do corpo (`RequestParser.java`);
- o cancelamento de ponta a ponta (com o `amountPaid` e o `refund` gravados).

Os testes passam e acertam o centro do cenário, mas são poucos e cobrem só um sexto do código.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. As 6 colunas de dinheiro são `numeric(…,2)`. Há 1 chave estrangeira (de `reservations` para `rooms`), com índice. Nenhuma data está guardada como texto. Há 0 restrições `CHECK`, `UNIQUE` ou `EXCLUDE`: a situação, o plano, o tipo e o canal são `varchar` sem `CHECK`. O esquema é criado no código Java, com 0 arquivos de migração.
**Nota:** com problemas

**O que está certo.**

- Os tipos estão certos: `NUMERIC(12,2)` para o dinheiro e `DATE` para as datas (`Db.java:96-114`).
- A reserva está ligada ao quarto por chave estrangeira (`Db.java:98`), com índice em `(room_number, check_in, check_out)` (`Db.java:115`).
- Todo SQL usa parâmetros. Os trechos montados com `+` concatenam só constantes (`RES_COLS` e `" FOR UPDATE"`, `ReservationService.java:250, 267`).
- Há um pool HikariCP (`Db.java:17-25`), as conexões são fechadas com try-with-resources e cada operação roda numa transação com rollback (`Db.java:58-72`). A confirmação e o e-mail andam juntos na mesma transação (`ReservationService.java:124-140`).
- A duplicação de reserva é evitada por uma trava no banco, que vale entre processos: `SELECT … FOR UPDATE` na linha do quarto (`ReservationService.java:74, 250`) antes de procurar conflito (`ReservationService.java:85-97`).

**Os defeitos.**

- O esquema é feito de `CREATE TABLE IF NOT EXISTS` no código (`Db.java:79-125`), sem migrações versionadas. A primeira coluna nova num banco que já tem dados não vai ser criada.
- Nenhuma regra é protegida pelo próprio banco. Não há `CHECK` para a situação, o plano ou `guests >= 1`, nem uma restrição de exclusão contra noites sobrepostas. Qualquer escrita que não passe pelo serviço pode gravar dado inválido.
- Os quartos são semeados por `INSERT … ON CONFLICT DO NOTHING` (`Db.java:88-94`). Uma mudança de diária no código nunca chega a um banco que já existe.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam:

- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 01 (os planos novos): 21 de 21;
- reservas simultâneas: 2 de 2 (em 10 rodadas, nunca as duas criadas, sempre uma criada e a outra recusada com 409).

**Nota:** bom

Tudo o que o texto pede funcionou pela API:

- os preços com temporada e arredondamento meio para o par;
- os códigos de erro;
- a confirmação com o e-mail e o comprovante, o check-in, o check-out e os cancelamentos com as devoluções;
- os dois relatórios CSV;
- os três planos novos, com os valores que o financeiro passou: R$ 686,22, R$ 820,00 com devolução de R$ 410,00 e R$ 1.574,37, além dos outros percentuais do convênio, do pacote na suíte e da estadia longa com noites de temporada.

Os dados continuam lá depois de reiniciar, e duas reservas simultâneas para o mesmo quarto nunca passam juntas. Para o dono da pousada, o sistema faz o que ele descreveu.

## Dúvidas

Nenhuma sobre as notas. Uma ressalva sobre o design: as regras dos planos lançam `ApiException`, uma exceção com o status HTTP (400). Isso não tira os planos da estrutura do Strategy, mas é a causa do ciclo entre os pacotes. Por isso o problema foi contado na arquitetura, e não na classe do pattern.
