package br.com.loja.checkout;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.Pagamento;
import br.com.loja.checkout.pedido.Carrinho;
import br.com.loja.checkout.pedido.PedidoRequest;
import br.com.loja.checkout.pedido.ResumoCompra;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ResumoCompraService {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public ResumoCompraService(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
                               List<NivelClube> niveis, List<FormaPagamento> formasPagamento) {
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.niveis = new Catalogo<>(niveis);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    /** As validações seguem a ordem combinada para os códigos de erro. */
    public ResumoCompra calcular(PedidoRequest pedido) {
        Carrinho carrinho = Carrinho.de(pedido.itens());
        NivelClube nivel = niveis.obter(pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = Regiao.obter(pedido.regiao());

        ModalidadeEntrega modalidade = modalidades.obter(pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        exigir(modalidade.atende(carrinho), "MODALIDADE_INDISPONIVEL");
        BigDecimal frete = nivel.frete(modalidade.frete(carrinho));

        Optional<Cupom> cupom = Optional.ofNullable(pedido.cupom())
                .filter(codigo -> !codigo.isBlank())
                .map(codigo -> cupons.obter(codigo, "CUPOM_INVALIDO"));
        exigir(cupom.map(c -> c.aplicavel(carrinho)).orElse(true), "CUPOM_NAO_APLICAVEL");
        BigDecimal descontoCupom = cupom.map(c -> c.desconto(carrinho, frete)).orElse(Dinheiro.ZERO);

        FormaPagamento formaPagamento = formasPagamento.obter(pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = Objects.requireNonNullElse(pedido.parcelas(), 1);
        exigir(formaPagamento.permiteParcelas(parcelas), "PARCELAMENTO_INVALIDO");

        BigDecimal seguro = regiao.seguro(carrinho.subtotal());
        BigDecimal totalPedido = carrinho.subtotal().subtract(descontoCupom).add(frete).add(seguro);
        exigir(formaPagamento.atende(totalPedido), "FORMA_PAGAMENTO_INDISPONIVEL");
        Pagamento pagamento = formaPagamento.pagar(totalPedido, parcelas);

        return new ResumoCompra(
                carrinho.subtotal(),
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.creditoProximaCompra(carrinho.subtotal()),
                nivel.brinde(carrinho.subtotal()));
    }

    private static void exigir(boolean condicao, String erro) {
        if (!condicao) {
            throw new PedidoRecusadoException(erro);
        }
    }
}
