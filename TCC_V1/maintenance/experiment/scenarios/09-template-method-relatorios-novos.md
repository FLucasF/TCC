O contador e a prefeitura me pediram relatórios novos, e eu aproveitei para mudar uma coisa em todos eles.

O primeiro é o da taxa de turismo, que a prefeitura cobra: R$ 4,50 por hóspede por noite. Ele lista as reservas pagas que começam no mês (as confirmadas, as que já fizeram check-in e as que já fizeram check-out), na mesma ordem do relatório financeiro, com a taxa de cada uma, e no fim o total da taxa. Por exemplo, 2 hóspedes por 3 noites dão R$ 27,00.

O segundo é para a gerência ver qual plano de tarifa traz mais dinheiro. Ele tem uma linha para cada plano, o STANDARD e depois o NON_REFUNDABLE, mesmo que um deles não tenha nenhuma reserva no mês, com a quantidade de reservas e o dinheiro que ficou com a pousada, contando as mesmas reservas que o relatório financeiro conta. No fim vem o total.

E a mudança em todos, nos dois de hoje e nos dois novos, é que eles passam a começar com uma linha de título, com o nome da pousada, o nome do relatório e o período, separados por vírgula, tipo "Pousada Maré Alta,occupancy,2027-07-10". Depois do título continua tudo como é hoje: o cabeçalho, as linhas e o total no fim.

O contador já avisou que vai pedir mais, um de cancelamentos e outro de hóspedes por estado, e a prefeitura muda o valor da taxa todo ano. E no ano que vem todos os relatórios vão ter que terminar com uma linha com a data em que foram tirados; quando o jeito dos relatórios mudar, quero que mude em todos de uma vez.

O pessoal do site combinou comigo os detalhes. O da taxa fica em GET /reports/tourism-tax?month=2027-07, com as colunas reservation, room, check_in, check_out, guests, nights e tax, e a última linha é "total" e o valor. O dos planos fica em GET /reports/rate-plans?month=2027-07, com as colunas rate_plan, reservations e amount, e a última linha é "total", a quantidade e o valor. Os valores vão escritos como no financeiro (27.00), e os nomes dos relatórios no título são occupancy, revenue, tourism-tax e rate-plans.
