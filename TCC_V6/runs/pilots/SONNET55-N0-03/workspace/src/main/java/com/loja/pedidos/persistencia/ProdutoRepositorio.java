package com.loja.pedidos.persistencia;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.loja.pedidos.dominio.ItemPedido;
import com.loja.pedidos.dominio.Produto;

@Repository
public class ProdutoRepositorio {

    private final JdbcClient jdbc;

    public ProdutoRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Produto> buscar(String sku) {
        return jdbc.sql("SELECT * FROM produto WHERE sku = :sku").param("sku", sku)
                .query((rs, n) -> new Produto(
                        rs.getString("sku"),
                        rs.getString("nome"),
                        rs.getBigDecimal("preco"),
                        peso(rs.getBigDecimal("peso_kg")),
                        rs.getInt("estoque"),
                        rs.getBoolean("fora_de_linha")))
                .optional();
    }

    public Map<String, Produto> buscarPorSkus(Collection<String> skus) {
        Map<String, Produto> mapa = new HashMap<>();
        for (String sku : skus) {
            buscar(sku).ifPresent(p -> mapa.put(sku, p));
        }
        return mapa;
    }

    public void inserirSeAusente(Produto p) {
        jdbc.sql("""
                INSERT INTO produto (sku, nome, preco, peso_kg, estoque, fora_de_linha)
                SELECT :sku, :nome, :preco, :peso, :estoque, :fora
                WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = :sku)""")
                .param("sku", p.sku()).param("nome", p.nome()).param("preco", p.preco())
                .param("peso", p.pesoKg()).param("estoque", p.estoque()).param("fora", p.foraDeLinha())
                .update();
    }

    /**
     * Baixa o estoque de todos os itens ou de nenhum. Trava as linhas em ordem de SKU (evita deadlock)
     * e só então confere e baixa, então o estoque nunca fica negativo, mesmo com pagamentos simultâneos.
     * Deve ser chamado dentro de uma transação.
     */
    public boolean baixarEstoque(List<ItemPedido> itens) {
        List<ItemPedido> ordenados = itens.stream().sorted(Comparator.comparing(ItemPedido::sku)).toList();
        for (ItemPedido item : ordenados) {
            Integer estoque = jdbc.sql("SELECT estoque FROM produto WHERE sku = :sku FOR UPDATE")
                    .param("sku", item.sku()).query(Integer.class).optional().orElse(null);
            if (estoque == null || estoque < item.quantidade()) {
                return false;
            }
        }
        for (ItemPedido item : ordenados) {
            jdbc.sql("UPDATE produto SET estoque = estoque - :q WHERE sku = :sku")
                    .param("q", item.quantidade()).param("sku", item.sku()).update();
        }
        return true;
    }

    public void devolverEstoque(List<ItemPedido> itens) {
        for (ItemPedido item : itens.stream().sorted(Comparator.comparing(ItemPedido::sku)).toList()) {
            jdbc.sql("UPDATE produto SET estoque = estoque + :q WHERE sku = :sku")
                    .param("q", item.quantidade()).param("sku", item.sku()).update();
        }
    }

    private static BigDecimal peso(BigDecimal valor) {
        BigDecimal limpo = valor.stripTrailingZeros();
        return limpo.scale() < 2 ? limpo.setScale(2) : limpo;
    }
}
