package loja.checkout;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import loja.checkout.clube.NivelClube;
import loja.checkout.cupom.Cupom;
import loja.checkout.entrega.ModalidadeEntrega;
import loja.checkout.pagamento.FormaPagamento;

@Service
public class CalculadoraResumo {

    private final Indice<NivelClube> niveis;
    private final Indice<ModalidadeEntrega> modalidades;
    private final Indice<Cupom> cupons;
    private final Indice<FormaPagamento> formasPagamento;

    public CalculadoraResumo(List<NivelClube> niveis, List<ModalidadeEntrega> modalidades,
                             List<Cupom> cupons, List<FormaPagamento> formasPagamento) {
        this.niveis = new Indice<>(niveis, NivelClube::codigo);
        this.modalidades = new Indice<>(modalidades, ModalidadeEntrega::codigo);
        this.cupons = new Indice<>(cupons, Cupom::codigo);
        this.formasPagamento = new Indice<>(formasPagamento, FormaPagamento::codigo);
    }

    public Resumo calcular(Pedido pedido) {
        Carrinho carrinho = carrinho(pedido);
        NivelClube nivel = niveis.buscar(pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = Regiao.de(pedido.regiao()).orElseThrow(() -> new ErroCheckout("REGIAO_INVALIDA"));
        ModalidadeEntrega modalidade = modalidades.buscar(pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!modalidade.atende(carrinho)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.frete(modalidade.frete(carrinho));

        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (pedido.cupom() != null && !pedido.cupom().isBlank()) {
            Cupom cupom = cupons.buscar(pedido.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(carrinho)) {
                throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.desconto(carrinho, frete);
        }

        FormaPagamento pagamento = formasPagamento.buscar(pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!pagamento.parcelasPermitidas(parcelas)) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);
        if (!pagamento.disponivel(totalPedido)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        FormaPagamento.Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new Resumo(subtotal, descontoCupom, frete, modalidade.prazoDias(), seguro,
                cobranca.totalFinal().subtract(totalPedido), cobranca.totalFinal(), parcelas,
                cobranca.valorParcela(), nivel.credito(subtotal), nivel.brinde(subtotal));
    }

    private Carrinho carrinho(Pedido pedido) {
        List<Pedido.ItemPedido> itens = pedido.itens();
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(CalculadoraResumo::invalido)) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        return new Carrinho(itens.stream()
                .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList());
    }

    private static boolean invalido(Pedido.ItemPedido item) {
        return item == null
                || item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().signum() <= 0;
    }
}
