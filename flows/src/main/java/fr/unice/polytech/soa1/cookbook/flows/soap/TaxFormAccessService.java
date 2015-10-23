package fr.unice.polytech.soa1.cookbook.flows.soap;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;


@WebService(name="TaxFormAccess")
public interface TaxFormAccessService {


	@WebMethod(operationName = "retrieveTaxFormFromUID")
	@WebResult(name="amount")
	double getTaxForm(@WebParam(name="request") String request);

}
