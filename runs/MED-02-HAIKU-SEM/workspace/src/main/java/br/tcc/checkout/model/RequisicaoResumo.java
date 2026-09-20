package br.tcc.checkout.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RequisicaoResumo {

	@JsonProperty("itens")
	private List<ItemCarrinho> itens;

	@JsonProperty("modalidadeEntrega")
	private String modalidadeEntrega;

	@JsonProperty("cupom")
	private String cupom;

	@JsonProperty("formaPagamento")
	private String formaPagamento;

	@JsonProperty("parcelas")
	private Integer parcelas;

	public RequisicaoResumo() {
	}

	public RequisicaoResumo(List<ItemCarrinho> itens, String modalidadeEntrega, String cupom,
			String formaPagamento, Integer parcelas) {
		this.itens = itens;
		this.modalidadeEntrega = modalidadeEntrega;
		this.cupom = cupom;
		this.formaPagamento = formaPagamento;
		this.parcelas = parcelas;
	}

	public List<ItemCarrinho> getItens() {
		return itens;
	}

	public void setItens(List<ItemCarrinho> itens) {
		this.itens = itens;
	}

	public String getModalidadeEntrega() {
		return modalidadeEntrega;
	}

	public void setModalidadeEntrega(String modalidadeEntrega) {
		this.modalidadeEntrega = modalidadeEntrega;
	}

	public String getCupom() {
		return cupom;
	}

	public void setCupom(String cupom) {
		this.cupom = cupom;
	}

	public String getFormaPagamento() {
		return formaPagamento;
	}

	public void setFormaPagamento(String formaPagamento) {
		this.formaPagamento = formaPagamento;
	}

	public Integer getParcelas() {
		return parcelas;
	}

	public void setParcelas(Integer parcelas) {
		this.parcelas = parcelas;
	}
}
