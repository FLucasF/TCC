package br.com.loja.checkout;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.dominio.Resumo;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.Pagamento;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CalculadoraResumo {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
            List<NivelClube> niveis, List<FormaPagamento> formasPagamento) {
        this.modalidades = new Catalogo<>(modalidades, ModalidadeEntrega::codigo);
        this.cupons = new Catalogo<>(cupons, Cupom::codigo);
        this.niveis = new Catalogo<>(niveis, NivelClube::codigo);
        this.formasPagamento = new Catalogo<>(formasPagamento, FormaPagamento::codigo);
    }

    /** As validações seguem a ordem combinada; o primeiro problema encontrado é o devolvido. */
    public Resumo calcular(Pedido pedido) {
        Carrinho carrinho = pedido.carrinho();
        NivelClube nivel = niveis.buscar(pedido.nivelClube())
                .orElseThrow(() -> new CheckoutException("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.deCodigo(pedido.regiao())
                .orElseThrow(() -> new CheckoutException("REGIAO_INVALIDA"));
        ModalidadeEntrega modalidade = modalidades.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));
        exigir(modalidade.atende(carrinho), "MODALIDADE_INDISPONIVEL");

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.frete(modalidade.frete(carrinho));
        BigDecimal descontoCupom = descontoCupom(pedido.cupom(), carrinho, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.centavos(subtotal.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento forma = formasPagamento.buscar(pedido.formaPagamento())
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));
        exigir(forma.permiteParcelas(pedido.parcelas()), "PARCELAMENTO_INVALIDO");
        exigir(forma.atende(totalPedido), "FORMA_PAGAMENTO_INDISPONIVEL");
        Pagamento pagamento = forma.pagar(totalPedido, pedido.parcelas());

        return new Resumo(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido)),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private BigDecimal descontoCupom(String codigo, Carrinho carrinho, BigDecimal frete) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));
        exigir(cupom.aplicavel(carrinho), "CUPOM_NAO_APLICAVEL");
        return cupom.desconto(carrinho, frete);
    }

    private static void exigir(boolean condicao, String codigoErro) {
        if (!condicao) {
            throw new CheckoutException(codigoErro);
        }
    }
}
