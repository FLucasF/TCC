package br.com.loja.checkout.api;

import br.com.loja.checkout.calculo.Item;
import br.com.loja.checkout.calculo.Pedido;
import br.com.loja.checkout.calculo.PedidoRecusadoException;
import br.com.loja.checkout.calculo.Regiao;
import br.com.loja.checkout.catalogo.Catalogo;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.EscolhaPagamento;
import br.com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Confere o que o site enviou, na ordem definida pelo negocio, e monta o
 * pedido validado. Cada conferencia pergunta ao proprio caso se ele atende o
 * pedido; aqui so mora a ordem.
 */
@Component
public class MontadorPedido {

    private final Catalogo<ModalidadeEntrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveisClube;
    private final Catalogo<FormaPagamento> formasPagamento;

    public MontadorPedido(Catalogo<ModalidadeEntrega> entregas,
                          Catalogo<Cupom> cupons,
                          Catalogo<NivelClube> niveisClube,
                          Catalogo<FormaPagamento> formasPagamento) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public PedidoValidado montar(RequisicaoResumo requisicao) {
        List<Item> itens = itens(requisicao);
        NivelClube clube = exigir(niveisClube.buscar(requisicao.nivelClube()), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = exigir(Regiao.CATALOGO.buscar(requisicao.regiao()), "REGIAO_INVALIDA");
        ModalidadeEntrega entrega =
                exigir(entregas.buscar(requisicao.modalidadeEntrega()), "MODALIDADE_INVALIDA");

        BigDecimal pesoKg = itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
        recusarSe(!entrega.atende(pesoKg), "MODALIDADE_INDISPONIVEL");

        Optional<Cupom> cupom = cupom(requisicao.cupom());
        Pedido pedido = new Pedido(itens, entrega, cupom, clube, regiao);
        recusarSe(cupom.isPresent() && !cupom.get().aplicavel(pedido.contextoCupom()),
                "CUPOM_NAO_APLICAVEL");

        FormaPagamento forma =
                exigir(formasPagamento.buscar(requisicao.formaPagamento()), "FORMA_PAGAMENTO_INVALIDA");
        EscolhaPagamento pagamento =
                new EscolhaPagamento(forma, Optional.ofNullable(requisicao.parcelas()).orElse(1));
        recusarSe(!pagamento.parcelamentoPermitido(), "PARCELAMENTO_INVALIDO");
        recusarSe(!pagamento.atende(pedido.totalPedido()), "FORMA_PAGAMENTO_INDISPONIVEL");

        return new PedidoValidado(pedido, pagamento);
    }

    private List<Item> itens(RequisicaoResumo requisicao) {
        List<ItemRequisicao> recebidos = requisicao.itens();
        recusarSe(recebidos == null || recebidos.isEmpty(), "PEDIDO_INVALIDO");
        return recebidos.stream().map(this::item).toList();
    }

    private Item item(ItemRequisicao recebido) {
        recusarSe(recebido == null, "PEDIDO_INVALIDO");
        recusarSe(!positivo(recebido.precoUnitario()), "PEDIDO_INVALIDO");
        recusarSe(!positivo(recebido.pesoKg()), "PEDIDO_INVALIDO");
        recusarSe(recebido.quantidade() == null || recebido.quantidade() <= 0, "PEDIDO_INVALIDO");
        return new Item(recebido.nome(), recebido.precoUnitario(), recebido.quantidade(),
                recebido.pesoKg());
    }

    private Optional<Cupom> cupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(exigir(cupons.buscar(codigo), "CUPOM_INVALIDO"));
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private <T> T exigir(Optional<T> encontrado, String codigoDoErro) {
        return encontrado.orElseThrow(() -> new PedidoRecusadoException(codigoDoErro));
    }

    private void recusarSe(boolean problema, String codigoDoErro) {
        if (problema) {
            throw new PedidoRecusadoException(codigoDoErro);
        }
    }
}
