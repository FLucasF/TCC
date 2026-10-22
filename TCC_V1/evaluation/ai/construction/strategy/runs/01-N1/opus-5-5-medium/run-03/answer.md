# Cenário 01

## Visão geral

O agente resolveu os planos de tarifa com Strategy, numa classe abstrata `PlanoDeTarifa`. Cada um dos cinco planos tem a sua subclasse, inclusive os dois que já existiam, `STANDARD` e `NON_REFUNDABLE`. As subclasses são escolhidas por código num mapa do catálogo `PlanosDeTarifa`. O cálculo, a validação e a devolução são pedidos ao plano, e nenhum `switch` ou `if` sobre o plano aparece no serviço, na reserva ou no cálculo. Não há estrutura sobrando fora dessa parte. Conferi as regras e os exemplos do enunciado pela leitura do código, e todos batem. Não achei problema sério. O único ponto fraco é pequeno: o código da empresa, que só o convênio usa, viaja no `Estadia` que todos os planos recebem.

## Como foi implementado, e por que o julgamento

**O projeto.** São três pacotes:
- `dominio`: as regras. Tem a reserva e o ciclo de status em `Reserva`, a estadia em `Estadia`, o dinheiro em `Dinheiro`, a temporada em `Temporada`, o quarto em `Quarto` e `TipoQuarto`, e os planos em `PlanoDeTarifa`, nas cinco subclasses e em `PlanosDeTarifa`.
- `servico`: o `ReservaServico` (criar, confirmar, cancelar, check-in, check-out, segunda via), os quartos em `Pousada`, os e-mails guardados em `CaixaDeSaida`, o `Comprovante` e os `Relatorios` em CSV.
- `web`: as rotas Javalin em `Rotas`, a leitura e validação do JSON em `Pedidos` e o formato da resposta em `Respostas`.

O caminho de uma requisição é este. A rota (`web/Rotas.java:26`) chama `Pedidos.reserva`, que monta um `PedidoDeReserva` e lê o `companyCode` como campo opcional (`web/Pedidos.java:37`). Depois vem `ReservaServico.criar`, que valida as datas e os hóspedes, acha o quarto, pede o plano ao catálogo (`servico/ReservaServico.java:37`) e cria a `Reserva`. No construtor da reserva, o plano calcula a `Cobranca` (`dominio/Reserva.java:26`). Os dados ficam numa lista em memória (`servico/ReservaServico.java:21`). `Rotas` monta os objetos à mão, e `Aplicacao` sobe o sistema na porta de `PORT`, ou na 7070 se ela não vier (`Aplicacao.java:13-14`). O shade gera o jar executável. O agente escreveu dois testes: `PlanosDeTarifaTest`, de unidade, que cobre cada plano, e `ApiTest`, que sobe a API e cobre o fluxo completo, o cancelamento do pacote e os erros.

**A parte que pede o pattern.** `PlanoDeTarifa` (`dominio/PlanoDeTarifa.java:10-43`) define três ganchos e um método final:
- `validar`, que por padrão não faz nada;
- `diarias`, que por padrão soma as noites com a temporada pelo `Estadia`;
- `desconto`, que por padrão é zero;
- `devolucao`, que por padrão devolve tudo;
- `cobrar`, final, que valida, calcula as diárias e o desconto e devolve uma `Cobranca` (linhas 38-42).

Cada plano mora no seu arquivo:
- `PlanoNormal` (`dominio/PlanoNormal.java:3-8`) fica com os padrões.
- `PlanoNaoReembolsavel` dá 15% de desconto e devolve zero (`dominio/PlanoNaoReembolsavel.java:12-19`).
- `PlanoConvenio` guarda o mapa de empresas e percentuais, recusa código ausente ou desconhecido e aplica o desconto da empresa (`dominio/PlanoConvenio.java:8-28`).
- `PlanoFimDeSemana` exige entrada na sexta e duas noites, troca as diárias pelo preço fechado do tipo de quarto e devolve 50% (`dominio/PlanoFimDeSemana.java:9-35`).
- `PlanoEstadiaLonga` exige 7 noites e dá 10% de desconto (`dominio/PlanoEstadiaLonga.java:13-23`).

A escolha é um único ponto: um mapa por código, montado a partir da lista dos cinco planos (`dominio/PlanosDeTarifa.java:10-34`). O cancelamento só chama `plano.devolucao(valorPago)` (`dominio/Reserva.java:49`), e o JSON usa `plano().codigo()` (`web/Respostas.java:53`). Procurei os códigos dos planos e `instanceof` no código todo. Os códigos só aparecem nos construtores das subclasses, e `instanceof` não aparece em lugar nenhum.

**O raciocínio.** É exatamente o que as instruções chamam de acerto:
- uma classe abstrata com uma implementação por plano, os dois antigos inclusive;
- a escolha feita num mapa;
- preço, validação e devolução de cada plano dentro da classe dele;
- quem usa o plano (a `Reserva` e o `ReservaServico`) sem nomear nenhum plano.

Um plano novo é uma subclasse a mais e uma linha no catálogo. Os comportamentos padrão da base, como devolver tudo, são herança comum e não perguntam qual é o plano. Também não há validação de plano fora do plano. A única ressalva é o `codigoEmpresa` dentro de `Estadia` (`dominio/Estadia.java:8`), um dado que só o convênio usa. Ninguém decide nada com ele fora do `PlanoConvenio`, então a ressalva fica nas dúvidas.

No exagero colateral, nada pesou:
- `Cobranca` e `Estadia` são records com uso real;
- `PlanosDeTarifa` é o ponto de seleção do próprio pattern;
- não há interface com uma implementação só, nem fábrica, nem camada que só repassa.

No comportamento, todas as contas do enunciado conferem pela leitura, e as recusas dão 400 com `INVALID_REQUEST`.

## Item a item

**O pattern pedido.** `PlanoDeTarifa` é uma classe abstrata com os ganchos `validar`, `diarias`, `desconto` e `devolucao` e com o `cobrar` final (`dominio/PlanoDeTarifa.java:10-43`). Os cinco planos são subclasses, e os dois antigos também: `dominio/PlanoNormal.java:3`, `dominio/PlanoNaoReembolsavel.java:5`, `dominio/PlanoConvenio.java:6`, `dominio/PlanoFimDeSemana.java:7` e `dominio/PlanoEstadiaLonga.java:5`. A escolha é um mapa por código (`dominio/PlanosDeTarifa.java:10-34`). O cálculo pede ao plano (`dominio/Reserva.java:26`), e o cancelamento também (`dominio/Reserva.java:49`). Não há `switch` nem `if` sobre o plano no serviço (`servico/ReservaServico.java:28-52`) nem em outro lugar. Cada plano tem tudo o que é dele num lugar só, que é o Strategy que o texto pedia.
Conclusão: acerto

**O exagero colateral.** Não há:
- `Cobranca` (`dominio/Cobranca.java:6`) e `Estadia` (`dominio/Estadia.java:8`) são records simples com uso real;
- `CaixaDeSaida`, `Pousada` e `Relatorios` são classes concretas com responsabilidade própria;
- não há interfaces de implementação única, fábricas, builders nem camadas de repasse.
Conclusão: contenção

**O comportamento.** A conferência foi por leitura do código, sem subir o serviço nem chamar a API.
- **Convênio:** 779,80 × 12% = 93,576, que arredonda para R$ 93,58, e o total dá R$ 686,22 (`dominio/PlanoConvenio.java:27`, `dominio/Dinheiro.java:26-28`, `dominio/Cobranca.java:8-10`). Os percentuais da Agrovale (18%) e da Farmavida (25%) estão no mapa (`dominio/PlanoConvenio.java:8-11`).
- **Recusas do convênio:** sem código, ou com código fora da lista, dá 400 com `INVALID_REQUEST` (`dominio/PlanoConvenio.java:20-21`, `dominio/ApiException.java:6,31-32`). Nos outros planos o `companyCode` é ignorado.
- **Pacote:** só é aceito com entrada na sexta e 2 noites, ou seja, saída no domingo (`dominio/PlanoFimDeSemana.java:20-23`). O preço é fechado em R$ 520,00, R$ 820,00 ou R$ 1.200,00, sem temporada e sem desconto (`dominio/PlanoFimDeSemana.java:9-12,28-30`). Quem cancela depois de pagar recebe metade, R$ 410,00 no exemplo (`dominio/PlanoFimDeSemana.java:33-34`).
- **Estadia longa:** abaixo de 7 noites é recusada. Com 7 × 249,90 = 1.749,30, o desconto é R$ 174,93 e o total R$ 1.574,37, e a temporada continua valendo pelo `Estadia.somaDasNoites` (`dominio/PlanoEstadiaLonga.java:15-22`, `dominio/Estadia.java:15-28`).
- **Devolução total:** convênio e estadia longa herdam o padrão de devolver tudo (`dominio/PlanoDeTarifa.java:34-36`).
- **Planos antigos:** o não reembolsável dá 15% de desconto e devolve zero. O normal cobra as noites com a temporada, e o exemplo de R$ 299,88 por noite e R$ 599,76 no total confere. Uma reserva cancelada ainda pendente tem `valorPago` zero e, portanto, nada a devolver (`dominio/Reserva.java:16,49`).

O resto confere pela leitura: o fluxo de status, o comprovante, o e-mail e os relatórios. Um detalhe: a validação do plano roda antes das checagens de capacidade (422) e de quarto ocupado (409) (`servico/ReservaServico.java:40-49`). O enunciado não fixa essa ordem.
Conclusão: segue

## Dúvidas

O `codigoEmpresa` faz parte do `Estadia` (`dominio/Estadia.java:8`), que todos os planos recebem, embora só o convênio use. É um dado de um plano só carregado num objeto comum. Mas ninguém fora do `PlanoConvenio` decide nada com ele, então não muda a classe. A outra dúvida é a recusa de códigos em minúsculas, como "tecnosul" (`dominio/PlanoConvenio.java:20`). O texto diz que o código é o nome em maiúsculas, então a recusa me parece aceitável.
