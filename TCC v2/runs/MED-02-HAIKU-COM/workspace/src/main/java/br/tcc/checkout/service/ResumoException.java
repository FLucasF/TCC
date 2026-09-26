package br.tcc.checkout.service;

public class ResumoException extends Exception {
    private String codigo;

    public ResumoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
