package br.com.loja.checkout.resumo;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.clube.Vantagens;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.Pagamento;
import br.com.loja.checkout.seguro.Regiao;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CalculadoraResumo {

    private final Catalogo<NivelClube> niveis;
    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(
            List<NivelClube> niveis,
            List<ModalidadeEntrega> modalidades,
            List<Cupom> cupons,
            List<FormaPagamento> formasPagamento) {
        this.niveis = new Catalogo<>(niveis);
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    /** Segue a ordem de conferência dos erros: cada etapa só é calculada depois que a anterior foi validada. */
    public Resumo calcular(Compra compra) {
        Carrinho carrinho = carrinhoValido(compra.itens());
        BigDecimal subtotal = carrinho.subtotal();

        NivelClube nivel = niveis.buscar(compra.nivelClube())
                .orElseThrow(() -> new CheckoutException("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.buscar(compra.regiao())
                .orElseThrow(() -> new CheckoutException("REGIAO_INVALIDA"));
        ModalidadeEntrega modalidade = modalidades.buscar(compra.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));
        if (!modalidade.atende(carrinho)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Vantagens vantagens = nivel.vantagens(subtotal);
        BigDecimal frete = vantagens.isentaFrete() ? Dinheiro.ZERO : modalidade.frete(carrinho);

        BigDecimal descontoCupom = descontoCupom(compra.cupom(), carrinho, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento forma = formasPagamento.buscar(compra.formaPagamento())
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));
        if (!forma.aceitaParcelas(compra.parcelas())) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!forma.atende(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Pagamento pagamento = forma.pagar(totalPedido, compra.parcelas());

        return new Resumo(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                vantagens.credito(),
                vantagens.brinde());
    }

    private static Carrinho carrinhoValido(List<Item> itens) {
        if (itens == null || itens.isEmpty() || !itens.stream().allMatch(CalculadoraResumo::itemValido)) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        return new Carrinho(itens);
    }

    private static boolean itemValido(Item item) {
        return item != null
                && positivo(item.precoUnitario())
                && item.quantidade() > 0
                && positivo(item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private BigDecimal descontoCupom(String codigo, Carrinho carrinho, BigDecimal frete) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));
        if (!cupom.aplicavel(carrinho)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(carrinho, frete);
    }
}
