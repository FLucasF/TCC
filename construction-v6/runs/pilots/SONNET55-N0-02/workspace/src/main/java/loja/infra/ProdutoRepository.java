package loja.infra;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import loja.dominio.Produto;

@Repository
public class ProdutoRepository {

    private final JdbcClient jdbc;

    public ProdutoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Produto> buscar(String sku) {
        return jdbc.sql("SELECT * FROM produto WHERE sku = ?").param(sku)
                .query((rs, n) -> new Produto(rs.getString("sku"), rs.getString("nome"), rs.getBigDecimal("preco"),
                        rs.getBigDecimal("peso_kg"), rs.getInt("estoque"), rs.getBoolean("fora_de_linha")))
                .optional();
    }

    public Map<String, Produto> buscarVarios(Collection<String> skus) {
        Map<String, Produto> mapa = new HashMap<>();
        for (String sku : skus) {
            buscar(sku).ifPresent(p -> mapa.put(sku, p));
        }
        return mapa;
    }

    /** Baixa só se houver saldo suficiente (atômico, nunca deixa negativo). */
    public boolean baixarEstoque(String sku, int quantidade) {
        return jdbc.sql("UPDATE produto SET estoque = estoque - ? WHERE sku = ? AND estoque >= ?")
                .params(quantidade, sku, quantidade).update() == 1;
    }

    public void reporEstoque(String sku, int quantidade) {
        jdbc.sql("UPDATE produto SET estoque = estoque + ? WHERE sku = ?").params(quantidade, sku).update();
    }

    /** Carrega o catálogo inicial sem sobrescrever o que já existe. */
    public void semear(List<Produto> produtos) {
        for (Produto p : produtos) {
            if (buscar(p.sku()).isEmpty()) {
                jdbc.sql("INSERT INTO produto (sku, nome, preco, peso_kg, estoque, fora_de_linha) VALUES (?,?,?,?,?,?)")
                        .params(p.sku(), p.nome(), p.preco(), p.pesoKg(), p.estoque(), p.foraDeLinha()).update();
            }
        }
    }

    public static Produto produto(String sku, String nome, String preco, String peso, int estoque, boolean fora) {
        return new Produto(sku, nome, new BigDecimal(preco), new BigDecimal(peso), estoque, fora);
    }
}
