Oi! Sou o dono da Pousada Maré Alta, uma pousada pequena no litoral. O sistema de reservas que a gente usa está nesta pasta. Ele funciona bem, e quem fez deixou um README explicando como ele é.

Preciso de uma mudança, que explico abaixo.

Tivemos três problemas com os e-mails que a pousada manda, e cada um pede uma regra.

O primeiro é e-mail digitado errado. O hóspede escreve "ana@gmail" ou "ana.gmail.com", e a mensagem vai para lugar nenhum. A partir de agora, mensagem para endereço inválido não deve ser enviada: ela tem que aparecer na lista de mensagens como bloqueada, para a recepção ligar para o hóspede. Para mim, um endereço é válido quando tem um único @, alguma coisa antes dele e, depois dele, um nome com pelo menos um ponto no meio, tipo ana@gmail.com.

O segundo é o arquivo. O contador quer cópia de tudo o que a pousada manda, então toda mensagem enviada tem que gerar uma cópia para arquivo@pousadamarealta.com.br, com o mesmo texto e com o assunto começando com "[Cópia] ". Mensagem bloqueada não gera cópia.

O terceiro é o mais grave: quando o pessoal do site testa o sistema, os e-mails estão indo para hóspedes de verdade! Quando o sistema estiver rodando em modo de teste, nenhuma mensagem pode sair para o endereço real. Ela vai para testes@pousadamarealta.com.br, com o assunto começando com "[TESTE] " e com uma primeira linha a mais no texto, "Destinatário original: " seguido do endereço que seria o certo. No modo de teste não precisa fazer a cópia para o arquivo, e o endereço inválido continua sendo bloqueado do mesmo jeito (a mensagem bloqueada fica com o endereço original).

E não acaba aí. Já pediram para não mandar mensagem de madrugada e para ter um limite de mensagens por hóspede por dia, e cada regra dessas pode valer sempre, só no modo de teste ou só fora dele. E cada instalação do sistema pode querer outra combinação: a segunda pousada que a gente vai abrir, por exemplo, não quer a cópia para o arquivo.

O pessoal do site pediu três detalhes. Cada mensagem da lista (o GET /messages) ganha um campo `status`, que é SENT ou BLOCKED. A cópia para o arquivo aparece na lista como outra mensagem, logo depois da original. E o modo de teste é ligado pela variável de ambiente HOTEL_TEST_MODE=true; sem ela, o sistema funciona normalmente.

Tudo o que funciona hoje tem que continuar funcionando, menos o que eu pedi para mudar. Quando terminar, o `mvn verify` precisa passar. Não precisa me perguntar nada: se alguma coisa não estiver clara, decida do jeito que achar mais sensato e siga em frente.
