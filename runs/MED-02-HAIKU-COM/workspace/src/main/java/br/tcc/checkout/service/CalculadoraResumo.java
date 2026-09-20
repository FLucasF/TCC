package br.tcc.checkout.service;

import java.math.BigDecimal;
import java.util.List;
import br.tcc.checkout.cupom.Cupom;
import br.tcc.checkout.cupom.CupomFactory;
import br.tcc.checkout.domain.Item;
import br.tcc.checkout.domain.Pedido;
import br.tcc.checkout.dto.ResumoResponse;
import br.tcc.checkout.entrega.Entrega;
import br.tcc.checkout.entrega.EntregaFactory;
import br.tcc.checkout.pagamento.BoletoPagamento;
import br.tcc.checkout.pagamento.FormaPagamento;
import br.tcc.checkout.pagamento.FormaPagamentoFactory;
import br.tcc.checkout.util.Arredondador;

public class CalculadoraResumo {
    private Pedido pedido;
    private BigDecimal subtotalProdutos;
    private BigDecimal descontoCupom;
    private BigDecimal frete;
    private Integer prazoEntregaDias;
    private BigDecimal totalSemAjuste;
    private BigDecimal ajustePagamento;
    private BigDecimal totalFinal;
    private Integer parcelas;
    private BigDecimal valorParcela;

    public CalculadoraResumo(Pedido pedido) {
        this.pedido = pedido;
        this.parcelas = pedido.getParcelas() != null ? pedido.getParcelas() : 1;
    }

    public ResumoResponse calcular() throws ResumoException {
        validarPedidoBasico();
        validarEntrega();

        calcularSubtotal();

        validarCupom();
        validarFormaPagamento();

        calcularDesconto();
        calcularFrete();
        calcularTotalSemAjuste();

        validarBoletoPorLimite();

        calcularAjuste();
        calcularTotalFinal();
        calcularParcelas();

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                prazoEntregaDias,
                ajustePagamento,
                totalFinal,
                parcelas,
                valorParcela);
    }

    private void validarPedidoBasico() throws ResumoException {
        List<Item> itens = pedido.getItens();

        if (itens == null || itens.isEmpty()) {
            throw new ResumoException("PEDIDO_INVALIDO");
        }

        for (Item item : itens) {
            if (item.getPrecoUnitario() == null
                    || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResumoException("PEDIDO_INVALIDO");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new ResumoException("PEDIDO_INVALIDO");
            }
            if (item.getPesoKg() == null || item.getPesoKg() < 0) {
                throw new ResumoException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarEntrega() throws ResumoException {
        String modalidade = pedido.getModalidadeEntrega();

        if (modalidade == null || modalidade.isEmpty()) {
            throw new ResumoException("MODALIDADE_INVALIDA");
        }

        if (!EntregaFactory.existe(modalidade)) {
            throw new ResumoException("MODALIDADE_INVALIDA");
        }

        Double pesoTotal = calcularPesoTotal();
        Entrega entrega = EntregaFactory.criar(modalidade);

        if (!entrega.podeAtender(pesoTotal)) {
            throw new ResumoException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarCupom() throws ResumoException {
        String cupom = pedido.getCupom();

        if (cupom == null || cupom.isEmpty()) {
            return;
        }

        if (!CupomFactory.existe(cupom)) {
            throw new ResumoException("CUPOM_INVALIDO");
        }

        Cupom cupomObj = CupomFactory.criar(cupom, pedido.getItens());
        if (!cupomObj.aplicavel(subtotalProdutos)) {
            throw new ResumoException("CUPOM_NAO_APLICAVEL");
        }
    }

    private void validarFormaPagamento() throws ResumoException {
        String forma = pedido.getFormaPagamento();

        if (forma == null || forma.isEmpty()) {
            throw new ResumoException("FORMA_PAGAMENTO_INVALIDA");
        }

        if (!FormaPagamentoFactory.existe(forma)) {
            throw new ResumoException("FORMA_PAGAMENTO_INVALIDA");
        }

        FormaPagamento formaPagamento = FormaPagamentoFactory.criar(forma);
        if (!formaPagamento.parcelavalida(parcelas)) {
            throw new ResumoException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarBoletoPorLimite() throws ResumoException {
        String forma = pedido.getFormaPagamento();
        if (forma.equals("BOLETO") && totalSemAjuste.compareTo(BoletoPagamento.LIMITE_MAXIMO) > 0) {
            throw new ResumoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private void calcularSubtotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : pedido.getItens()) {
            BigDecimal precoItem = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            total = total.add(precoItem);
        }
        subtotalProdutos = Arredondador.arredondar(total);
    }

    private void calcularDesconto() {
        String cupom = pedido.getCupom();
        if (cupom == null || cupom.isEmpty()) {
            descontoCupom = Arredondador.arredondar(BigDecimal.ZERO);
            return;
        }

        Cupom cupomObj = CupomFactory.criar(cupom, pedido.getItens());
        Entrega entrega = EntregaFactory.criar(pedido.getModalidadeEntrega());
        BigDecimal freteTemp = entrega.calcularFrete(calcularPesoTotal());
        descontoCupom = cupomObj.calcularDesconto(subtotalProdutos, freteTemp);
    }

    private void calcularFrete() {
        Entrega entrega = EntregaFactory.criar(pedido.getModalidadeEntrega());
        frete = entrega.calcularFrete(calcularPesoTotal());
        prazoEntregaDias = entrega.getPrazoEntregaDias();
    }

    private void calcularTotalSemAjuste() {
        totalSemAjuste = subtotalProdutos.subtract(descontoCupom).add(frete);
        totalSemAjuste = Arredondador.arredondar(totalSemAjuste);
    }

    private void calcularAjuste() {
        FormaPagamento formaPagamento = FormaPagamentoFactory.criar(pedido.getFormaPagamento());
        totalFinal = formaPagamento.calcularTotalFinal(totalSemAjuste, parcelas);
        ajustePagamento = totalFinal.subtract(totalSemAjuste);
    }

    private void calcularTotalFinal() {
    }

    private void calcularParcelas() {
        FormaPagamento formaPagamento = FormaPagamentoFactory.criar(pedido.getFormaPagamento());
        valorParcela = formaPagamento.calcularValorParcela(totalSemAjuste, parcelas);
    }

    private Double calcularPesoTotal() {
        Double peso = 0.0;
        for (Item item : pedido.getItens()) {
            peso += item.getPesoKg() * item.getQuantidade();
        }
        return peso;
    }
}
