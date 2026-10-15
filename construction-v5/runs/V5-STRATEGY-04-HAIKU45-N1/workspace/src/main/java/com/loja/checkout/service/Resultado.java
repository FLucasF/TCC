package com.loja.checkout.service;

import com.loja.checkout.dto.ResumoResponse;

public sealed interface Resultado {
    record Sucesso(ResumoResponse resumo) implements Resultado {}
    record Erro(String codigo) implements Resultado {}
}
