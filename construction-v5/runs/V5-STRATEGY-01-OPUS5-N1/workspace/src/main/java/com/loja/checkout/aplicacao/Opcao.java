package com.loja.checkout.aplicacao;

/** Traduz o texto que o site envia para a opcao correspondente do dominio. */
final class Opcao {

    private Opcao() {
    }

    static <E extends Enum<E>> E exigir(Class<E> tipo, String texto, CodigoErro quandoNaoExiste) {
        E opcao = texto == null ? null : buscar(tipo, texto);
        if (opcao == null) {
            throw new PedidoRecusadoException(quandoNaoExiste);
        }
        return opcao;
    }

    private static <E extends Enum<E>> E buscar(Class<E> tipo, String texto) {
        for (E candidato : tipo.getEnumConstants()) {
            if (candidato.name().equals(texto)) {
                return candidato;
            }
        }
        return null;
    }
}
