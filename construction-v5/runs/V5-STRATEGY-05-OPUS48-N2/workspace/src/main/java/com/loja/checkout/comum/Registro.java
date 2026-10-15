package com.loja.checkout.comum;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transforma um código recebido do site no caso certo (modalidade, cupom,
 * nível, forma de pagamento). A escolha é sempre uma busca em mapa, nunca uma
 * sequência de condições; adicionar um caso novo é só registrar mais um
 * componente.
 */
public class Registro<T extends Codificavel> {

    private final Map<String, T> porCodigo;

    public Registro(List<T> itens) {
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
