package fr.unice.polytech.soa1.cookbook.flows.business;


public class TaxForm {

	private double amount;
	private String date;

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	@Override
	public String toString() {
		return "TaxForm {" +
				"amount=" + amount +
				", date='" + date + '\'' +
				'}';
	}
}
