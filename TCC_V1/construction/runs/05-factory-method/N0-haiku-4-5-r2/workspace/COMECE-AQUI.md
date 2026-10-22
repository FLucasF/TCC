# 🏖️ COMECE AQUI - Pousada Maré Alta

## ⚡ 5 Minutos para Começar

### Passo 1: Compilar (1 min)
```bash
cd /workspace
mvn clean package
```

✅ Resultado: `target/mare-alta-1.0.0.jar` (7.2 MB)

### Passo 2: Iniciar Banco de Dados (1 min)
```bash
docker run --name marealta-postgres \
  -e POSTGRES_USER=pousada \
  -e POSTGRES_PASSWORD=senha123 \
  -e POSTGRES_DB=marealta \
  -p 5432:5432 \
  postgres:18
```

✅ Resultado: PostgreSQL rodando

### Passo 3: Executar Aplicação (1 min)
```bash
DB_URL="jdbc:postgresql://localhost:5432/marealta" \
DB_USER="pousada" \
DB_PASSWORD="senha123" \
PORT=7070 \
java -jar target/mare-alta-1.0.0.jar
```

✅ Resultado: API rodando em http://localhost:7070

### Passo 4: Testar (1 min)
```bash
curl http://localhost:7070/rooms | jq .
```

✅ Resultado: Ver os 5 quartos

### Passo 5: Criar Primeira Reserva (1 min)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .
```

✅ Resultado: Reserva criada com ID

---

## 📚 O que você tem

### ✅ Sistema Completo
- [x] 11 endpoints REST
- [x] 3 tipos de identificação (CPF, CNPJ, Passaporte)
- [x] 5 quartos pré-cadastrados
- [x] Tarifas com desconto e alta temporada
- [x] Documentos em 3 formatos (português x2, inglês)
- [x] Emails automáticos
- [x] Relatórios em CSV
- [x] Arredondamento preciso (banker's rounding)

### ✅ Banco de Dados
- [x] PostgreSQL 18
- [x] 3 tabelas (rooms, reservations, messages)
- [x] Schema automático

### ✅ Build & Deploy
- [x] JAR executável (7.2 MB)
- [x] Docker Compose
- [x] Scripts de inicialização
- [x] Testes unitários

### ✅ Documentação
- [x] 8 documentos MD
- [x] Exemplos de requisições/respostas
- [x] Guias rápidos
- [x] Referência técnica

---

## 🎯 Próximas Ações

### Para Explorar a API
👉 Veja: [EXEMPLOS.md](EXEMPLOS.md)

### Para Entender o Sistema
👉 Veja: [SISTEMA.md](SISTEMA.md)

### Para Detalhes do Banco
👉 Veja: [BANCO-DADOS.md](BANCO-DADOS.md)

### Para Documentação Completa
👉 Veja: [README.md](README.md)

### Para Checklist Final
👉 Veja: [ENTREGA.md](ENTREGA.md)

---

## 🚀 Atalho Rápido (Usar Docker)

Se quiser tudo automático:

```bash
chmod +x run.sh
./run.sh
```

O script vai:
1. Compilar
2. Parar containers antigos
3. Iniciar PostgreSQL
4. Iniciar a aplicação
5. Tudo automático! ✅

---

## 🧪 Testar Tudo

Se quiser rodar todos os testes:

```bash
chmod +x test-api.sh
./test-api.sh
```

Vai testar:
- Listar quartos
- Criar reservas (CPF, CNPJ, Passaporte)
- Confirmar
- Check-in / Check-out
- Cancelamento
- Relatórios
- Mensagens

---

## 📋 O que Funciona

### ✅ Criar Reserva
```
POST /reservations
→ Retorna reserva com ID
```

### ✅ Confirmar Pagamento
```
POST /reservations/{id}/confirmation
→ Envia email automático
→ Guarda na história
```

### ✅ Check-in & Check-out
```
POST /reservations/{id}/check-in
POST /reservations/{id}/check-out
→ Retorna documento
```

### ✅ Cancelar
```
POST /reservations/{id}/cancellation
→ Reembolsa (ou não, dependendo do plano)
```

### ✅ Documentos
```
GET /reservations/{id}/document
→ Comprovante / Fatura / Booking Receipt
```

### ✅ Relatórios
```
GET /reports/occupancy?date=2027-07-10
GET /reports/revenue?month=2027-07
→ CSV pronto
```

---

## 💰 Exemplo de Cálculo

**Quartos DELUXE (R$ 389,90) em março (1-2 noites)**

### Plano STANDARD
```
Noite 1: R$ 389,90
Noite 2: R$ 389,90
Subtotal: R$ 779,80
Desconto: R$ 0,00
TOTAL: R$ 779,80
```

### Plano NON_REFUNDABLE
```
Noite 1: R$ 389,90
Noite 2: R$ 389,90
Subtotal: R$ 779,80
Desconto (15%): R$ 116,97
TOTAL: R$ 662,83
```

### Com CNPJ (empresa)
```
TOTAL: R$ 662,83
ISS retido (5%): R$ 33,14
```

### Em julho (alta temporada +20%)
```
Noite 1: R$ 389,90 × 1,20 = R$ 467,88
Noite 2: R$ 389,90 × 1,20 = R$ 467,88
Subtotal: R$ 935,76
Desconto (15%, se NON_REFUNDABLE): R$ 140,36
TOTAL: R$ 795,40
```

---

## 🔐 Segurança

- ✅ Validações rigorosas
- ✅ Erro handling robusto
- ✅ Sem SQL injection (JDBC parameterizado)
- ✅ Sem XSS (JSON, não HTML)
- ✅ Sem hardcoded secrets

---

## 📞 Problemas?

### Porta já em uso
```bash
PORT=8080 java -jar target/mare-alta-1.0.0.jar
```

### PostgreSQL não conecta
```bash
# Verifique se está rodando
docker ps | grep postgres

# Veja os logs
docker logs marealta-postgres
```

### Compilação falha
```bash
# Limpe e tente novamente
mvn clean
mvn package -DskipTests
```

---

## 🎓 Para Aprender Mais

| Documento | Tempo | Objetivo |
|-----------|-------|----------|
| [EXEMPLOS.md](EXEMPLOS.md) | 10 min | Ver todas as requisições/respostas |
| [SISTEMA.md](SISTEMA.md) | 10 min | Entender arquitetura |
| [BANCO-DADOS.md](BANCO-DADOS.md) | 10 min | Detalhes do schema |
| [README.md](README.md) | 20 min | Documentação completa |

---

## ✨ Resumo

| Item | Status |
|------|--------|
| **Implementação** | ✅ 100% |
| **Testes** | ✅ Passando |
| **Documentação** | ✅ Completa |
| **Build** | ✅ SUCCESS |
| **Deploy** | ✅ Pronto |

---

## 🚀 Você Está Pronto!

```bash
# Compilar
mvn clean package

# Executar
java -jar target/mare-alta-1.0.0.jar

# Ou com Docker
./run.sh

# Testar
./test-api.sh
```

**Sucesso!** 🎉

Sistema da Pousada Maré Alta está rodando em http://localhost:7070
