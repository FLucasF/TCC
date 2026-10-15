package br.com.loja.checkout;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.Pagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Calcula o resumo da compra, conferindo cada escolha do cliente na ordem dos erros. */
@Service
public class CalculadoraResumo {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
            List<NivelClube> niveis, List<FormaPagamento> formasPagamento) {
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.niveis = new Catalogo<>(niveis);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    public Resumo calcular(Pedido pedido) {
        Carrinho carrinho = pedido.carrinho();
        NivelClube nivel = niveis.buscar(pedido.nivelClube()).orElseThrow(() -> recusa(Erro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.buscar(pedido.regiao()).orElseThrow(() -> recusa(Erro.REGIAO_INVALIDA));

        ModalidadeEntrega modalidade = modalidades.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> recusa(Erro.MODALIDADE_INVALIDA));
        exigir(modalidade.atende(carrinho), Erro.MODALIDADE_INDISPONIVEL);
        BigDecimal frete = nivel.isentaFrete() ? Dinheiro.ZERO : modalidade.frete(carrinho);

        BigDecimal desconto = cupom(pedido.cupom())
                .map(cupom -> {
                    exigir(cupom.aplicavel(carrinho), Erro.CUPOM_NAO_APLICAVEL);
                    return cupom.desconto(carrinho, frete);
                })
                .orElse(Dinheiro.ZERO);

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal seguro = regiao.seguro(carrinho);
        BigDecimal totalPedido = Dinheiro.centavos(subtotal.subtract(desconto).add(frete).add(seguro));

        FormaPagamento forma = formasPagamento.buscar(pedido.formaPagamento())
                .orElseThrow(() -> recusa(Erro.FORMA_PAGAMENTO_INVALIDA));
        exigir(forma.permiteParcelas(pedido.parcelas()), Erro.PARCELAMENTO_INVALIDO);
        exigir(forma.atende(totalPedido), Erro.FORMA_PAGAMENTO_INDISPONIVEL);
        Pagamento pagamento = forma.pagar(totalPedido, pedido.parcelas());

        return new Resumo(
                subtotal,
                Dinheiro.centavos(desconto),
                Dinheiro.centavos(frete),
                modalidade.prazoDias(),
                seguro,
                Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido)),
                Dinheiro.centavos(pagamento.totalFinal()),
                pedido.parcelas(),
                Dinheiro.centavos(pagamento.valorParcela()),
                nivel.creditoProximaCompra(carrinho),
                nivel.brinde(carrinho));
    }

    /** Cupom ausente é pedido sem cupom; cupom informado precisa existir. */
    private Optional<Cupom> cupom(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.of(cupons.buscar(codigo).orElseThrow(() -> recusa(Erro.CUPOM_INVALIDO)));
    }

    private static void exigir(boolean condicao, Erro erro) {
        if (!condicao) {
            throw recusa(erro);
        }
    }

    private static PedidoRecusadoException recusa(Erro erro) {
        return new PedidoRecusadoException(erro);
    }
}
