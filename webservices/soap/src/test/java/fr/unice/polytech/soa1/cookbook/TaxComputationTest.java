package fr.unice.polytech.soa1.cookbook;

import junit.framework.TestCase;
import org.junit.*;
import fr.unice.polytech.soa1.cookbook.data.*;
import java.util.UUID;


public class TaxComputationTest extends TestCase {

	private TaxComputationService service = null;
	protected void setUp() { this.service = new TaxComputationImpl(); }

	@Test
	public void test_simple_method() {
		SimpleTaxRequest request = buildSimpleTaxRequest(100);
		TaxComputation result = service.simple(request);
		assertEquals(request.getIdentifier(),result.getIdentifier());
		assertEquals(result.getAmount(), 20.0f);
	}

	@Test
	public void test_complex_method() {
		// Countryside computation
		AdvancedTaxRequest req1 = buildAdvancedTaxRequest(100, 50, "1000");
		TaxComputation res1 = service.complex(req1);
		assertEquals(req1.getIdentifier(),res1.getIdentifier());
		assertEquals(res1.getAmount(), 23.0f);

		// Urban computation
		AdvancedTaxRequest req2 = buildAdvancedTaxRequest(100, 50, "2000");
		TaxComputation res2 = service.complex(req2);
		assertEquals(req2.getIdentifier(),res2.getIdentifier());
		assertEquals(res2.getAmount(), 26.0f);
	}

	/**
	 * Private helpers
	 */

	private SimpleTaxRequest buildSimpleTaxRequest(float income){
		SimpleTaxRequest req = new SimpleTaxRequest();
		req.setIdentifier(UUID.randomUUID().toString());
		req.setIncome(income);
		return req;
	}

	private AdvancedTaxRequest buildAdvancedTaxRequest(float income, float assets, String zone ) {
		AdvancedTaxRequest req = new AdvancedTaxRequest();
		req.setIdentifier(UUID.randomUUID().toString());
		req.setIncome(income);
		req.setAssets(assets);
		req.setZone(zone);
		return req;
	}
}
