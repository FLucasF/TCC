package br.tcc.checkout.model;

public enum FormaPagamento {
    PIX("PIX"),
    CARTAO("CARTAO"),
    BOLETO("BOLETO");

    private final String codigo;

    FormaPagamento(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public static FormaPagamento porCodigo(String codigo) {
        for (FormaPagamento f : values()) {
            if (f.codigo.equals(codigo)) {
                return f;
            }
        }
        return null;
    }
}
