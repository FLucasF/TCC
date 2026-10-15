package com.loja.persistencia;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import com.loja.dominio.Aviso;
import com.loja.dominio.Catalogo;
import com.loja.dominio.Cliente;
import com.loja.dominio.Dinheiro;
import com.loja.dominio.Estado;
import com.loja.dominio.Item;
import com.loja.dominio.Mudancas;
import com.loja.dominio.Pedido;
import com.loja.dominio.Persistencia;
import com.loja.dominio.Produto;
import com.loja.dominio.Resumo;
import com.loja.dominio.SituacaoPedido;
import com.loja.dominio.TipoAviso;

/** Guarda tudo no PostgreSQL. */
public class PgPersistencia implements Persistencia {

    private static final Logger log = LoggerFactory.getLogger(PgPersistencia.class);

    private static final String ESQUEMA = """
            CREATE TABLE IF NOT EXISTS produto (
                sku TEXT PRIMARY KEY,
                nome TEXT NOT NULL,
                preco NUMERIC(14,2) NOT NULL,
                peso_kg NUMERIC(10,2) NOT NULL,
                estoque INTEGER NOT NULL CHECK (estoque >= 0),
                fora_de_linha BOOLEAN NOT NULL);
            CREATE TABLE IF NOT EXISTS cliente (
                cpf TEXT PRIMARY KEY,
                email TEXT NOT NULL,
                saldo_credito NUMERIC(14,2) NOT NULL);
            CREATE TABLE IF NOT EXISTS pedido (
                id TEXT PRIMARY KEY,
                cpf TEXT NOT NULL,
                situacao TEXT NOT NULL,
                criado_em TEXT NOT NULL,
                codigo_rastreio TEXT,
                id_transacao TEXT,
                resumo TEXT NOT NULL,
                itens TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS aviso (
                id TEXT PRIMARY KEY,
                seq BIGINT NOT NULL,
                cpf TEXT NOT NULL,
                tipo TEXT NOT NULL,
                pedido_id TEXT NOT NULL,
                situacao TEXT NOT NULL)""";

    private final PgConexao conexao;
    private final JsonMapper json = JsonMapper.builder().build();

    public PgPersistencia(PgConexao conexao) {
        this.conexao = conexao;
    }

    @Override
    public Estado carregar() {
        prepararBanco();
        Estado estado = new Estado();
        for (String[] l : conexao.consultar(
                "SELECT sku, nome, preco, peso_kg, estoque, fora_de_linha FROM produto ORDER BY sku")) {
            estado.produtos.put(l[0], new Produto(l[0], l[1], Dinheiro.arredondar(new BigDecimal(l[2])),
                    Dinheiro.arredondar(new BigDecimal(l[3])), Integer.parseInt(l[4]), l[5].equals("t")));
        }
        for (String[] l : conexao.consultar("SELECT cpf, email, saldo_credito FROM cliente")) {
            estado.clientes.put(l[0], new Cliente(l[0], l[1], Dinheiro.arredondar(new BigDecimal(l[2]))));
        }
        for (String[] l : conexao.consultar("SELECT id, cpf, situacao, criado_em, codigo_rastreio, id_transacao, "
                + "resumo, itens FROM pedido ORDER BY criado_em, id")) {
            estado.adicionarPedido(new Pedido(l[0], l[1], SituacaoPedido.valueOf(l[2]), Instant.parse(l[3]),
                    json.readValue(l[6], Resumo.class), json.readValue(l[7], new TypeReference<List<Item>>() { }),
                    l[4], l[5]));
        }
        for (String[] l : conexao.consultar("SELECT id, seq, cpf, tipo, pedido_id, situacao FROM aviso ORDER BY seq")) {
            estado.adicionarAviso(new Aviso(l[0], Long.parseLong(l[1]), l[2], TipoAviso.valueOf(l[3]), l[4],
                    l[5].equals("LIDO")));
        }
        return estado;
    }

    private void prepararBanco() {
        conexao.consultar(ESQUEMA);
        List<String> comandos = new ArrayList<>();
        if (conexao.consultar("SELECT 1 FROM produto LIMIT 1").isEmpty()) {
            log.info("Banco vazio: carregando o catálogo inicial");
            for (Produto p : Catalogo.inicial()) {
                comandos.add("INSERT INTO produto (sku, nome, preco, peso_kg, estoque, fora_de_linha) VALUES ("
                        + texto(p.sku()) + "," + texto(p.nome()) + "," + p.preco().toPlainString() + ","
                        + p.pesoKg().toPlainString() + "," + p.estoque() + "," + p.foraDeLinha()
                        + ") ON CONFLICT (sku) DO NOTHING");
            }
        }
        conexao.transacao(comandos);
    }

    @Override
    public void gravar(Mudancas m) {
        List<String> comandos = new ArrayList<>();
        for (Produto p : m.produtos) {
            comandos.add("INSERT INTO produto (sku, nome, preco, peso_kg, estoque, fora_de_linha) VALUES ("
                    + texto(p.sku()) + "," + texto(p.nome()) + "," + p.preco().toPlainString() + ","
                    + p.pesoKg().toPlainString() + "," + p.estoque() + "," + p.foraDeLinha()
                    + ") ON CONFLICT (sku) DO UPDATE SET nome = EXCLUDED.nome, preco = EXCLUDED.preco, "
                    + "peso_kg = EXCLUDED.peso_kg, estoque = EXCLUDED.estoque, "
                    + "fora_de_linha = EXCLUDED.fora_de_linha");
        }
        for (Cliente c : m.clientes) {
            comandos.add("INSERT INTO cliente (cpf, email, saldo_credito) VALUES (" + texto(c.cpf()) + ","
                    + texto(c.email()) + "," + c.saldoCredito().toPlainString()
                    + ") ON CONFLICT (cpf) DO UPDATE SET email = EXCLUDED.email, "
                    + "saldo_credito = EXCLUDED.saldo_credito");
        }
        for (Pedido p : m.pedidos) {
            comandos.add("INSERT INTO pedido (id, cpf, situacao, criado_em, codigo_rastreio, id_transacao, "
                    + "resumo, itens) VALUES (" + texto(p.id()) + "," + texto(p.cpf()) + ","
                    + texto(p.situacao().name()) + "," + texto(p.criadoEm().toString()) + ","
                    + textoOuNulo(p.codigoRastreio()) + "," + textoOuNulo(p.idTransacao()) + ","
                    + texto(json.writeValueAsString(p.resumo())) + "," + texto(json.writeValueAsString(p.itens()))
                    + ") ON CONFLICT (id) DO UPDATE SET situacao = EXCLUDED.situacao, "
                    + "codigo_rastreio = EXCLUDED.codigo_rastreio, id_transacao = EXCLUDED.id_transacao");
        }
        for (Aviso a : m.avisos) {
            comandos.add("INSERT INTO aviso (id, seq, cpf, tipo, pedido_id, situacao) VALUES (" + texto(a.id())
                    + "," + a.seq() + "," + texto(a.cpf()) + "," + texto(a.tipo().name()) + ","
                    + texto(a.pedidoId()) + "," + texto(a.lido() ? "LIDO" : "NOVO")
                    + ") ON CONFLICT (id) DO UPDATE SET situacao = EXCLUDED.situacao");
        }
        conexao.transacao(comandos);
    }

    private static String textoOuNulo(String valor) {
        return valor == null ? "NULL" : texto(valor);
    }

    /** Literal de texto seguro (aspas simples dobradas; o servidor trata barras como texto comum). */
    static String texto(String valor) {
        if (valor.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("texto com caractere nulo");
        }
        return "'" + valor.replace("'", "''") + "'";
    }
}
