package com.loja.checkout.model;

import java.math.BigDecimal;

public class Resumo {
    private BigDecimal subtotalProdutos;
    private BigDecimal descontoCupom;
    private BigDecimal frete;
    private Integer prazoEntregaDias;
    private BigDecimal seguro;
    private BigDecimal ajustePagamento;
    private BigDecimal totalFinal;
    private Integer parcelas;
    private BigDecimal valorParcela;
    private BigDecimal creditoProximaCompra;
    private Boolean brinde;

    public Resumo() {
        this.subtotalProdutos = BigDecimal.ZERO.setScale(2);
        this.descontoCupom = BigDecimal.ZERO.setScale(2);
        this.frete = BigDecimal.ZERO.setScale(2);
        this.prazoEntregaDias = 0;
        this.seguro = BigDecimal.ZERO.setScale(2);
        this.ajustePagamento = BigDecimal.ZERO.setScale(2);
        this.totalFinal = BigDecimal.ZERO.setScale(2);
        this.parcelas = 1;
        this.valorParcela = BigDecimal.ZERO.setScale(2);
        this.creditoProximaCompra = BigDecimal.ZERO.setScale(2);
        this.brinde = false;
    }

    public BigDecimal getSubtotalProdutos() { return subtotalProdutos; }
    public void setSubtotalProdutos(BigDecimal subtotalProdutos) { this.subtotalProdutos = subtotalProdutos; }

    public BigDecimal getDescontoCupom() { return descontoCupom; }
    public void setDescontoCupom(BigDecimal descontoCupom) { this.descontoCupom = descontoCupom; }

    public BigDecimal getFrete() { return frete; }
    public void setFrete(BigDecimal frete) { this.frete = frete; }

    public Integer getPrazoEntregaDias() { return prazoEntregaDias; }
    public void setPrazoEntregaDias(Integer prazoEntregaDias) { this.prazoEntregaDias = prazoEntregaDias; }

    public BigDecimal getSeguro() { return seguro; }
    public void setSeguro(BigDecimal seguro) { this.seguro = seguro; }

    public BigDecimal getAjustePagamento() { return ajustePagamento; }
    public void setAjustePagamento(BigDecimal ajustePagamento) { this.ajustePagamento = ajustePagamento; }

    public BigDecimal getTotalFinal() { return totalFinal; }
    public void setTotalFinal(BigDecimal totalFinal) { this.totalFinal = totalFinal; }

    public Integer getParcelas() { return parcelas; }
    public void setParcelas(Integer parcelas) { this.parcelas = parcelas; }

    public BigDecimal getValorParcela() { return valorParcela; }
    public void setValorParcela(BigDecimal valorParcela) { this.valorParcela = valorParcela; }

    public BigDecimal getCreditoProximaCompra() { return creditoProximaCompra; }
    public void setCreditoProximaCompra(BigDecimal creditoProximaCompra) { this.creditoProximaCompra = creditoProximaCompra; }

    public Boolean getBrinde() { return brinde; }
    public void setBrinde(Boolean brinde) { this.brinde = brinde; }

    public BigDecimal totalPedido() {
        return subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);
    }
}
