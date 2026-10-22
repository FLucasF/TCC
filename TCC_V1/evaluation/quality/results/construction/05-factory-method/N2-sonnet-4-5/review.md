# Revisão: construção, cenário 05

## Visão geral

O projeto tem 9 pacotes num desenho em camadas. A borda HTTP fica em `api.controller` e `api.dto`, as regras em `domain.service`, `domain.model`, `domain.rateplan` e `domain.identification`, e o JDBC puro, com Hikari, em `infrastructure.repository` e `infrastructure.database`. Tudo é montado à mão em `Main.java:29-76`. Uma requisição passa assim: o handler do `ReservationController` lê o JSON e monta o tipo de identificação, o `ReservationService` valida, calcula o preço com o `PricingService` e o `RatePlan` e grava pelo `ReservationRepository`. Os dados ficam em 3 tabelas que um `DatabaseInitializer` cria com `CREATE TABLE IF NOT EXISTS`. A variação do cenário (o documento por tipo de hóspede) foi resolvida com um método polimórfico no próprio tipo de identificação (`GuestIdentification.generateDocument`), que os três lugares chamam.

Os problemas mais sérios são quatro:
- **Reservas duplicadas.** Nada impede duas reservas simultâneas do mesmo quarto: o roteiro criou as duas nas 10 rodadas de 10.
- **Dinheiro no formato errado.** Os documentos em português formatam o dinheiro com o locale padrão da JVM (`String.format("R$ %,.2f")`). Numa máquina em inglês, o comprovante e a fatura saem "R$ 1,159.80" em vez de "R$ 1.159,80".
- **Título do booking receipt.** Ele sai com "nº" em vez de "no.".
- **Quase sem testes.** Há 5 testes, só do preço, com 5,1% de cobertura de linhas.

## Arquitetura

**Números:** 9 pacotes, 19 dependências entre eles e 1 ciclo, entre `domain.model` e `domain.identification` (`measures/dependencies.md`). O Javalin aparece só em `api.controller` e no `Main`, e o JDBC só em `infrastructure.*` e no `Main`. A maior classe é o `ReservationController`, com 311 linhas, seguida do `ReservationRepository`, com 294.
**Nota:** com problemas

A direção geral é boa:
- As regras não conhecem o Javalin nem o SQL. O `ReservationService` recebe tipos de domínio e devolve `Reservation` (`domain/service/ReservationService.java:31-88`).
- O SQL está todo nos repositórios, com parâmetros.
- A montagem fica num lugar só (`Main.java:37-54`).

Há três defeitos localizados.

1. **O controller faz trabalho do domínio.**
   - A regra "segunda via só para reserva paga" mora no handler (`api/controller/ReservationController.java:229-234`) e não no serviço.
   - A montagem do documento do check-out e da segunda via também está no controller: ele busca o quarto e chama `generateDocument` com dez argumentos (`ReservationController.java:191-204` e `236-249`). A mesma montagem é repetida dentro do `MessageService` (`domain/service/MessageService.java:22-33`).
   - Quem quiser mudar o documento precisa mexer na borda HTTP e no serviço.
2. **O ciclo entre `domain.model` e `domain.identification`.** A `Reservation` guarda uma `GuestIdentification` (`domain/model/Reservation.java:3`), e a `GuestIdentification` usa `RoomType` do modelo (`domain/identification/GuestIdentification.java:3`). O ciclo é pequeno, entre pacotes vizinhos, mas mostra que a geração do documento ficou dentro do tipo de identificação, misturando "quem é o hóspede" com "como o documento se escreve".
3. **O `ReservationController` cresceu demais.** Tem 311 linhas, quase todas de `try/catch` repetidos para converter as exceções em HTTP (`ReservationController.java:94-256`), porque não há um tratador de exceções central no Javalin.

## Design de baixo nível

**Números:**
- **Os três lugares do documento** (e-mail `MessageService.java:22`, check-out `ReservationController.java:193`, segunda via `ReservationController.java:238`) chamam todos `identification.generateDocument(...)`. Nenhum `if` ou `switch` sobre o tipo de hóspede decide o documento.
- **A escolha do tipo** acontece só ao construir a identificação, em dois pontos inevitáveis: a requisição (`ReservationController.java:268-304`) e a linha do banco (`infrastructure/repository/ReservationRepository.java:274-293`).
- **Três subclasses:** Cpf, Cnpj e Passport.

**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede que o documento varie por tipo de hóspede, nos três lugares, com uma quarta variação já anunciada (as agências). O agente pôs a criação do documento num método do próprio tipo de hóspede (`GuestIdentification.java:9-41`), uma das formas que a referência aceita como acerto. Cada subclasse define título, linha do hóspede, formatos de dinheiro e data e a linha extra: o ISS em `CnpjIdentification.java:31-35`, as linhas em inglês em `PassportIdentification.java:22-56`. Uma agência seria uma nova subclasse, e os três lugares não mudam.

O que afasta do ideal:
- **O "documento" é só um `String`.** O título do JSON é recuperado do texto por expressão regular (`api/dto/DocumentResponse.java:31`, `replaceAll(" nº \\d+$", "")`), o que é frágil.
- **O " nº " ficou fixo no método da base** (`GuestIdentification.java:23`), e por isso o booking receipt sai com "nº" em vez de "no.". A variação do título não foi inteira para a subclasse.
- **Cinco getters abstratos que devolvem `null`.** A classe base declara `getCpf`, `getCnpj`, `getCompanyName`, `getPassport` e `getCountry` (`GuestIdentification.java:76-80`), e cada subclasse devolve `null` nos que não se aplicam (por exemplo `CpfIdentification.java:41-59`). Um quarto tipo obriga a mexer em todas as subclasses, nos DTOs e no esquema.

**O exagero.** O `RatePlanFactory` (`domain/rateplan/RatePlanFactory.java:7-14`), com a interface `RatePlan` e duas implementações, é uma fábrica fora da parte do pattern. Mas há dois planos reais que variam desconto e devolução, então a estrutura tem uso: contenção.

**O resto do código.**
- **Duplicação:**
  - A chamada de `generateDocument` com dez argumentos aparece três vezes (`ReservationController.java:193-204`, `238-249`; `MessageService.java:22-33`).
  - A conversão do `id` e o `try/catch` de cada exceção aparecem seis vezes no `ReservationController` (`ReservationController.java:95-102`, `114-121`, `136-143`, `158-165`, `180-187`, `217-224`).
  - O `formatMoney` é idêntico em `CpfIdentification.java:28` e `CnpjIdentification.java:39`.
- **Formato do dinheiro.** `String.format("R$ %,.2f", amount)` depende do locale da JVM e, em `en_US`, produz "R$ 1,159.80". É o defeito que aparece no comportamento.
- **Validações em dobro.** O serviço testa `canConfirm`/`canCheckIn` (`ReservationService.java:98-103`) e a entidade testa de novo, lançando `IllegalStateException` (`domain/model/Reservation.java:65-70`).
- **Erros.** `catch (Exception e)` engole a causa no corpo inválido e no plano de tarifa (`ReservationController.java:35`, `59`). Os repositórios embrulham tudo em `RuntimeException`, que o Javalin devolve como 500 sem o formato `{error, message}`.
- **Tipos.** Estão certos: `BigDecimal` com `HALF_EVEN` em cada passo (`domain/service/PricingService.java:18`, `34`, `43`; `NonRefundableRatePlan.java:10`), `LocalDate` e `enum` para situação, plano e tipo de quarto.

## Testes

**Números:** 5 testes rodaram, 0 falharam, 0 pulados; 5 métodos escritos, num arquivo só. Cobertura: linhas 47 de 925 (5,1%), ramos 9 de 172 (5,2%). Não há testes de `identification`, `controller`, `repository` nem `database` (0% em cada), nenhum pela API e nenhum contra o banco (`measures/tests.md`).
**Nota:** ruim

Os 5 testes (`src/test/java/com/pousadamaralta/PricingServiceTest.java:17-62`) conferem com valores só a parte do preço:
- a diária de alta temporada (299,88);
- a de baixa (249,90);
- a soma de 10 a 12/07 (599,76);
- o desconto NON_REFUNDABLE (89,96);
- o desconto zero do STANDARD.

Nada confere o resto: as situações da reserva, o cancelamento e a devolução, as recusas e os códigos de erro, os relatórios, o e-mail e, sobretudo, a parte nova do cenário (os três documentos, o ISS, o formato em inglês, a regra "exatamente uma identificação").

Pela pergunta da mutação:
- trocar `0.05` por `0.50` no ISS (`CnpjIdentification.java:10`) não derruba nenhum teste;
- trocar "1 night" por "1 nights" (`PassportIdentification.java:30`) também não;
- o formato errado do dinheiro em `pt-BR` e o "nº" do booking receipt passaram justamente porque nenhum teste lê um documento.

Há um `test-api.sh`, mas ele só imprime as respostas com `curl | jq` e não confere nada. Não é teste.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` nas 5 colunas, e as datas em `date`. As conferências acusaram duas coisas:
- a chave estrangeira `reservations(room_number) -> rooms` sem índice;
- nenhum CHECK sobre `status`, `rate_plan_type`, `type` e `channel`, e nenhuma restrição UNIQUE, EXCLUDE ou CHECK.

Não há transações explícitas nem `FOR UPDATE`, e nenhum SQL é montado com `+` (`measures/database.md`).
**Nota:** com problemas

Os dados são guardados certo:
- tipos corretos e `NOT NULL` no que é obrigatório (`infrastructure/database/DatabaseInitializer.java:21-44`);
- a reserva ligada ao quarto por chave estrangeira (`DatabaseInitializer.java:42`);
- SQL sempre com `?` e conexões do pool em try-with-resources (`ReservationRepository.java:47-70`, por exemplo);
- os dados sobrevivem ao reinício (3 de 3 no roteiro).

Os defeitos:
1. **Nada protege contra duas reservas do mesmo quarto na mesma noite.** O serviço consulta os conflitos (`domain/service/ReservationService.java:55-61`) e depois insere (`ReservationService.java:87`), em conexões separadas, sem transação, trava ou restrição de exclusão no banco. O roteiro criou as duas reservas nas 10 rodadas simultâneas de 10. As mudanças de situação também leem e regravam a linha inteira sem trava (`ReservationRepository.java:91-112`), o que permite atualização perdida.
2. **A confirmação não é atômica.** A reserva confirmada é gravada (`ReservationService.java:105`) e depois a mensagem é gravada em outra conexão (`ReservationService.java:108`), sem transação. Se a segunda falhar, a reserva fica paga sem e-mail registrado.
3. **O esquema é criado no código** com `CREATE TABLE IF NOT EXISTS` (`DatabaseInitializer.java:14`, `21`, `47`), sem migrações. O próximo pedido que precisar de uma coluna nova (as agências, por exemplo) não tem como evoluir um banco que já tem dados.
4. **Menores:**
   - a chave estrangeira sem índice;
   - nenhum CHECK nos campos de situação, plano e tipo;
   - nenhum CHECK que garanta "exatamente uma identificação" nas cinco colunas `guest_cpf`, `company_cnpj`, `company_name`, `guest_passport` e `guest_country` (`DatabaseInitializer.java:26-30`).

## Comportamento

**Números:** 75 de 83 conferências passaram (`measures/behavior.md`):
- parte comum: 42 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 05: 29 de 32;
- reservas simultâneas: 1 de 2.

**Nota:** com problemas

O que falhou e o que significa para o dono da pousada:
- **Duas reservas simultâneas do mesmo quarto:** as duas foram criadas nas 10 rodadas de 10. Se dois hóspedes reservarem o mesmo quarto quase no mesmo instante pelo site, a pousada fica com o quarto vendido duas vezes.
- **O dinheiro nos documentos em português:**
  - O e-mail de confirmação trouxe "Total: R$ 599.76" em vez de "R$ 599,76".
  - A fatura trouxe "R$ 779.80" e "ISS retido (5%): R$ 38.99" em vez de "R$ 779,80" e "R$ 38,99". O valor do ISS está certo; só o formato está errado.
  - Conferi à mão com o serviço de pé: o comprovante de CPF sai "Diárias: R$ 1,159.80".
  - A causa é `String.format` com o locale padrão da JVM (`CpfIdentification.java:28`, `CnpjIdentification.java:39`). O comprovante e a fatura só saem como o texto mostra se o servidor estiver configurado em português.
- **O booking receipt** começa com "Booking receipt nº 3" em vez de "Booking receipt no. 3" (`GuestIdentification.java:23`): o estrangeiro recebe um símbolo português no documento em inglês.
- **"amountPaid 0.00".** Na resposta da criação, o JSON trouxe `"amountPaid":0` e `"discount":0` em vez de `0.00`, porque o serviço usa `BigDecimal.ZERO` sem escala (`ReservationService.java:83`, `domain/rateplan/StandardRatePlan.java:9`). O texto pede número com 2 casas, então é um defeito pequeno. Depois de ler do banco, o valor vem `0.00`.
- **"o 201 comporta 3" e "a diária do 201 é 389.90".** As duas falharam só porque o sistema devolve `number` como texto (`"201"`) e o roteiro procura o número 201. Conferi à mão: o `GET /rooms` traz `"capacity":3,"nightlyRate":389.90` para o 201. O texto não diz se o número do quarto é texto ou número, então essa falha não pesa.

Fora do roteiro, a regra "um desses jeitos, e só um" deixa passar o CPF junto com a razão social: a contagem só olha CPF, CNPJ e passaporte (`ReservationController.java:269-279`). Conferi à mão: a criação foi aceita com 201.

## Dúvidas

- **A forma do pattern.** Foi usado um método polimórfico no tipo de hóspede, com Template Method dentro (`GuestIdentification.generateDocument` e as etapas sobrescritas). A referência aceita essa forma como acerto. Pesou a favor que nenhum dos três lugares pergunta o tipo. Pesaram contra, sem mudar a classe, o título recuperado do texto por regex (`DocumentResponse.java:31`) e o "nº" fixo na base (`GuestIdentification.java:23`).
- **A nota do Banco.** Ficou entre `com problemas` e `ruim`: a falta total de proteção contra reserva duplicada, confirmada nas 10 rodadas, é um risco de dado errado. Dei `com problemas` porque o critério da coluna põe a proteção fraca contra duplicação nessa faixa e o resto do esquema está certo.
- **O formato do dinheiro em português.** Ele depende do locale do servidor. Num servidor em `pt_BR`, os documentos sairiam como o texto pede. Contei como defeito, porque o sistema não fixa o formato.
