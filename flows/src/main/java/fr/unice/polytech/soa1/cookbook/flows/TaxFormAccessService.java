package fr.unice.polytech.soa1.cookbook.flows;

import org.apache.camel.builder.RouteBuilder;

import static fr.unice.polytech.soa1.cookbook.flows.utils.Endpoints.*;

public class TaxFormAccessService extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		from(STORE_TAX_FORM)
				.to("log:storing");

	}

}
