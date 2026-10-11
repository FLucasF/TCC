package br.com.loja.checkout;

/** Opção identificada pelo código que o site envia (ex.: "EXPRESSA", "PIX"). */
public interface Codificado {

    String codigo();
}
