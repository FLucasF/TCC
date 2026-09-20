package br.tcc.checkout.model;

public enum ModalidadeEntrega {
	ECONOMICA(12.00, 2.00, 7),
	EXPRESSA(25.00, 4.50, 2),
	RETIRADA_LOJA(0.00, 0.00, 1),
	MOTOBOY(18.00, 0.00, 0);

	private final Double baseCost;
	private final Double costPerKg;
	private final Integer prazoEntregaDias;

	ModalidadeEntrega(Double baseCost, Double costPerKg, Integer prazoEntregaDias) {
		this.baseCost = baseCost;
		this.costPerKg = costPerKg;
		this.prazoEntregaDias = prazoEntregaDias;
	}

	public Double getBaseCost() {
		return baseCost;
	}

	public Double getCostPerKg() {
		return costPerKg;
	}

	public Integer getPrazoEntregaDias() {
		return prazoEntregaDias;
	}

	public Double calcularFrete(Double pesoTotalKg) {
		return baseCost + (costPerKg * pesoTotalKg);
	}

	public static ModalidadeEntrega fromString(String value) {
		if (value == null) {
			return null;
		}
		try {
			return ModalidadeEntrega.valueOf(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
