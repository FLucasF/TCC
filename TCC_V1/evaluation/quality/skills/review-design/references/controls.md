# Os controles negativos

Nestes cenários, o texto pede uma mudança pontual (um número, um texto, um campo, uma coluna) e termina com uma frase
vaga sobre o futuro ("talvez um dia... mas por enquanto"). A frase é proposital: ela tenta o agente a construir a
estrutura para o que ninguém pediu.

- `contenção`: a mudança pequena, no lugar onde a coisa já mora. Uma constante, um dado, um campo, um método privado ou
  uma lista simples de valores não são estrutura nova.
- `exagero`: uma estrutura criada para a variação que ninguém pediu, bem ou mal feita: uma interface, uma hierarquia de
  classes, uma fábrica, um *builder*, um embrulho, uma classe por caso, algo configurável sem que o texto peça. Também
  é exagero refatorar para um pattern uma parte da base que o pedido não tocava.

Julgue pelo código, nunca pelos nomes. Na dúvida, conte a dúvida na seção "Dúvidas" e dê a classe que a evidência
sustenta melhor.

## 02: a alta temporada mudou

- A frase que tenta: "um acréscimo diferente para cada temporada".
- `contenção`: a constante vira 0,25, e as datas da Páscoa entram como dado (uma lista de períodos, ou duas condições a
  mais no `isHighSeason`).
- `exagero`: uma classe ou estratégia por temporada; uma interface de regra de temporada; um acréscimo por temporada,
  parametrizado, sem que ninguém peça.

## 04: o e-mail de confirmação

- A frase que tenta: "talvez um dia a gente mande por WhatsApp também".
- `contenção`: muda o assunto e o texto em `confirmationEmailBody` (ou onde o texto estiver).
- `exagero`: um canal de mensagem abstrato, um ouvinte de confirmação ou um modelo de mensagem com várias
  implementações.

## 06: crianças na reserva

- A frase que tenta: "cobrar alguma coisa das crianças maiores no futuro".
- `contenção`: um campo a mais na reserva e no pedido, a soma na conferência da capacidade, a linha no comprovante e a
  coluna nova no banco; a reserva continua criada direto.
- `exagero`: um *builder* ou uma fábrica para a reserva ou para o pedido de reserva; uma política de preço de criança
  sem preço nenhum.

## 08: assinatura nas mensagens

- A frase que tenta: "talvez a gente troque a assinatura no Natal".
- `contenção`: a assinatura acrescentada num lugar só (no `EmailSender.send`, por onde passa toda mensagem, ou no texto
  da confirmação, que hoje é a única mensagem).
- `exagero`: um embrulho (Decorator) só para a assinatura; uma assinatura por época do ano.

## 10: o nome do hóspede na ocupação

- A frase que tenta: "talvez um dia a recepção peça o telefone do hóspede também".
- `contenção`: uma coluna a mais no `OccupancyReport`, e a troca da vírgula por espaço.
- `exagero`: um esqueleto genérico de relatórios, ou um sistema de colunas configuráveis.
