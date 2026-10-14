package com.loja;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.loja.persistencia.PgConexao;

class ScramTest {

    @Test
    void vetorDaRfc7677() throws Exception {
        Class<?> tipo = Class.forName("com.loja.persistencia.Scram");
        var construtor = tipo.getDeclaredConstructor(String.class, String.class, String.class);
        construtor.setAccessible(true);
        Object scram = construtor.newInstance("user", "pencil", "rOprNGfwEbeRWgbNEkqO");
        var metodo = tipo.getDeclaredMethod("mensagemFinal", String.class);
        metodo.setAccessible(true);
        String resposta = (String) metodo.invoke(scram,
                "r=rOprNGfwEbeRWgbNEkqO%hvYDpWUa2RaTCAfuxFIlj)hNlF$k0,s=W22ZaJ0SNY7soEsUEjb6gQ==,i=4096");
        assertEquals("c=biws,r=rOprNGfwEbeRWgbNEkqO%hvYDpWUa2RaTCAfuxFIlj)hNlF$k0,"
                + "p=dHzbZapWIk4jUhN+Ute9ytag9zjfMHgsqmmiz7AndVQ=", resposta);
    }

    @Test
    void urlJdbcEhInterpretada() {
        new PgConexao("jdbc:postgresql://db:5433/loja?sslmode=disable", "u", "s").close();
    }
}
