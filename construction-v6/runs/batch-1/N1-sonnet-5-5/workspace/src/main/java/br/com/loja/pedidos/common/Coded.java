package br.com.loja.pedidos.common;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Algo que o site identifica por um código (forma de entrega, cupom, nível do clube, forma de pagamento). */
public interface Coded {

    String code();

    static <T extends Coded> Map<String, T> index(Collection<T> items) {
        Map<String, T> byCode = new LinkedHashMap<>();
        items.forEach(item -> byCode.put(item.code(), item));
        return byCode;
    }
}
