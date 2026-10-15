package com.loja.checkout.dominio;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Busca de um caso pelo seu código. É igual para entregas, cupons, níveis do
 * clube e formas de pagamento, então mora num lugar só. A escolha do caso é um
 * acesso a mapa, não uma sequência de condições. Adicionar um caso novo é só
 * registrar mais um bean da dimensão correspondente.
 */
public class RegistroPorCodigo<T extends Codificado> {

    private final Map<String, T> porCodigo;

    public RegistroPorCodigo(Collection<T> casos) {
        Map<String, T> mapa = new HashMap<>();
        for (T caso : casos) {
            mapa.put(caso.codigo(), caso);
        }
        this.porCodigo = Map.copyOf(mapa);
    }

    public Optional<T> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
