package fr.unice.polytech.soa1.cookbook.flows.utils;

/**
 * This file is part of the ws-flows project
 *
 * @author mosser (15/10/2015, 16:25)
 **/
public class Endpoints {


	public static String CSV_INPUT_DIRECTORY = "file:camel/input";

	public static String CSV_OUTPUT_DIRECTORY = "file:camel/output";

	public static String HANDLE_CITIZEN_ASYNCHRONOUS = "activemq:handleACitizen";

	public static String HANDLE_CITIZEN_SYNCHRONOUS = "activemq:handleACitizen";

}
