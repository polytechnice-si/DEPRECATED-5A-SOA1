package fr.unice.polytech.soa1.cookbook.flows;

import static fr.unice.polytech.soa1.cookbook.flows.utils.Endpoints.*;

import fr.unice.polytech.soa1.cookbook.flows.utils.LetterWriter;
import org.apache.camel.builder.RouteBuilder;


/**
 * feature:install http
 */
public class HandleACitizen extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		// Dead letter channel as a logger
		errorHandler(deadLetterChannel("log:deadPool"));

		// Route to handle a given Person
		from(HANDLE_CITIZEN)
				.log("    Routing ${body.lastName} according to income ${body.income}")
				.log("      Storing the Person as an exchange property")
				.setProperty("person", body())
				.log("      Calling an existing generator")
				.to("direct:generator")
				.setProperty("p_uuid", body())
				.setBody(simple("${property.person}"))
				.choice()
					.when(simple("${body.income} >= 42000"))
						.setProperty("tax_computation_method", constant("COMPLEX"))
						.to("direct:complexTaxMethod")
					.when(simple("${body.income} >= 0 && ${body.income} < 42000"))
						.setProperty("tax_computation_method", constant("SIMPLE"))
						.to("direct:simpleTaxMethod")
					.otherwise()
						.to("direct:badCitizen").stop() // stopping the route for bad citizens
				.end() // End of the content-based-router
				.setHeader("person_uid", simple("${property.person.uid}"))
				.multicast()
					.parallelProcessing()
					.to("direct:generateLetter")
					.to(STORE_TAX_FORM)
		;

		// bad information about a given citizen
		from("direct:badCitizen")
				.log("    Bad information for citizen ${body.lastName}, ending here.")
		;


		from("direct:generateLetter")
				.bean(LetterWriter.class, "write(${property.person}, ${body}, ${property.tax_computation_method})")
				.to(CSV_OUTPUT_DIRECTORY + "?fileName=${property.p_uuid}.txt")
		;
	}


}
