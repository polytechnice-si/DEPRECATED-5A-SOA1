package fr.unice.polytech.soa1.cookbook.flows;

import static fr.unice.polytech.soa1.cookbook.flows.utils.Endpoints.*;
import org.apache.camel.builder.RouteBuilder;


public class HandleACitizen extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		from(HANDLE_CITIZEN_ENDPOINT)
				.log("Handling a citizen with message ${id}")
				.to("log:loggingExample");

	}

}
