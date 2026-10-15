package com.loja.pedidos.persistencia;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Produto;

/** Ao subir: cria as tabelas que faltam e cadastra os produtos iniciais que ainda não existem (nunca sobrescreve). */
@Component
public class Inicializador implements SmartInitializingSingleton {

    private static final List<Produto> CATALOGO_INICIAL = List.of(
            produto("CAM-001", "Camiseta básica", "79.90", "0.30", 50, false),
            produto("TEN-001", "Tênis corrida", "249.90", "1.20", 10, false),
            produto("FON-001", "Fone sem fio", "199.90", "0.25", 5, false),
            produto("MEI-001", "Meia esportiva", "19.90", "0.10", 100, false),
            produto("JAQ-001", "Jaqueta corta-vento", "389.90", "0.80", 3, false),
            produto("MOC-001", "Mochila urbana", "159.90", "0.90", 1, false),
            produto("BON-001", "Boné aba reta", "59.90", "0.15", 0, false),
            produto("REL-001", "Relógio digital", "129.90", "0.20", 8, true));

    private final DataSource dataSource;
    private final ProdutoRepositorio produtos;

    public Inicializador(DataSource dataSource, ProdutoRepositorio produtos) {
        this.dataSource = dataSource;
        this.produtos = produtos;
    }

    @Override
    public void afterSingletonsInstantiated() {
        try (Connection c = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(c, new ClassPathResource("schema.sql"));
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao criar o esquema do banco", e);
        }
        for (Produto p : CATALOGO_INICIAL) {
            try {
                produtos.inserirSeAusente(p);
            } catch (DuplicateKeyException e) {
                // outra instância cadastrou ao mesmo tempo
            }
        }
    }

    private static Produto produto(String sku, String nome, String preco, String peso, int estoque, boolean fora) {
        return new Produto(sku, nome, new BigDecimal(preco), new BigDecimal(peso), estoque, fora);
    }
}
