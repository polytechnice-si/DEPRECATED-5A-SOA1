package fr.unice.polytech.soa1.cookbook.flows;


import static fr.unice.polytech.soa1.cookbook.flows.utils.Endpoints.*;

import fr.unice.polytech.soa1.cookbook.flows.business.Person;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.CsvDataFormat;

import java.util.Map;

/**
 * Route loading a given CSV file, transforming it into objects and transferring each object to another route
 *
 *   - in ServiceMix, to load the CSV transformation service:
 *     karaf@root> feature:install camel-csv
 *
 */
public class HandleCSVFile extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		from(CSV_INPUT_DIRECTORY)
				.log("Processing ${file:name}")
				.log("  Loading the file as a CSV document")
				.unmarshal(buildCsvFormat())  // Body is now a List(Map("navn" -> ..., ...), ...)
				.log("  Splitting the content of the file into atomic lines")
				.split(body())
				.log("  Transforming a CSV line into a Person")
				.process(csv2person)
				.log("  Transferring to the route that handle a given citizen")
				.to(HANDLE_CITIZEN)   // Async transfer with JMS ( activemq:... )
				;

	}

	/**
	 * Helpers to support the implementation of the route
	 */

	// transform a CSV file delimited by commas, skipping the headers and producing a Map as output
	private static CsvDataFormat buildCsvFormat() {
		CsvDataFormat format = new CsvDataFormat();
		format.setDelimiter(",");
		format.setSkipHeaderRecord(true);
		format.setUseMaps(true);
		return format;
	}


	// Process a map<String -> Object> into a person
	private static Processor csv2person = new Processor() {

		public void process(Exchange exchange) throws Exception {
			// retrieving the body of the exchanged message
			Map<String, Object> input = (Map<String, Object>) exchange.getIn().getBody();
			// transforming the input into a person
			Person output =  builder(input);
			// Putting the person inside the body of the message
			exchange.getIn().setBody(output);
		}

		private Person builder(Map<String, Object> data) {
			Person p = new Person();
			// name
			String name =  (String) data.get("Navn");
			p.setFirstName((name.split(",")[1].trim()));
			p.setLastName((name.split(",")[0].trim()));
			// zip code
			p.setZipCode(Integer.parseInt((String) data.get("Postnummer")));
			// address
			p.setAddress((String) data.get("Postaddressen"));
			// email
			p.setEmail((String) data.get("Epost"));
			// Unique identifier
			p.setUid((String) data.get("Fodselsnummer"));
			// Money
			p.setIncome(getMoneyValue(data, "Inntekt"));
			p.setAssets(getMoneyValue(data, "Formue"));
			return p;
		}

		private int getMoneyValue(Map<String, Object> data, String field) {
			String rawIncome = (String) data.get(field);
			return Integer.parseInt(rawIncome.replace(",", "").substring(0, rawIncome.length() - 3));
		}
	};


}
