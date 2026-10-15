package com.loja.pedidos.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.loja.pedidos.dominio.Aviso;
import com.loja.pedidos.dominio.TipoAviso;

@Repository
public class AvisoRepositorio {

    private static final RowMapper<Aviso> MAPPER = (rs, n) -> new Aviso(
            rs.getString("id"),
            TipoAviso.valueOf(rs.getString("tipo")),
            rs.getString("pedido_id"),
            rs.getString("situacao"));

    private final JdbcClient jdbc;

    public AvisoRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void inserir(String cpf, TipoAviso tipo, String pedidoId) {
        jdbc.sql("INSERT INTO aviso (id, cpf, tipo, pedido_id, situacao) VALUES (:id, :cpf, :tipo, :pedido, 'NOVO')")
                .param("id", UUID.randomUUID().toString()).param("cpf", cpf)
                .param("tipo", tipo.name()).param("pedido", pedidoId).update();
    }

    public List<Aviso> listar(String cpf) {
        return jdbc.sql("SELECT id, tipo, pedido_id, situacao FROM aviso WHERE cpf = :cpf ORDER BY seq")
                .param("cpf", cpf).query(MAPPER).list();
    }

    public Optional<Aviso> marcarLido(String id) {
        jdbc.sql("UPDATE aviso SET situacao = 'LIDO' WHERE id = :id").param("id", id).update();
        return jdbc.sql("SELECT id, tipo, pedido_id, situacao FROM aviso WHERE id = :id")
                .param("id", id).query(MAPPER).optional();
    }
}
