package br.com.loja.pedidos.catalogo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProdutoRepositorio {

    private final JdbcClient jdbc;

    public ProdutoRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Produto> buscar(String sku) {
        return jdbc.sql("SELECT * FROM produto WHERE sku = ?").param(sku).query(this::mapear).optional();
    }

    /** Trava as linhas dos produtos, sempre na mesma ordem, até o fim da transação. */
    public List<Produto> travar(Collection<String> skus) {
        return skus.stream().sorted()
                .map(sku -> jdbc.sql("SELECT * FROM produto WHERE sku = ? FOR UPDATE").param(sku).query(this::mapear).single())
                .toList();
    }

    public void alterarEstoque(String sku, int variacao) {
        jdbc.sql("UPDATE produto SET estoque = estoque + ? WHERE sku = ?").param(variacao).param(sku).update();
    }

    private Produto mapear(ResultSet rs, int linha) throws SQLException {
        BigDecimal peso = rs.getBigDecimal("peso_kg").stripTrailingZeros();
        if (peso.scale() < 2) {
            peso = peso.setScale(2, RoundingMode.UNNECESSARY);
        }
        return new Produto(rs.getString("sku"), rs.getString("nome"), rs.getBigDecimal("preco").setScale(2),
                peso, rs.getInt("estoque"), rs.getBoolean("fora_de_linha"));
    }
}
