package com.loja.checkout.comum;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Escolher um caso é sempre um lookup por código, nunca uma cadeia de condições.
 * Esta é a parte igual às quatro famílias; cada registro concreto só diz qual é
 * o seu tipo. Opções novas aparecem só adicionando um bean que implementa a
 * interface da família — nada aqui muda.
 */
public abstract class RegistroDeCodigos<T extends Codificavel> {

    private final Map<String, T> porCodigo;

    protected RegistroDeCodigos(List<T> itens) {
        this.porCodigo = itens.stream()
                .collect(Collectors.toMap(Codificavel::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
