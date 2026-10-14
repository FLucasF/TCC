package br.com.loja.pedidos.catalogo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProdutoRepositorio {

    private final JdbcClient jdbc;

    public ProdutoRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Produto> buscar(String sku) {
        return jdbc.sql("SELECT * FROM produto WHERE sku = :sku")
                .param("sku", sku)
                .query(ProdutoRepositorio::mapear)
                .optional();
    }

    public Map<String, Produto> buscarVarios(Collection<String> skus) {
        if (skus.isEmpty()) {
            return Map.of();
        }
        return jdbc.sql("SELECT * FROM produto WHERE sku IN (:skus)")
                .param("skus", skus)
                .query(ProdutoRepositorio::mapear)
                .list()
                .stream()
                .collect(Collectors.toMap(Produto::sku, Function.identity()));
    }

    private static Produto mapear(ResultSet rs, int linha) throws SQLException {
        return new Produto(
                rs.getString("sku"),
                rs.getString("nome"),
                rs.getBigDecimal("preco"),
                rs.getBigDecimal("peso_kg"),
                rs.getInt("estoque"),
                rs.getBoolean("fora_de_linha"));
    }
}
