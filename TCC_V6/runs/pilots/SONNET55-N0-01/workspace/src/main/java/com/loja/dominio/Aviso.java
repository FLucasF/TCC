package com.loja.dominio;

public class Aviso {

    private final String id;
    private final long seq;
    private final String cpf;
    private final TipoAviso tipo;
    private final String pedidoId;
    private boolean lido;

    public Aviso(String id, long seq, String cpf, TipoAviso tipo, String pedidoId, boolean lido) {
        this.id = id;
        this.seq = seq;
        this.cpf = cpf;
        this.tipo = tipo;
        this.pedidoId = pedidoId;
        this.lido = lido;
    }

    public String id() { return id; }
    public long seq() { return seq; }
    public String cpf() { return cpf; }
    public TipoAviso tipo() { return tipo; }
    public String pedidoId() { return pedidoId; }
    public boolean lido() { return lido; }

    public void marcarLido() {
        lido = true;
    }
}
