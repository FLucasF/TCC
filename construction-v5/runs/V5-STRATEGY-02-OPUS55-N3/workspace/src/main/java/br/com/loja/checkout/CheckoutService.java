package br.com.loja.checkout;

import br.com.loja.checkout.api.ItemRequest;
import br.com.loja.checkout.api.ResumoRequest;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.dominio.Resumo;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.Pagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final Catalogo<NivelClube> niveis;
    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formas;

    public CheckoutService(List<NivelClube> niveis, List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
            List<FormaPagamento> formas) {
        this.niveis = new Catalogo<>(niveis);
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.formas = new Catalogo<>(formas);
    }

    public Resumo calcular(ResumoRequest pedido) {
        Carrinho carrinho = carrinho(pedido.itens());
        NivelClube nivel = niveis.buscar(pedido.nivelClube()).orElseThrow(() -> erro("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.buscar(pedido.regiao()).orElseThrow(() -> erro("REGIAO_INVALIDA"));

        ModalidadeEntrega modalidade = modalidades.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> erro("MODALIDADE_INVALIDA"));
        exigir(modalidade.atende(carrinho), "MODALIDADE_INDISPONIVEL");

        Cupom cupom = cupom(pedido.cupom());
        exigir(cupom.aplicavel(carrinho), "CUPOM_NAO_APLICAVEL");

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.frete(modalidade.frete(carrinho));
        BigDecimal desconto = cupom.desconto(carrinho, frete);
        BigDecimal seguro = regiao.seguro(carrinho);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento forma = formas.buscar(pedido.formaPagamento())
                .orElseThrow(() -> erro("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = Optional.ofNullable(pedido.parcelas()).orElse(1);
        exigir(forma.parcelasPermitidas(parcelas), "PARCELAMENTO_INVALIDO");
        exigir(forma.atende(totalPedido), "FORMA_PAGAMENTO_INDISPONIVEL");
        Pagamento pagamento = forma.calcular(totalPedido, parcelas);

        return new Resumo(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivel.credito(carrinho),
                nivel.brinde(carrinho));
    }

    private Carrinho carrinho(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty() && itens.stream().allMatch(CheckoutService::itemValido),
                "PEDIDO_INVALIDO");
        return new Carrinho(itens.stream()
                .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList());
    }

    private static boolean itemValido(ItemRequest item) {
        return item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private Cupom cupom(String codigo) {
        if (codigo == null) {
            return Cupom.NENHUM;
        }
        return cupons.buscar(codigo).orElseThrow(() -> erro("CUPOM_INVALIDO"));
    }

    private static void exigir(boolean condicao, String codigoErro) {
        if (!condicao) {
            throw erro(codigoErro);
        }
    }

    private static CheckoutException erro(String codigo) {
        return new CheckoutException(codigo);
    }
}
