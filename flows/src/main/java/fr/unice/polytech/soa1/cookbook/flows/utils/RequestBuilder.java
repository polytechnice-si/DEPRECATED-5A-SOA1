package fr.unice.polytech.soa1.cookbook.flows.utils;

import fr.unice.polytech.soa1.cookbook.flows.business.Person;

/**
 * This file is part of the system project
 *
 * @author mosser (19/10/2015, 12:28)
 **/
public class RequestBuilder {

	public String buildSimpleRequest(Person p, String uuid) {
		StringBuilder builder = new StringBuilder();
		builder.append("<cook:simple xmlns:cook=\"http://cookbook.soa1.polytech.unice.fr/\">\n");
		builder.append("  <simpleTaxInfo>\n");
		builder.append("    <id>"     + uuid          + "</id>\n");
		builder.append("    <income>" + p.getIncome() + "</income>\n");
		builder.append("  </simpleTaxInfo>\n");
		builder.append("</cook:simple>");
		return builder.toString();
	}

	public String buildAdvancedRequest(Person p, String uuid) {
		StringBuilder builder = new StringBuilder();
		builder.append("<cook:complex xmlns:cook=\"http://cookbook.soa1.polytech.unice.fr/\">\n");
		builder.append("  <complexTaxInfo>\n");
		builder.append("    <id>"     + uuid           + "</id>\n");
		builder.append("    <income>" + p.getIncome()  + "</income>\n");
		builder.append("    <assets>" + p.getAssets()  + "</assets>\n");
		builder.append("    <zone>"   + p.getZipCode() + "</zone>\n");
		builder.append("  </complexTaxInfo>\n");
		builder.append("</cook:complex>");
		return builder.toString();
	}
}
