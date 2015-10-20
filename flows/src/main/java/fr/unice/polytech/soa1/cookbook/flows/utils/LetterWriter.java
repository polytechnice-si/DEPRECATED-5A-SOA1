package fr.unice.polytech.soa1.cookbook.flows.utils;


import fr.unice.polytech.soa1.cookbook.flows.business.Person;
import fr.unice.polytech.soa1.cookbook.flows.business.TaxForm;

public class LetterWriter {

	public String write(Person p, TaxForm form, String method) {
		StringBuilder b = new StringBuilder();

		b.append("Dear " + p.getFirstName() + " " + p.getLastName() + ", \n");
		b.append("\n");
		b.append("  Address: " + p.getAddress() + " " + p.getZipCode() + "\n");
		b.append("  ID: " + p.getUid() + "\n");
		b.append("\n\n");
		b.append("Taxes computed using the " + method + " method on " + form.getDate() + "\n");
		b.append("\n\n");
		b.append("Amount to pay: " + form.getAmount() + "\n");
		b.append("\n");

		return b.toString();
	}
}
