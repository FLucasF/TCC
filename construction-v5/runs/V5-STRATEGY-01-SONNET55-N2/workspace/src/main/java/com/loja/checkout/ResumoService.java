package com.loja.checkout;

import static com.loja.checkout.dominio.CodigoErro.CUPOM_INVALIDO;
import static com.loja.checkout.dominio.CodigoErro.CUPOM_NAO_APLICAVEL;
import static com.loja.checkout.dominio.CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL;
import static com.loja.checkout.dominio.CodigoErro.FORMA_PAGAMENTO_INVALIDA;
import static com.loja.checkout.dominio.CodigoErro.MODALIDADE_INDISPONIVEL;
import static com.loja.checkout.dominio.CodigoErro.MODALIDADE_INVALIDA;
import static com.loja.checkout.dominio.CodigoErro.NIVEL_CLUBE_INVALIDO;
import static com.loja.checkout.dominio.CodigoErro.PARCELAMENTO_INVALIDO;
import static com.loja.checkout.dominio.CodigoErro.REGIAO_INVALIDA;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Opcoes;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.Entrega;
import com.loja.checkout.pagamento.Cobranca;
import com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private final Opcoes<Entrega> entregas;
    private final Opcoes<Cupom> cupons;
    private final Opcoes<NivelClube> niveis;
    private final Opcoes<FormaPagamento> pagamentos;

    public ResumoService(List<Entrega> entregas, List<Cupom> cupons, List<NivelClube> niveis,
                         List<FormaPagamento> pagamentos) {
        this.entregas = new Opcoes<>(entregas);
        this.cupons = new Opcoes<>(cupons);
        this.niveis = new Opcoes<>(niveis);
        this.pagamentos = new Opcoes<>(pagamentos);
    }

    public Resumo calcular(PedidoEntrada pedido) {
        Carrinho carrinho = pedido.carrinho();
        NivelClube nivel = niveis.buscar(pedido.nivelClube())
                .orElseThrow(() -> new PedidoRecusadoException(NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.buscar(pedido.regiao())
                .orElseThrow(() -> new PedidoRecusadoException(REGIAO_INVALIDA));
        Entrega entrega = entregas.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> new PedidoRecusadoException(MODALIDADE_INVALIDA));
        if (!entrega.disponivelPara(carrinho)) {
            throw new PedidoRecusadoException(MODALIDADE_INDISPONIVEL);
        }

        BigDecimal frete = nivel.freteDevido(entrega.frete(carrinho));
        Optional<Cupom> cupom = buscarCupom(pedido.cupom());
        if (cupom.isPresent() && !cupom.get().aplicavelA(carrinho)) {
            throw new PedidoRecusadoException(CUPOM_NAO_APLICAVEL);
        }
        BigDecimal desconto = cupom.map(c -> c.desconto(carrinho, frete)).orElse(Dinheiro.ZERO);

        FormaPagamento pagamento = pagamentos.buscar(pedido.formaPagamento())
                .orElseThrow(() -> new PedidoRecusadoException(FORMA_PAGAMENTO_INVALIDA));
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal seguro = regiao.seguro(carrinho);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);
        if (!pagamento.disponivelPara(totalPedido)) {
            throw new PedidoRecusadoException(FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new Resumo(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(totalPedido),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.creditoProximaCompra(carrinho),
                nivel.brinde(carrinho));
    }

    private Optional<Cupom> buscarCupom(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.of(cupons.buscar(codigo).orElseThrow(() -> new PedidoRecusadoException(CUPOM_INVALIDO)));
    }
}
