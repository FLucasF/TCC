# As instruções do pattern: Factory Method

Este avaliador cuida de dois pedidos, que você reconhece pelo texto do `pedido.md`: o do **comprovante para
empresa e para estrangeiro** (cenário 05, que pede o pattern) e o das **crianças na reserva** (cenário 06,
controle negativo).

## O que havia na base

O comprovante (`ReservationReceipt`) é montado com `new` em três lugares do `ReservationService`: `confirm` (que o
manda por e-mail), `checkOut` e `receiptCopy` (a segunda via). Havia um único tipo de comprovante. A reserva
(`Reservation`) e o pedido de reserva (`NewReservation`) são objetos de dados criados direto.

## Cenário 05: o documento por tipo de hóspede (pede o pattern)

O pedido traz três tipos de hóspede (pessoa com CPF, empresa, estrangeiro), cada um com o seu documento (o
comprovante de hoje, a fatura de hospedagem, o *booking receipt* em inglês), que sai nos mesmos três lugares de
hoje, e diz que as agências de turismo vão precisar de um documento próprio. A resposta certa é concentrar a
criação: a escolha de qual documento criar mora num lugar só, e os três lugares pedem o documento a esse lugar.

É **acerto** a forma clássica do Factory Method (um criador com o método de fábrica sobrescrito por tipo) e
também uma fábrica concentrada (uma classe ou um método que escolhe o documento pelo tipo de hóspede) ou um
método polimórfico no próprio tipo de hóspede (`guest.issueDocument(...)`). Conte nas `duvidas` qual das formas
foi usada.

É **omissão** quando o `if` ou o `switch` sobre o tipo de hóspede aparece repetido nos três lugares (mesmo que o
texto de cada documento esteja em funções auxiliares separadas: o que importa é onde se decide qual criar).

É **malfeito** quando a fábrica existe, mas um dos três lugares ainda cria o documento à mão, ou quando a
fábrica devolve sempre o mesmo documento e a escolha acontece depois, fora dela.

O comportamento pedido: a reserva aceita exatamente um jeito de identificar o hóspede (o CPF; ou o CNPJ com a
razão social; ou o passaporte com o país) e recusa nenhum, dois ou um incompleto; a fatura mostra a empresa no
lugar do CPF e o ISS retido de 5% sobre o total (779,80 dá R$ 38,99); o *booking receipt* é em inglês, com
`BRL 1,234.56`, datas `2027-03-10` e "1 night" ou "N nights"; o documento sai igual no e-mail de confirmação, no
check-out e na segunda via, e o JSON dele ganha o campo `text`; a pessoa com CPF recebe o comprovante de hoje.

## Cenário 06: as crianças na reserva (controle negativo)

O pedido acrescenta o número de crianças à reserva. A frase "já pensamos em cobrar alguma coisa das crianças
maiores no futuro, mas por enquanto criança não paga" é a isca.

É **contenção** um campo a mais no pedido de reserva e na reserva, a soma na conferência da capacidade e a linha
nova no comprovante; a reserva continua sendo criada direto.

É **exagero** criar um *builder* ou uma fábrica para a reserva ou para o pedido de reserva, ou uma "política de
preço de criança" (uma interface com uma implementação que cobra zero), sem que ninguém tenha pedido preço.

O comportamento pedido: o campo `children` é opcional (zero quando não vem) e não pode ser negativo; adultos e
crianças juntos não passam da capacidade do quarto; criança não muda o preço; o comprovante ganha "Crianças: N"
logo depois da linha do quarto, só quando há crianças.
