package fr.unice.polytech.soa1.cookbook.flows;

import static fr.unice.polytech.soa1.cookbook.flows.utils.Endpoints.*;

import org.apache.camel.builder.RouteBuilder;


public class HandleACitizen extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		// Dead letter channel as a logger
		errorHandler(deadLetterChannel("log:deadPool"));

		// Route to handle a given Person
		from(HANDLE_CITIZEN_ASYNCHRONOUS)
				.log("    Routing ${body.lastName} according to geographical area [${body.zipCode}]")
				.choice()
					.when(simple("${body.zipCode} > 0 && ${body.zipCode} < 50000"))
						.to("direct:complexTaxMethod")
					.when(simple("${body.zipCode} >= 50000"))
						.to("direct:simpleTaxMethod")
					.otherwise()
						.to("direct:badCitizen").stop() // stopping the route for bad citizens
				.end() // End of the content-based-router
				.log("    Executed for each good citizen, here ${body.lastName}");

		// Using the  complex method
		from("direct:complexTaxMethod")
				.log("    Computing ${body.lastName} with advanced computation");

		// Using the simple method
		from("direct:simpleTaxMethod")
				.log("    Computing ${body.lastName} with simple computation");

		// bad information about a given citizen
		from("direct:badCitizen")
				.log("    Bad information for citizen ${body.lastName}");
	}

}
