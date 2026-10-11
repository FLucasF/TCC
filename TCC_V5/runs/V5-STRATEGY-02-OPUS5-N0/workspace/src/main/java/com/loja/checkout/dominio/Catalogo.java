package com.loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Catalogo de politicas indexadas por codigo. Para adicionar uma opcao nova
 * (transportadora, cupom, nivel do clube, forma de pagamento) basta publicar
 * um bean novo da politica: ele entra no catalogo automaticamente.
 */
public class Catalogo<T extends Identificavel> {

    private final Map<String, T> porCodigo = new LinkedHashMap<>();

    protected Catalogo(Collection<T> politicas) {
        for (T politica : politicas) {
            porCodigo.put(politica.codigo(), politica);
        }
    }

    public Optional<T> buscar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }

    public Collection<T> todas() {
        return porCodigo.values();
    }
}
