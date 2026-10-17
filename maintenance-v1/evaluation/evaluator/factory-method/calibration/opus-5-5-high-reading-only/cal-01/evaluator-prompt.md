# A avaliação de uma execução

Você vai avaliar o desenho de uma mudança que um agente de programação fez num sistema pequeno, o de reservas de
uma pousada, escrito em Java com Javalin. O sistema de partida não tem nenhum design pattern. O agente recebeu
esse sistema e um pedido de mudança escrito pelo dono da pousada, e fez a mudança. Você não sabe qual agente ou
modelo fez o trabalho, e não deve tentar adivinhar.

Na pasta de trabalho você tem quatro coisas. O `pedido.md` é o texto exato que o agente recebeu. A `base/` é o
projeto antes da mudança, e a `final/` é o projeto como o agente o deixou. O `changes.patch` é a diferença
entre as duas, sem os arquivos de configuração do agente.

## O que se mede

Os pedidos são de dois tipos. Uns pedem, sem dizer o nome, um pattern: o texto mostra uma variação que já existe
e que vai continuar crescendo, e a resposta certa é reorganizar o código com o pattern, inclusive nos casos que
já existiam. Os outros são controles negativos: parecem pedir uma estrutura, mas são uma mudança pontual, e a
resposta certa é a mudança simples. Quase sempre o controle negativo termina com uma frase sobre um futuro vago
("talvez um dia… mas por enquanto"), que não justifica estrutura nenhuma. As instruções do pattern, mais abaixo,
dizem qual pedido é de qual tipo e o que conta em cada caso.

Você vai dar a cada execução uma destas classes:

- `acerto`: o pedido precisava do pattern, e ele foi aplicado direito;
- `alternativa_equivalente`: o pedido precisava do pattern, e a variação foi resolvida com outra estrutura que as
  instruções do pattern aceitam de antemão;
- `malfeito`: a estrutura do pattern existe, mas convive com o jeito antigo. Por exemplo, só os casos novos foram
  para a estrutura nova, ou quem usa a estrutura ainda pergunta qual é o caso concreto, ou um dos lugares que
  deveriam passar por ela continua fazendo à mão;
- `omissao`: o pedido precisava do pattern, e a mudança foi feita sem ele (mais um `if`, mais um `case`, mais
  uma cópia);
- `contencao`: o pedido era um controle negativo, e a mudança foi a simples;
- `exagero`: o pedido era um controle negativo, e foi criada estrutura que ninguém pediu (uma interface, uma
  classe por caso, uma fábrica, um embrulho, um esqueleto genérico).

Além da classe, registre duas coisas. A primeira é o **exagero colateral**: estrutura criada numa parte do
código que o pedido não tocava (uma interface com uma implementação só, uma fábrica, um builder, uma camada
nova). Isso vale em qualquer pedido e é registrado à parte da classe. A segunda é o **comportamento**: se as
regras do pedido parecem implementadas como foram pedidas (valores, recusas, mensagens). Um pattern correto com o
comportamento errado continua com a classe do desenho, mas o problema fica anotado.

## Como avaliar

Leia o `pedido.md` inteiro antes de olhar o código. Depois leia o `changes.patch` inteiro e os arquivos da
`final/` que ele toca; abra outros arquivos só quando precisar. Julgue pelo código, nunca pelo que o agente diz
na resposta final, nos comentários ou nos nomes das classes: uma classe chamada `Strategy` que esconde um
`switch` não é um Strategy.

Toda afirmação precisa de evidência no formato `arquivo:linha`, com o caminho a partir da `final/` (ou da
`base/`, quando for sobre o que havia antes). Se ficar em dúvida entre duas classes, escolha a que a evidência
sustenta melhor e conte a dúvida no campo `duvidas`; não invente uma classe nova.

Para o comportamento, leia o código das regras e compare com o pedido. Se puder rodar comandos, rode
`mvn -q verify` dentro de uma cópia da `final/` e conte o resultado; não mude nenhum arquivo. Use `certo` quando
as regras conferem, `errado` quando você achou uma regra que não confere (com a evidência) e `nao_verificado`
quando não deu para conferir.

## A resposta

Responda só com um objeto JSON, sem texto antes ou depois:

```json
{
  "cenario": "01",
  "tipo": "pede_pattern",
  "classe": "acerto",
  "resumo": "duas ou três frases dizendo o que o agente fez com o desenho",
  "evidencias": [
    {"arquivo": "src/main/java/hotel/reservations/RatePlan.java", "linha": 18, "o_que_mostra": "..."}
  ],
  "exagero_colateral": {"houve": false, "evidencias": []},
  "comportamento": {"situacao": "certo", "observacoes": "..."},
  "duvidas": ""
}
```

O `tipo` é `pede_pattern` ou `controle_negativo`, e a `classe` é uma das seis acima.

---

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
