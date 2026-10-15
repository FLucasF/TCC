package br.tcc.checkout.dto;

public class RespostaErro {
    private String erro;

    public RespostaErro() {}

    public RespostaErro(String erro) {
        this.erro = erro;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }
}
