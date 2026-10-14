package br.com.loja.pedidos.comum;

/** Recusa de uma operação: vira {"erro": codigo} com a situação HTTP indicada. */
public class ErroNegocio extends RuntimeException {

    private final int status;
    private final String codigo;

    public ErroNegocio(int status, String codigo) {
        super(codigo, null, false, false);
        this.status = status;
        this.codigo = codigo;
    }

    public static ErroNegocio naoProcessavel(String codigo) {
        return new ErroNegocio(422, codigo);
    }

    public static ErroNegocio naoEncontrado(String codigo) {
        return new ErroNegocio(404, codigo);
    }

    public static ErroNegocio conflito(String codigo) {
        return new ErroNegocio(409, codigo);
    }

    public int status() {
        return status;
    }

    public String codigo() {
        return codigo;
    }
}
