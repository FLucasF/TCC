package br.com.loja.pedidos.aviso;

public record AvisoResposta(long id, String tipo, long pedidoId, String situacao) {

    public static AvisoResposta de(Aviso aviso) {
        return new AvisoResposta(aviso.id(), aviso.tipo().name(), aviso.pedidoId(), aviso.situacao().name());
    }
}
