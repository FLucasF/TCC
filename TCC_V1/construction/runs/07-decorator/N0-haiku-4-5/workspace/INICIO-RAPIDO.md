# Início Rápido - Sistema de Reservas Pousada Maré Alta

Bem-vindo! Este guia te leva do zero ao sistema funcionando em 5 minutos.

## O que foi entregue

✅ **Sistema completo de reservas** em Java 21 com Javalin 6.7.0  
✅ **API REST** com 9 endpoints  
✅ **Sistema de emails** com 3 camadas de proteção  
✅ **Relatórios em CSV** (ocupação e receita)  
✅ **20 testes unitários** (todos passando)  
✅ **Documentação completa**  

## Compilação (30 segundos)

```bash
cd /workspace
mvn clean verify
```

✅ Se ver "BUILD SUCCESS" → pronto para usar!

## Executar o Sistema (10 segundos)

### Modo Normal (Produção)
```bash
java -jar target/reservas-1.0.0.jar
```

Servidor responde em: `http://localhost:7070`

### Modo Teste (Protege clientes reais)
```bash
HOTEL_TEST_MODE=true java -jar target/reservas-1.0.0.jar
```

## Testar em 1 Minuto

Abra outro terminal e execute:

```bash
# 1. Listar quartos
curl -s http://localhost:7070/rooms | jq '.'

# 2. Criar uma reserva
curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq '.id, .total'

# 3. Confirmar a reserva
curl -s -X POST http://localhost:7070/reservations/1/confirmation | jq '.status'

# 4. Ver emails enviados
curl -s http://localhost:7070/messages | jq '.[].status'
```

Pronto! O sistema está funcionando.

## Estrutura do Projeto

```
/workspace/
├── pom.xml                          # Configuração Maven
├── README.md                        # Documentação principal
├── INICIO-RAPIDO.md                 # Este arquivo
├── MENSAGENS.md                     # Sistema de emails
├── ARQUITETURA.md                   # Detalhes técnicos
├── DEPLOYMENT.md                    # Como deployar
├── TESTE-INTEGRADO.md               # Cenários completos
├── test-api.sh                      # Script de testes
├── target/
│   └── reservas-1.0.0.jar           # JAR executável (12MB)
└── src/
    ├── main/java/com/pousadamarealta/
    │   ├── model/                   # Entidades (Room, Reservation, etc)
    │   ├── service/                 # Lógica de negócio
    │   ├── dto/                     # Request/Response JSON
    │   ├── util/                    # Utilitários (Money, Dates, etc)
    │   └── Main.java                # Aplicação Javalin
    └── test/java/com/pousadamarealta/
        ├── MoneyTest.java           # Testes de cálculo
        ├── EmailValidatorTest.java  # Testes de email
        ├── MessageServiceTest.java  # Testes de mensagens
        ├── ReservationServiceTest.java
        └── ReportServiceTest.java
```

## Quartos Disponíveis

| Quarto | Tipo | Capacidade | Preço/Noite |
|--------|------|-----------|------------|
| 101, 102 | STANDARD | 2 pessoas | R$ 249,90 |
| 201, 202 | DELUXE | 3 pessoas | R$ 389,90 |
| 301 | SUITE | 4 pessoas | R$ 579,90 |

**Alta temporada** (15/12 a 29/02 e julho): +20%

## API em 30 Segundos

### Criar Reserva
```bash
POST /reservations
{
  "roomNumber": 101,
  "guestName": "Nome do Hóspede",
  "guestEmail": "email@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"  // ou "NON_REFUNDABLE"
}
```

### Confirmar Pagamento
```bash
POST /reservations/{id}/confirmation
```

### Check-in / Check-out
```bash
POST /reservations/{id}/check-in
POST /reservations/{id}/check-out
```

### Relatórios
```bash
GET /reports/occupancy?date=2027-03-10
GET /reports/revenue?month=2027-03
```

### Mensagens Enviadas
```bash
GET /messages
```

[Veja README.md para documentação completa da API]

## Recursos Importantes

### ✅ Validação de Email
```
Válido: ana@gmail.com ✓
Inválido: ana@gmail ✗ (sem ponto)
Inválido: ana.gmail.com ✗ (sem @)
```

Emails inválidos são bloqueados automaticamente.

### ✅ Modo Teste (Proteção para Clientes Reais)
```bash
# Todos os emails redirecionados para testes@pousadamarealta.com.br
HOTEL_TEST_MODE=true java -jar target/reservas-1.0.0.jar
```

Perfeito para testar sem enviar emails reais.

### ✅ Arredondamento Correto
- Usa "meio para par" (banker's rounding)
- Cada cálculo é arredondado corretamente
- Exemplo: 249,90 × 1,2 = 299,88

### ✅ Cópia para Arquivo
- Todo email confirmado gera cópia para `arquivo@pousadamarealta.com.br`
- Contador tem registro de tudo que foi enviado
- (Desativado em modo teste)

## Próximas Etapas

### Curto Prazo
1. Instalar em servidor
2. Testar com hóspedes reais (em modo teste primeiro)
3. Ajustar configurações de email (SMTP real)

### Médio Prazo
1. Implementar persistência em banco de dados
2. Adicionar autenticação/autorização
3. Interface web/admin para gerenciamento

### Longo Prazo
1. Integração com plataformas de pagamento
2. Notificações SMS
3. Painel de gerenciamento visual

## Solução de Problemas

### "Port already in use"
```bash
# Use outra porta
PORT=8080 java -jar target/reservas-1.0.0.jar
```

### "BUILD FAILED"
```bash
# Limpar e recompilar
mvn clean verify
```

### Testes falhando
```bash
# Rodar testes novamente
mvn test
```

## Documentação Completa

| Documento | Descrição |
|-----------|-----------|
| **README.md** | Visão geral, API, uso |
| **MENSAGENS.md** | Sistema de emails em detalhes |
| **ARQUITETURA.md** | Estrutura técnica e padrões |
| **DEPLOYMENT.md** | Como deployar em produção |
| **TESTE-INTEGRADO.md** | Cenários de teste completos |

## Exemplo Completo: Reserva do Início ao Fim

```bash
#!/bin/bash

# 1. Criar reserva
RES=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq '.id')

echo "Reserva criada: #$RES"

# 2. Confirmar (pagamento)
curl -s -X POST http://localhost:7070/reservations/$RES/confirmation > /dev/null
echo "✓ Confirmada"

# 3. Check-in
curl -s -X POST http://localhost:7070/reservations/$RES/check-in > /dev/null
echo "✓ Check-in"

# 4. Check-out (gera comprovante)
curl -s -X POST http://localhost:7070/reservations/$RES/check-out
echo ""

# 5. Segunda via do comprovante
curl -s http://localhost:7070/reservations/$RES/document
```

## Suporte

Dúvidas? Veja:
1. README.md - Visão geral
2. MENSAGENS.md - Sistema de emails
3. ARQUITETURA.md - Como funciona por dentro
4. TESTE-INTEGRADO.md - Exemplos práticos

## Checklist Final

- [ ] Código compilado (`mvn verify` passou)
- [ ] JAR criado (`target/reservas-1.0.0.jar` existe)
- [ ] Servidor rodando (`java -jar target/reservas-1.0.0.jar`)
- [ ] API respondendo (`curl http://localhost:7070/rooms` retorna JSON)
- [ ] Teste de reserva funcionando

Tudo pronto? Sua Pousada Maré Alta está no ar! 🎉

---

**Próximo passo**: Leia [DEPLOYMENT.md](DEPLOYMENT.md) para colocar em produção.
