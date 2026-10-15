package br.tcc.checkout.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RespostaErro {

	@JsonProperty("erro")
	private String erro;

	public RespostaErro() {
	}

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
