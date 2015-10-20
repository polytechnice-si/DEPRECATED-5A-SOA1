package fr.unice.polytech.soa1.cookbook.flows;

import fr.unice.polytech.soa1.cookbook.flows.utils.Database;
import org.apache.camel.builder.RouteBuilder;

import static fr.unice.polytech.soa1.cookbook.flows.utils.Endpoints.*;

public class TaxFormAccessService extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		// Store a tax form in the database
		from(STORE_TAX_FORM)
				.log("\n\n\n\n[${headers}]\n\n\n\n")
				.bean(Database.class, "setData(${header.person_uid}, ${body})")
		;

		// Retrieve a taxform in the database
		from("direct:getTaxForm")
				.bean(Database.class, "getData(${header.uid})")
		;

		/**
		 * Exposing how to retrieve a tax form as a REST service
		 */

		restConfiguration().component("servlet");     // feature:install camel-servlet    + blueprint

		rest("/taxForm/{uid}")
				.get()
				.to("direct:getTaxForm")
		;


		/**
		 * Exposing how to retrieve a tex form as a SOAP service
		 */



	}

}
