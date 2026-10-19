# Cenário 01

## Visão geral

O agente resolveu os avisos da equipe com um Observer de manual. Há uma interface de ouvinte, `OuvinteReserva`, e uma implementação para cada interessado: o hóspede, a governança e o financeiro. O `ReservaService` recebe a lista de ouvintes pronta, montada em `Aplicacao.criar()`, e só a percorre na confirmação e no cancelamento de uma reserva confirmada. O serviço não sabe quem é avisado. O e-mail do hóspede também é um ouvinte, como pedem as instruções. O resto do sistema é enxuto: não há interface com uma implementação só, fábrica nem camada que só repassa. Nos testes com `curl`, tudo o que a parte do fim pede apareceu certo, e o que conferi da parte comum também. Não achei problema sério. Há uma escolha estranha e sem efeito: a formatação de datas fica na classe `Dinheiro` (`dominio/Dinheiro.java:32`).

## Como foi implementado, e por que o julgamento

**O projeto.** Há quatro pacotes debaixo de `br.com.marealta`:
- **`dominio`**: o modelo e as regras. O enum `TipoQuarto` guarda a capacidade e a diária, e `Quarto` é um record. `Pousada` tem a lista fixa dos cinco quartos. `PlanoTarifa` é um enum que sabe o desconto e se o plano é reembolsável. `Preco` faz o cálculo noite a noite, com o acréscimo de alta temporada e o arredondamento HALF_EVEN. `Reserva` guarda a máquina de estados e o reembolso. `Comprovante` monta o texto do comprovante. `ReservaService` cria e guarda as reservas e anuncia os eventos. `ErroNegocio` e `CodigoErro` levam o código de erro com o status HTTP.
- **`notificacao`**: `CaixaDeSaida` é a lista em memória das mensagens enviadas; `Mensagem` e `Canal` vêm junto. `AvisoHospede`, `AvisoGovernanca` e `AvisoFinanceiro` são os três ouvintes.
- **`relatorio`**: `Relatorios` gera os dois CSVs a partir de `ReservaService.todas()`.
- **`web`**: `Rotas` registra as rotas do Javalin e o tratamento de erros. `PedidoJson` lê e valida o corpo do POST. `Json` tem os records de saída com os nomes que o site espera.

**O caminho de uma requisição.** Uma chamada como `POST /reservations/{id}/confirmation` entra em `Rotas` (`web/Rotas.java:43-44`) e passa por `ReservaService.confirmar` (`dominio/ReservaService.java:49-54`). Ali, `Reserva.confirmar()` valida o estado e grava o valor pago (`dominio/Reserva.java:39-43`), e depois os ouvintes são avisados. Os dados ficam numa `ArrayList` dentro do serviço (`dominio/ReservaService.java:10`). As mensagens ficam na `CaixaDeSaida` (`notificacao/CaixaDeSaida.java:9`).

**A montagem.** O sistema é montado à mão em `Aplicacao.criar()` (`Aplicacao.java:23-37`), e o `main` lê a variável `PORT`, com 7070 se ela não vier. O jar sobe sozinho graças ao shade com `mainClass`.

**Os testes do agente.** Há 8 testes de integração em `PousadaTest`: cada um sobe a aplicação real numa porta aleatória e a chama por HTTP. Cobrem o preço, a confirmação com os três e-mails, os dois tipos de cancelamento de reserva confirmada, o cancelamento de pendente, o fluxo completo com o relatório de ocupação, os erros e a lista de quartos.

**A parte do fim: avisar a equipe.**
- **O contrato.** `OuvinteReserva` (`dominio/OuvinteReserva.java:7-16`) tem dois eventos com implementação padrão vazia: `reservaConfirmada(Reserva)` e `reservaConfirmadaCancelada(Reserva)`. Cada ouvinte sobrescreve só o que lhe interessa.
- **O serviço.** `ReservaService` recebe `List<OuvinteReserva>` no construtor e guarda uma cópia imutável (`dominio/ReservaService.java:12-15`). Na confirmação, faz `ouvintes.forEach(o -> o.reservaConfirmada(reserva))` (`:52`). No cancelamento, guarda se a reserva estava confirmada antes de cancelar e só então dispara `reservaConfirmadaCancelada` (`:58-61`). É assim que a reserva pendente cancelada não avisa ninguém. Não há no serviço nenhuma referência a endereço, assunto ou texto de e-mail.
- **O hóspede.** `AvisoHospede` (`notificacao/AvisoHospede.java:15-23`) monta o e-mail com o comprovante e ouve só a confirmação.
- **A governança.** `AvisoGovernanca` (`notificacao/AvisoGovernanca.java:17-34`) ouve os dois eventos e manda as datas e o número de hóspedes.
- **O financeiro.** `AvisoFinanceiro` (`notificacao/AvisoFinanceiro.java:17-30`) ouve os dois eventos. A regra "só avisa o reembolso quando há algo a devolver" mora no próprio ouvinte (`:25`), não no serviço.
- **O registro.** Os três ouvintes são registrados num só lugar, `Aplicacao.java:28-31`, com o comentário de que quem for avisado entra só nessa lista. Avisar o restaurante ou o transfer seria criar uma classe nova e uma linha nessa lista.

**Por que o julgamento.** O desenho bate com o que as instruções chamam de acerto:
- há interface de ouvinte, uma implementação por interessado, e o serviço percorre uma lista que vem de fora;
- o hóspede também é ouvinte;
- a confirmação e o cancelamento passam os dois pela lista.

Não há nenhum dos sinais de malfeito ou omissão. O serviço não monta e-mail da equipe, o cancelamento não manda nada à mão, e não existe um notificador central que conheça todos os destinos. A única decisão que fica no serviço é *qual* evento anunciar: o cancelamento de pendente não gera evento. É uma decisão sobre o fato que aconteceu, não sobre quem é avisado, e o enunciado a pede.

**O exagero colateral.** Procurei e não achei:
- as classes separadas (`Preco`, `Comprovante`, `Relatorios`, `PedidoJson`, `Json`) têm lógica própria;
- não há interface para repositório, fábrica nem builder;
- `Rotas` faz a ponte HTTP e não é uma camada que só repassa.

**O comportamento.** Tudo o que testei conferiu, e está detalhado no item a seguir.

## Item a item

**O enunciado (pede o pattern).**
- **Ouvintes e serviço.** Os três interessados implementam `OuvinteReserva` (`notificacao/AvisoHospede.java:7`, `notificacao/AvisoGovernanca.java:7`, `notificacao/AvisoFinanceiro.java:7`). `ReservaService` recebe a lista de fora (`dominio/ReservaService.java:12-15`) e a percorre na confirmação (`:52`) e no cancelamento de reserva confirmada (`:60-61`). Não conhece destinatários, assuntos nem textos.
- **Registro.** O registro fica na montagem (`Aplicacao.java:28-31`).
- **Hóspede.** O hóspede é ouvinte como os outros (`notificacao/AvisoHospede.java:15-23`).
- **Reembolso zero.** O filtro do financeiro fica dentro do ouvinte (`notificacao/AvisoFinanceiro.java:25`).

É o Observer que o enunciado precisava: os interessados novos (restaurante, transfer) entram sem mexer no serviço.
Conclusão: acerto

**O exagero colateral.** Não há:
- cada classe fora dos avisos tem trabalho próprio: o cálculo em `dominio/Preco.java:11-20`, o texto em `dominio/Comprovante.java:16-29`, os CSVs em `relatorio/Relatorios.java:23-52` e a validação do corpo em `web/PedidoJson.java:19-38`;
- não há interface com uma implementação só, fábrica, builder nem camada que só repassa;
- a montagem é feita à mão, em `Aplicacao.java:23-37`.

O único ponto estranho não é exagero: a formatação de datas está na classe `Dinheiro` (`dominio/Dinheiro.java:32`).
Conclusão: contenção

**O comportamento.** Fiz o build numa cópia em `/tmp/av`, subi o jar sombreado `reservas-1.0.0.jar` na porta 7070 e chamei a API com `curl`.

*Avisos (parte do fim):*
- **Confirmação.** Confirmei as reservas 1 (201 DELUXE, STANDARD), 2 (101, julho) e 3 (301, NON_REFUNDABLE). Cada uma gerou três e-mails:
  - o do hóspede, com o assunto "Reserva confirmada - Pousada Maré Alta" e o texto e o comprovante idênticos ao enunciado ("Comprovante de reserva nº 1 … Diárias: R$ 779,80 / Total: R$ 779,80"). Na 3 o texto traz "1 diária" e a linha "Desconto: R$ 104,38";
  - o da governança, em `governanca@pousadamarealta.com.br`, com o assunto "Reserva confirmada - quarto 201" e a entrada, a saída e os hóspedes;
  - o do financeiro, em `financeiro@pousadamarealta.com.br`, com o assunto "Pagamento recebido - reserva 1" e "R$ 779,80".
- **Milhar no valor.** Uma SUITE de 20 a 23/12/2027 deu R$ 2.087,64 no aviso do financeiro, com o milhar certo.
- **Cancelamento reembolsável.** Cancelar a 2, confirmada e STANDARD, deu `refund` 599.76, o aviso "Reserva cancelada - quarto 101" com as datas liberadas e "Reembolso - reserva 2" com R$ 599,76.
- **Cancelamento não reembolsável.** Cancelar a 3, confirmada e NON_REFUNDABLE, deu `refund` 0.00 e só o aviso da governança, sem o do financeiro, como pedido.
- **Cancelamento de pendente.** Cancelar a 4, que estava pendente, não gerou mensagem: a lista ficou em 12. Cancelar de novo deu 409 OPERATION_NOT_ALLOWED.

*Parte comum:*
- **Preços.** O exemplo do enunciado (STANDARD de 10/07 a 12/07/2027) deu 599.76. O desconto de 15% sobre 695.88 deu 104.38, e o total, 591.50.
- **Ciclo da reserva.** O check-in e o check-out devolveram o comprovante. A segunda via de reserva paga funcionou, e a de reserva pendente cancelada deu 409.
- **Erros.**
  - 400 INVALID_REQUEST para corpo que não é JSON, campo faltando, saída igual à entrada, `guests` 0 e data ou mês fora do formato;
  - 404 ROOM_NOT_FOUND e 404 RESERVATION_NOT_FOUND;
  - 409 ROOM_UNAVAILABLE para noite sobreposta;
  - 422 CAPACITY_EXCEEDED;
  - um quarto de reserva cancelada voltou a ficar disponível.
- **Relatórios.** Os dois vêm em `text/csv; charset=utf-8`.
  - Ocupação: certa, com quem entra contando e quem sai não, e as linhas `total,1/5` e `total,0/5`.
  - Financeiro de 2027-07: inclui a NON_REFUNDABLE cancelada (591.50), exclui a reembolsada e soma `total,1827.14`.

*Testes do agente:* o `mvn -q verify` passou, com 8 testes, 0 falhas e 0 erros. Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Uma observação só: o evento de cancelamento só existe para reserva que estava confirmada (`reservaConfirmadaCancelada`, `dominio/ReservaService.java:58-61`). Um interessado futuro que queira saber de todo cancelamento, inclusive de pendentes, exigiria mexer na interface e no serviço. É uma escolha de granularidade do evento, coerente com o enunciado atual, e não afeta a classe.
