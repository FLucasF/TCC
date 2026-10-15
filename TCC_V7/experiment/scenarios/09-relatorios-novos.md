## Relatórios novos, e todos com título

O contador e a prefeitura pediram relatórios novos, e aproveitei para mudar uma coisa em todos eles.

**Taxa de turismo** (`GET /reports/tourism-tax?month=2027-07`). A prefeitura cobra R$ 4,50 por hóspede por noite. O relatório lista as reservas pagas (confirmadas, com check-in feito ou com check-out feito) que começam no mês, na mesma ordem do relatório financeiro, com a taxa de cada uma, e no fim o total da taxa. Por exemplo, 2 hóspedes por 3 noites dão R$ 27,00.

**Por plano de tarifa** (`GET /reports/rate-plans?month=2027-07`). Para a gerência ver qual plano traz mais dinheiro. Uma linha por plano (`STANDARD` e `NON_REFUNDABLE`, nessa ordem, mesmo que um deles não tenha nenhuma reserva), com a quantidade de reservas e o dinheiro que ficou com a pousada, contando as mesmas reservas do relatório financeiro; no fim, o total.

**Todos os relatórios**, os dois de hoje e os dois novos, passam a começar com uma linha de título: o nome da pousada, o nome do relatório e o período, separados por vírgula, como em `Pousada Maré Alta,occupancy,2027-07-10`. Os nomes são `occupancy`, `revenue`, `tourism-tax` e `rate-plans`. Depois do título, tudo como hoje: o cabeçalho, as linhas e a linha do total.

O contador já avisou que vai pedir mais (um de cancelamentos, outro de hóspedes por estado), e a prefeitura muda o valor da taxa todo ano.

**Para o pessoal do site:** as colunas da taxa de turismo são `reservation,room,check_in,check_out,guests,nights,tax`, e a última linha é `total,<valor>`. As do relatório por plano são `rate_plan,reservations,amount`, e a última linha é `total,<quantidade>,<valor>`. Os valores vão escritos como no relatório financeiro (`27.00`).
