package loja.dominio;

/** Recusa de negócio: vira {"erro": codigo} com a situação HTTP indicada. */
public class NegocioException extends RuntimeException {

    private final String codigo;
    private final int status;

    public NegocioException(String codigo, int status) {
        super(codigo, null, false, false);
        this.codigo = codigo;
        this.status = status;
    }

    public static NegocioException invalido(String codigo) {
        return new NegocioException(codigo, 422);
    }

    public static NegocioException conflito(String codigo) {
        return new NegocioException(codigo, 409);
    }

    public static NegocioException naoEncontrado(String codigo) {
        return new NegocioException(codigo, 404);
    }

    public String codigo() {
        return codigo;
    }

    public int status() {
        return status;
    }
}
