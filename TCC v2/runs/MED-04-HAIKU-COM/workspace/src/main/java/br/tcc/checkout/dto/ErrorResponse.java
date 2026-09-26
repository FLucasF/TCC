package br.tcc.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResponse {
	@JsonProperty("erro")
	private String erro;

	public ErrorResponse(String erro) {
		this.erro = erro;
	}

	public String getErro() {
		return erro;
	}

	public void setErro(String erro) {
		this.erro = erro;
	}
}
