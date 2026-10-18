No sistema antigo tivemos três problemas com os e-mails que a pousada manda, e cada um pede uma regra.

O primeiro é e-mail digitado errado. O hóspede escreve "ana@gmail" ou "ana.gmail.com", e a mensagem vai para lugar nenhum. Mensagem para endereço inválido não deve ser enviada: ela tem que aparecer na lista de mensagens como bloqueada, para a recepção ligar para o hóspede. Para mim, um endereço é válido quando tem um único @, alguma coisa antes dele e, depois dele, um nome com pelo menos um ponto no meio, tipo ana@gmail.com.

O segundo é o arquivo. O contador quer cópia de tudo o que a pousada manda, então toda mensagem enviada tem que gerar uma cópia para arquivo@pousadamarealta.com.br, com o mesmo texto e com o assunto começando com "[Cópia] ". Mensagem bloqueada não gera cópia.

O terceiro é o mais grave: quando o pessoal do site testava o sistema antigo, os e-mails iam para hóspedes de verdade! Quando o sistema estiver rodando em modo de teste, nenhuma mensagem pode sair para o endereço real. Ela vai para testes@pousadamarealta.com.br, com o assunto começando com "[TESTE] " e com uma primeira linha a mais no texto, "Destinatário original: " seguido do endereço que seria o certo. No modo de teste não precisa fazer a cópia para o arquivo, e o endereço inválido continua sendo bloqueado do mesmo jeito (a mensagem bloqueada fica com o endereço original).

E não acaba aí. Já pediram para não mandar mensagem de madrugada e para ter um limite de mensagens por hóspede por dia, e cada regra dessas pode valer sempre, só no modo de teste ou só fora dele.

O pessoal do site pediu três detalhes. Cada mensagem da lista (o GET /messages) tem também um campo `status`, que é SENT ou BLOCKED. A cópia para o arquivo aparece na lista como outra mensagem, logo depois da original. E o modo de teste é ligado pela variável de ambiente HOTEL_TEST_MODE=true; sem ela, o sistema funciona normalmente.
