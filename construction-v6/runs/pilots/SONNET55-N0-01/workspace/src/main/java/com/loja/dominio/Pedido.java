package com.loja.dominio;

import java.time.Instant;
import java.util.List;

public class Pedido {

    private final String id;
    private final String cpf;
    private SituacaoPedido situacao;
    private final Instant criadoEm;
    private final Resumo resumo;
    private final List<Item> itens;
    private String codigoRastreio;
    private String idTransacao;

    public Pedido(String id, String cpf, SituacaoPedido situacao, Instant criadoEm, Resumo resumo,
                  List<Item> itens, String codigoRastreio, String idTransacao) {
        this.id = id;
        this.cpf = cpf;
        this.situacao = situacao;
        this.criadoEm = criadoEm;
        this.resumo = resumo;
        this.itens = List.copyOf(itens);
        this.codigoRastreio = codigoRastreio;
        this.idTransacao = idTransacao;
    }

    public String id() { return id; }
    public String cpf() { return cpf; }
    public SituacaoPedido situacao() { return situacao; }
    public Instant criadoEm() { return criadoEm; }
    public Resumo resumo() { return resumo; }
    public List<Item> itens() { return itens; }
    public String codigoRastreio() { return codigoRastreio; }
    public String idTransacao() { return idTransacao; }

    public Pedido copia() {
        return new Pedido(id, cpf, situacao, criadoEm, resumo, itens, codigoRastreio, idTransacao);
    }

    public void mudarPara(SituacaoPedido nova) {
        if (!situacao.permite(nova)) {
            throw ErroNegocio.conflito("OPERACAO_NAO_PERMITIDA");
        }
        situacao = nova;
    }

    public void registrarPagamento(String idTransacao) {
        mudarPara(SituacaoPedido.PAGO);
        this.idTransacao = idTransacao;
    }

    public void registrarEnvio(String codigoRastreio) {
        mudarPara(SituacaoPedido.ENVIADO);
        this.codigoRastreio = codigoRastreio;
    }
}
