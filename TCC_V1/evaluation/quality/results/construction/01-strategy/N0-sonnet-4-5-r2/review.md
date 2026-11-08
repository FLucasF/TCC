# Revisão: construção, cenário 01

## Visão geral

O projeto tem 5 pacotes em `com.pousadamarealta`. O `App` monta tudo à mão em `App.java:29-59`. O pacote `api` tem um `ApiController` com as rotas e os DTOs em records. O `service` tem `ReservationService`, `PricingService`, `ReportService` e `DocumentService`. O `repository` usa JDBC puro com `DriverManager` e cria o esquema com `CREATE TABLE IF NOT EXISTS` em `Database.java:27-65`. O `domain` tem a `Reservation` mutável e os enums. Um `POST /reservations` passa por estes passos: `ApiController.createReservation` monta uma `Reservation`, `ReservationService.createReservation` busca o quarto e valida, `PricingService.calculatePricing` calcula o preço, o serviço procura conflito e depois faz o `INSERT`. Os dados ficam no PostgreSQL e continuam lá depois de reiniciar. O mais sério:
- **Reservas em dobro:** nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos. Não há restrição no banco, nem transação, nem trava, e o roteiro criou as duas reservas nas 10 rodadas de 10.
- **Planos de tarifa num `switch`:** os cinco planos, que o texto avisa que vão continuar crescendo, ficaram em `switch` sobre o plano no preço e na devolução (`PricingService.java:30`, `PricingService.java:151`), sem o Strategy.
- **E-mail com caractere diferente:** o texto do e-mail traz um espaço inseparável no lugar do espaço em "R$ 599,76".

## Arquitetura

**Números:** 5 pacotes, 9 dependências entre eles, nenhum ciclo. O Javalin só aparece em `api` e na raiz, o JDBC só em `repository`, e o `service` não usa nenhuma das duas bibliotecas. A maior classe é a `ReservationRepository`, com 221 linhas.
**Nota:** com problemas

As camadas existem e as dependências vão na direção certa: `api` chama `service`, que chama `repository`. As regras não conhecem o Javalin nem o SQL, o acesso ao banco fica isolado em três repositórios, e a montagem acontece num lugar só (`App.java:29-59`). Os defeitos são localizados:
- **Uma regra no handler:** a regra "a segunda via só existe para reserva paga" está no handler, em `ApiController.java:145-147`, e não no serviço.
- **O controller pula o serviço:** ele fala direto com `RoomRepository` e `MessageRepository` (`ApiController.java:16,18,71,153`), então esses dois caminhos não passam pela camada de regras.
- **O tipo do quarto deduzido do número, em três lugares:** com `switch` sobre "101", "102" etc. em `DocumentService.java:41-48`, `ReportService.java:68-75` e `DocumentResponse.java:61-68`, quando o `Room` já tem o tipo. O total de quartos também está fixo no relatório (`"total,%d/5"`, `ReportService.java:33`). Se a pousada ganhar um quarto, é preciso mexer em quatro lugares.
- **Um serviço estático:** `DocumentService` é chamado direto de dentro do `ReservationService` (`ReservationService.java:121`), fora da montagem.

## Design de baixo nível

**Números:** 0 dos 5 planos estão numa estrutura própria. Há dois `switch` de comportamento sobre `RatePlan`: o do preço, com 5 casos (`PricingService.java:30-36`), e o da devolução, com 3 casos e um `default` (`PricingService.java:151-158`). As validações dos planos (código da empresa, sexta a domingo, 7 noites) ficam em métodos privados da mesma classe.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `RatePlan` é um enum sem comportamento (`RatePlan.java:3-9`). O preço de cada plano é escolhido por um `switch` em `PricingService.calculatePricing` (`PricingService.java:30`), que chama `calculateStandard`, `calculateCorporate` e os outros métodos privados. A devolução é outro `switch` em `calculateRefund` (`PricingService.java:151`). As tabelas do convênio e do pacote também ficam em constantes dessa classe (`PricingService.java:15-24`). É exatamente o que a referência chama de omissão: o próximo plano (o da melhor idade, ou o de 60 dias de antecedência) obriga a mexer em dois `switch`, no enum e em constantes da mesma classe. Ainda assim, cada plano tem pelo menos o seu método privado, e as validações estão junto do cálculo do plano, e não espalhadas pelo serviço (`PricingService.java:60,77,84,95`).

**O exagero.** Não há estrutura sem necessidade: nenhuma interface com implementação única, nenhuma fábrica, nenhuma camada que só repassa.

**O resto do código.**
- **Duplicação:** além do tipo do quarto deduzido do número em três lugares, os métodos de preço repetem o mesmo trecho de subtotal, desconto e total três vezes (`PricingService.java:46-56`, `58-73`, `94-107`). A conversão de `IllegalStateException` em `OperationNotAllowedException` também se repete quatro vezes (`ReservationService.java:71-75`, `86-91`, `99-103`, `111-115`).
- **Código morto:** um ternário com os dois ramos iguais (`DocumentService.java:22-24`) e um `replace(",", ".")` que não tem efeito sobre o `toString()` de um `BigDecimal` (`ReportService.java:78`).
- **Erros engolidos:** o `catch (Exception e)` de `ApiController.java:99-107` converte qualquer erro em 400 `INVALID_REQUEST`, inclusive a `RuntimeException` de uma falha do banco, e esconde a causa.
- **Formatação do dinheiro:** é feita com `NumberFormat.getCurrencyInstance(pt-BR)` (`DocumentService.java:12,61-62`), que no Java 21 põe um espaço inseparável (U+00A0) depois de "R$". Por isso o comprovante não sai como o texto pede (conferi os bytes na API: `R$ 302 240 7 7 9 , 8 0`).
- **Estado sem controle:** a `Reservation` tem setter para todos os campos (`Reservation.java:30-164`), então qualquer um pode mudar o status ou o valor pago sem passar pelas transições de `confirm`, `checkIn`, `checkOut` e `cancel`.
- **O que está certo:** os tipos são os certos (`BigDecimal` com `HALF_EVEN`, `LocalDate`, enums de status e de plano).

## Testes

**Números:** 14 testes rodaram, com 0 falhas, 0 erros e 0 pulados, e há 14 métodos de teste no código (8 em `PricingServiceTest`, 6 em `ApiIT`). A cobertura é de 61,0% das linhas (461 de 756) e de 39,2% dos ramos (65 de 166). Os pacotes `api` e raiz têm 0%, e o `service` tem 176 de 238 linhas e 41 de 68 ramos. Há ainda um terceiro arquivo de teste, de 453 linhas, desligado pelo nome: `ReservationSystemIT.java.disabled`.
**Nota:** com problemas

**O que está bom.** Os testes conferem com valores os exemplos do texto: 599,76 na alta temporada (`PricingServiceTest.java:32-34`); a Tecnosul com 779,80, 93,58 e 686,22 (`PricingServiceTest.java:48-50`, `ApiIT.java:136-138`); o pacote com 820,00 e a devolução de 410,00 (`PricingServiceTest.java:63-65,77`); a estadia longa com 1.749,30, 174,93 e 1.574,37 (`PricingServiceTest.java:90-92`). Também testam a devolução do normal, do não reembolsável e da reserva pendente (`PricingServiceTest.java:95-123`).

**Pela pergunta da mutação:**
- trocar 0.12 por 0.21 é pego (`PricingServiceTest.java:49`);
- trocar `< 7` por `<= 7` é pego, porque o teste usa exatamente 7 noites;
- apagar a validação do código da empresa (`PricingService.java:60`) não é pego;
- apagar a regra de sexta a domingo (`PricingService.java:84`) não é pego;
- mudar a devolução do convênio ou da estadia longa para zero não é pego, porque nenhum teste cancela esses planos;
- nenhuma recusa (400, 404, 409, 422) é testada;
- o texto do e-mail e do comprovante não é conferido: `ApiIT.java:115-118` só olha canal e destinatário, por isso o espaço inseparável passou;
- o relatório financeiro só confere que existe uma linha `total,` (`ApiIT.java:191`).

**Os níveis.** A camada HTTP não tem nenhum teste: o teste pela API, com RestAssured e Testcontainers, foi desligado ao renomear o arquivo para `.disabled`.

**Um risco no `ApiIT`.** Ele roda `DROP TABLE ... CASCADE` nas três tabelas do banco que estiver em `DB_URL` (`ApiIT.java:53-55`). Um `mvn verify` com as variáveis de produção apaga todas as reservas. Além disso, quando não há banco, o `ApiIT` se pula em silêncio (`ApiIT.java:44,74`).

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária, e o dinheiro em `numeric(10,2)`.
- **O que as conferências acusaram:** a chave estrangeira `reservations(room_number)` não tem índice; `status`, `rate_plan`, `type` e `channel` não têm CHECK; não há nenhuma restrição UNIQUE, EXCLUDE ou CHECK.
- **Do código:** 0 pool de conexões, 1 `DriverManager.getConnection`, 0 transações explícitas e 0 `FOR UPDATE`. O script não achou SQL montado com `+`.

**Nota:** com problemas

**O que está certo.** Os tipos estão certos: dinheiro em `numeric(10,2)`, datas em `date` (`Database.java:41-52`), e a reserva ligada ao quarto por chave estrangeira (`Database.java:53`). O SQL usa sempre parâmetros `?` (por exemplo, `ReservationRepository.java:116-141`), e as conexões são fechadas com try-with-resources. Os dados continuam lá depois de reiniciar.

**Os defeitos:**
- **Esquema sem migração:** o esquema é criado no código com `CREATE TABLE IF NOT EXISTS` (`Database.java:27-65`), sem migração. A primeira coluna nova não vai chegar a um banco que já existe.
- **Sem pool:** cada operação abre uma conexão nova com `DriverManager` (`Database.java:19-20`).
- **Nada protege a regra de uma reserva por noite:** o serviço consulta os conflitos e depois insere, em duas conexões separadas, sem transação nem trava (`ReservationService.java:35-45`), e o banco não tem uma restrição de exclusão sobre quarto e período. O roteiro do comportamento confirmou o dano: duas reservas criadas nas 10 rodadas de 10. Isso é pior que a proteção "só dentro do processo" que a skill põe em `com problemas`. Mesmo assim, deixo a nota aqui, porque a falha também pesa no comportamento (veja "Dúvidas").
- **Menores:** falta índice na chave estrangeira, e as colunas de situação ficam em `varchar` sem CHECK.

## Comportamento

**Números:** 67 de 72 conferências passaram.

| parte | passaram |
|---|---|
| a parte comum | 42 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| os planos novos do cenário 01 | 21 de 21 |
| duas reservas ao mesmo tempo | 1 de 2 |

**Nota:** com problemas

Todos os planos novos funcionam como o texto pede:
- os três convênios e as recusas sem código ou com código errado;
- o pacote com preço fechado, inclusive em julho, as recusas fora de sexta a domingo e a devolução de 410,00;
- a estadia longa com a temporada, a recusa com 6 noites e a devolução integral.

O normal e o não reembolsável continuam certos.

**As falhas:**
- **"O 201 comporta 3" e "a diária do 201 é 389.90":** não são erro. A API devolve o número do quarto como texto (`"number":"201"`), e o roteiro procura um número. Conferi com `curl`, e a capacidade (3) e a diária (389.90) estão lá. O texto não diz o tipo de `number`, então foi uma escolha razoável.
- **Dinheiro sem 2 casas:** na criação, a API devolve `"amountPaid":0` e `"discount":0`, em vez de `0.00`, porque usa `BigDecimal.ZERO` (`Reservation.java:27`, `PricingService.java:42`). O texto pede número com 2 casas. É um defeito pequeno de formato, mas um site que compara o texto do JSON vai estranhar.
- **E-mail com caractere diferente:** o e-mail de confirmação traz "R$" seguido de um espaço inseparável, e não do espaço normal (`DocumentService.java:12,61-62`). Para o hóspede, o e-mail parece igual. Mas o texto não é o que o dono definiu, e qualquer busca ou conferência por "R$ 599,76" falha.
- **Reservas em dobro:** em 10 rodadas de dois pedidos simultâneos para o mesmo quarto e as mesmas noites, as duas reservas foram criadas todas as vezes. Para o dono, isso quer dizer dois hóspedes no mesmo quarto na mesma noite, se o site mandar dois pedidos quase juntos. A causa está descrita na coluna do banco.

**Fora do roteiro:** um id que não é número (`GET /reservations/abc`) responde 400 no formato do próprio Javalin (`{"id":[{"message":"TYPE_CONVERSION_FAILED",...}]}`), e não com `{"error","message"}`. Um plano desconhecido responde `INVALID_REQUEST`, mas a mensagem expõe o texto interno do Jackson.

## Dúvidas

- **A nota do banco:** fiquei entre `com problemas` e `ruim`. A regra de uma reserva por quarto e por noite não tem proteção nenhuma (nem dentro do processo), e o roteiro mostrou dados duplicados em 10 de 10 rodadas, o que é um risco de dado errado. Fiquei com `com problemas` porque a lista de `ruim` da skill não inclui esse caso e porque a falha já pesa na coluna do comportamento. Um revisor mais rígido daria `ruim`.
- **A nota da arquitetura:** está no limite com `bom`. As misturas (a regra da segunda via no handler, o controller lendo repositórios direto, o tipo do quarto deduzido do número) são pequenas e localizadas.
