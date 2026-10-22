# Notas de Implementação

## O Que Foi Implementado

Sistema completo de reservas para a Pousada Maré Alta com:

1. **Gestão de Quartos** (5 quartos predefinidos)
   - 101, 102: STANDARD (2 pessoas) - R$ 249,90
   - 201, 202: DELUXE (3 pessoas) - R$ 389,90
   - 301: SUITE (4 pessoas) - R$ 579,90

2. **Gestão de Reservas** (6 estados)
   - PENDING: Reserva criada, aguardando confirmação
   - CONFIRMED: Hóspede pagou
   - CHECKED_IN: Hóspede já está no quarto
   - CHECKED_OUT: Hóspede saiu
   - CANCELLED: Reserva cancelada

3. **Cálculo Inteligente de Preços**
   - Alta temporada: 15/12 a 28/02 + julho (20% acréscimo)
   - Plano STANDARD: Sem desconto, reembolso total
   - Plano NON_REFUNDABLE: 15% desconto, sem reembolso
   - Arredondamento "meio para o par" em cada etapa

4. **Três Tipos de Documento**
   - CPF: Comprovante em português
   - CNPJ: Fatura com ISS retido (5%)
   - Passaporte: Booking receipt em inglês

5. **API REST Completa** (13 endpoints)
   - Quartos, reservas, mensagens, relatórios
   - Todos os status HTTP corretos
   - Validação de entrada robusta

6. **Banco de Dados PostgreSQL 18**
   - Schema auto-criado na inicialização
   - 3 tabelas (rooms, reservations, messages)
   - Dados persistidos entre reinicializações

7. **Relatórios em CSV**
   - Ocupação: Quartos ocupados em uma data
   - Financeiro: Receita de um mês

## Decisões de Design

### 1. Arredondamento de Preços
Implementado com `BigDecimal.setScale(2, RoundingMode.HALF_EVEN)`:
- Chamado "banker's rounding"
- .5 arredonda para o número par mais próximo
- Exemplo: 1.5 → 2, 2.5 → 2, 3.5 → 4
- Aplicado em: cada noite, subtotal, desconto, total

### 2. Organização de Código
Separação em camadas:
- **Handlers**: Interface HTTP, validação básica
- **DAOs**: Acesso ao banco de dados
- **Utils**: Lógica de negócio (preços, formatação)
- **Models**: Entidades do domínio

Seguindo orientação do CLAUDE.md: cada variação entra num lugar só.

### 3. Disponibilidade de Quartos
Validação usando SQL:
```sql
SELECT COUNT(*) FROM reservations
WHERE room_number = ? AND status != 'CANCELLED'
  AND check_in < ? AND check_out > ?
```

Evita overlapping de reservas, mas permite bordas (saída no check-in é permitido).

### 4. E-mails Simulados
Armazenados em tabela `messages` em vez de enviados realmente:
- Permite testes sem SMTP
- A recepção pode consultar o histórico
- Fácil migração para envio real depois

### 5. Identificação do Hóspede
Validação: exatamente 1 de CPF, CNPJ ou Passaporte
- CPF: Sozinho
- CNPJ: Deve vir com companyName
- Passaporte: Deve vir com guestCountry

## Testes e Validação

### Compilação
```bash
mvn clean package
```
Gera JAR de 8.0 MB com todas as dependências.

### Execução de Testes
```bash
mvn verify
```
Garante que o código compila e os testes passam.

### Teste de API
```bash
bash test-api.sh
```
Testa todos os endpoints com casos reais.

## Como Usar

### 1. Desenvolvimento Local

```bash
# Terminal 1: PostgreSQL
docker run -d \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=pousada \
  -e POSTGRES_PASSWORD=senha123 \
  -p 5432:5432 \
  postgres:18-alpine

# Terminal 2: Aplicação
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="senha123"
export PORT=7070
java -jar target/pousada-mare-alta-1.0.0.jar

# Terminal 3: Testes
curl http://localhost:7070/rooms
```

### 2. Com Docker Compose

```bash
docker-compose up
# Espera a aplicação iniciar, depois:
curl http://localhost:7070/rooms
```

### 3. Produção

Usar as variáveis de ambiente do servidor:
```
DB_URL=jdbc:postgresql://prod-db:5432/pousada
DB_USER=pousada
DB_PASSWORD=<senha segura>
PORT=7070
java -jar pousada-mare-alta-1.0.0.jar
```

## Estrutura de Requisição/Resposta

### Criar Reserva
```json
{
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "111.222.333-44",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

### Resposta
```json
{
  "id": 1,
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "111.222.333-44",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.0,
  "total": 499.80,
  "status": "PENDING",
  "amountPaid": 0.0,
  "refund": null
}
```

## Cálculos Exemplo

### Cenário 1: STANDARD em março (fora de alta temporada)
- Base: R$ 249,90
- 2 noites: 249,90 × 2 = 499,80
- Sem desconto
- **Total: R$ 499,80**

### Cenário 2: DELUXE em julho (alta temporada) NON_REFUNDABLE
- Base: R$ 389,90
- Alta temporada: 389,90 × 1.20 = 467,88
- 2 noites: 467,88 × 2 = 935,76
- Desconto NON_REFUNDABLE: 935,76 × 0,15 = 140,36
- **Total: R$ 795,40**

### Cenário 3: SUITE em dezembro (alta temporada)
- Base: R$ 579,90
- Alta temporada: 579,90 × 1.20 = 695,88
- 3 noites: 695,88 × 3 = 2087,64
- Sem desconto (STANDARD)
- **Total: R$ 2.087,64**

## Funcionalidades Futuras Possíveis

Se precisar expandir o sistema:

1. **Envio Real de E-mails**
   - Trocar MessageDAO para enviar via SMTP
   - Manter histórico em banco

2. **Autenticação**
   - Login para recepção e contador
   - Roles/permissões

3. **Mais Planos de Tarifa**
   - Desconto antecipado
   - Desconto por longa permanência

4. **Descontos Complementares**
   - Cupom de desconto
   - Programa de fidelidade

5. **Integração de Pagamento**
   - Webhook do gateway de pagamento
   - Confirmar automaticamente após pagamento

6. **Mais Relatórios**
   - Hóspedes de um período
   - Taxa de ocupação
   - Receita média

7. **Interface Web**
   - Dashboard para recepção
   - Sistema de check-in/check-out visual

## Troubleshooting

### "Connection refused" no banco
- Verificar se PostgreSQL está rodando
- Verificar DB_URL, DB_USER, DB_PASSWORD

### "Erro ao inicializar banco"
- Deletar `target/` e recompilar
- Verificar permissões de escrita no banco

### "Porta já em uso"
- Mudar PORT para outro valor (ex: 8080)
- Ou matar o processo na porta 7070

### Maven lento na primeira compilação
- Normal, está fazendo download de 200+ dependências
- Próximas compilações serão muito mais rápidas

## Recursos Utilizados

- **Java 21**: Linguagem
- **Javalin 6.7.0**: Framework HTTP
- **PostgreSQL 18**: Banco de dados
- **Gson**: Parser JSON
- **BigDecimal**: Arredondamento de preços
- **SLF4J + Logback**: Logs
- **Maven**: Build tool
- **Docker**: Containerização

## Conformidade

- ✓ Java 21
- ✓ Javalin 6.7.0
- ✓ PostgreSQL 18
- ✓ `mvn package` gera JAR executável
- ✓ `java -jar` funciona standalone
- ✓ `mvn verify` passa
- ✓ Variáveis de ambiente (DB_URL, DB_USER, DB_PASSWORD, PORT)
- ✓ Schema auto-criado
- ✓ Nenhuma pergunta durante a implementação (conforme solicitado)
