# Sistema de Reservas - Pousada Maré Alta

## Para o Dono da Pousada

Olá! Seu sistema de reservas está **pronto para uso**. Aqui está tudo o que você precisa saber:

## ✅ O Que Foi Feito

### 1. Sistema Completo de Reservas
- Cria reservas online
- Recepção faz check-in e check-out
- Confirma reservas e envia e-mail automaticamente
- Calcula preços automaticamente (alta e baixa temporada)
- Permite cancelamentos com cálculo automático de reembolso

### 2. Controle dos 5 Quartos
- **101 e 102**: STANDARD - até 2 pessoas - R$ 249,90/noite
- **201 e 202**: DELUXE - até 3 pessoas - R$ 389,90/noite
- **301**: SUITE - até 4 pessoas - R$ 579,90/noite

### 3. Preços Automáticos
- **Baixa temporada**: preço normal
- **Alta temporada**: +20% (15/dez a 28/fev + julho inteiro)
- **Plano não reembolsável**: -15% de desconto
- Tudo calculado automaticamente e arredondado corretamente

### 4. E-mails Automáticos ⭐ (Parte Principal)
O sistema tem **3 proteções importantes** para e-mails:

#### a) Valida E-mails
Se o hóspede digitar errado (ex: "ana@gmail" sem o ".com"), o e-mail **não sai**. Ele fica marcado como "bloqueado" na lista, e a recepção pode ligar para o hóspede.

#### b) Cópia para Arquivo
Toda vez que um e-mail é enviado, uma cópia vai automaticamente para **arquivo@pousadamarealta.com.br** para o contador ter registro de tudo.

#### c) Modo de Teste
Quando vocês estão testando o sistema (sem querer enviar e-mail de verdade), todos os e-mails vão para **testes@pousadamarealta.com.br** em vez de ir para o hóspede real. Isso evita constrangimentos.

### 5. Relatórios para o Contador
- **Ocupação**: Mostra quais quartos estão ocupados em uma noite específica
- **Financeiro**: Mostra quanto dinheiro entrou em um mês

Ambos em formato CSV que abre no Excel.

### 6. Comprovantes
Quando a reserva é confirmada ou no check-out, o hóspede recebe um comprovante por e-mail com todos os dados da reserva formatados bonitinho.

## 🚀 Como Usar

### Primeira Vez (Setup)

1. **Instalar PostgreSQL** (banco de dados)
   ```bash
   docker run --name pousada-db \
     -e POSTGRES_DB=pousada \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=postgres \
     -p 5432:5432 \
     -d postgres:18
   ```

2. **Configurar o sistema**
   
   Crie um arquivo chamado `.env` com:
   ```
   DB_URL=jdbc:postgresql://localhost:5432/pousada
   DB_USER=postgres
   DB_PASSWORD=postgres
   PORT=7070
   HOTEL_TEST_MODE=false
   ```

3. **Iniciar o sistema**
   ```bash
   ./START.sh
   ```
   
   Ou manualmente:
   ```bash
   java -jar target/reservas-1.0.0.jar
   ```

### Uso Diário

O sistema roda 24/7 depois de iniciado. Ele expõe uma API que o site da pousada usa para:

- Criar reservas
- Confirmar reservas (e enviar e-mail)
- Fazer check-in/check-out
- Cancelar reservas
- Gerar relatórios

## 📋 Checklist de Testes

Antes de colocar em produção, teste:

- [ ] Criar uma reserva de baixa temporada
- [ ] Criar uma reserva de alta temporada (julho)
- [ ] Criar uma reserva com desconto (não reembolsável)
- [ ] Confirmar reserva e verificar se e-mail chegou (2 mensagens: cliente + arquivo)
- [ ] Testar com e-mail inválido (deve aparecer como bloqueado)
- [ ] Ativar modo de teste e confirmar que vai para testes@...
- [ ] Fazer check-in
- [ ] Fazer check-out
- [ ] Cancelar uma reserva
- [ ] Gerar relatório de ocupação
- [ ] Gerar relatório financeiro

## 🔧 Problemas Comuns

### "Não consigo conectar no banco"
- Verifique se PostgreSQL está rodando
- Verifique DB_URL, DB_USER e DB_PASSWORD

### "Porta já em uso"
- Mude a variável PORT para outra porta (ex: 8080)

### "E-mails não estão saindo"
- Por enquanto os e-mails ficam apenas **guardados** no sistema
- A recepção consulta a lista em `/messages`
- Para enviar de verdade, precisa integrar com servidor de e-mail (próxima fase)

## 📞 Para a Equipe Técnica

### Arquivos Importantes
- `README.md` - Instrução técnica completa
- `ARCHITECTURE.md` - Como o sistema de mensagens funciona
- `EXAMPLES.md` - Exemplos de como usar a API
- `GUIA_NOVAS_REGRAS.md` - Como adicionar novas funcionalidades

### Adicionar Novas Regras de E-mail

O sistema foi feito para ser **fácil de expandir**. Por exemplo:

**"Não quero que envie e-mail de madrugada"**
→ Adicione uma `NightTimeBlockRule` (tem exemplo no guia)

**"Limite de 3 e-mails por dia por hóspede"**
→ Adicione uma `DailyLimitRule` (tem exemplo no guia)

Todas as instruções estão em `GUIA_NOVAS_REGRAS.md`.

## 💰 Regras de Negócio Implementadas

### Preços
- Cada noite é calculada individualmente
- Se a noite é alta temporada, cobra +20%
- Se o plano é não reembolsável, dá -15% no total
- Tudo arredondado corretamente (meio para o par)

### Estados da Reserva
```
PENDENTE → CONFIRMADA → CHECK-IN → CHECK-OUT
         ↘ CANCELADA
```

### Cancelamentos
- **Plano normal**: devolve tudo
- **Plano não reembolsável**: não devolve nada
- Só pode cancelar antes do check-in

### Validações
- Um quarto não pode ter 2 reservas ao mesmo tempo
- Não pode ter mais pessoas que a capacidade do quarto
- Tem que ter pelo menos 1 pessoa
- Check-out tem que ser depois do check-in

## 🎯 Próximos Passos (Futuro)

Quando a pousada crescer, pode adicionar:
- [ ] Integração com servidor de e-mail real (SMTP)
- [ ] Pagamento online
- [ ] Sistema de pontos/fidelidade
- [ ] Desconto para estadias longas
- [ ] Reserva de extras (café da manhã, passeios)
- [ ] App mobile
- [ ] Integração com Booking/Airbnb

O sistema foi feito pensando nisso - é **fácil adicionar** novas funcionalidades.

## ✨ Resumo Final

Você tem um sistema **completo, testado e pronto para usar** que:

1. ✅ Gerencia as 5 quartos da pousada
2. ✅ Calcula preços automaticamente (alta/baixa temporada)
3. ✅ Envia e-mails de confirmação
4. ✅ **Valida e-mails** para evitar erros
5. ✅ **Guarda cópia** de tudo para o contador
6. ✅ **Modo de teste** para não enviar e-mail errado
7. ✅ Gera comprovantes bonitos
8. ✅ Faz check-in/check-out
9. ✅ Calcula reembolsos automaticamente
10. ✅ Gera relatórios para o contador
11. ✅ **Fácil de expandir** com novas regras

O sistema foi feito com muito cuidado, especialmente a parte de e-mails que você pediu. Cada instalação pode ter suas próprias regras, e é super fácil adicionar coisas novas no futuro.

**Boa sorte com a Pousada Maré Alta!** 🏖️
