#Cookbook: Integration Flows with Apache Camel

  * Author: [Sébastien Mosser](mosser@i3s.unice.fr)
  * Version: 1.0

## Technological Environment

  * Build: Maven (3)
  * Language: Java (8)
  * Deployment: OSGi components on top of Apache Service Mix (6.0.0)
  * Routing framework: Camel (2.15.2)

## Specifications

The goal of this integration demonstration is to build a system that bind together the TAIS and TCS services defined in this repository. Two uses cases are exposed:

  * From a large CSV file containing all the information about the taxpayers, compute anonymously the amount of taxes they have to pay, and produce a letter for each tax payer, to be sent by snail mail
  * Expose a web service allowing a tax payer to consult the computed amount online.

Two datasets are available: a small one containing 5 taxpayers, and a large one with more data. These datasets are available in the `datasets` directory.
  
## Setup

### Step #1: Setting up the environment

We are using maven to support the build. The architecture of the project is classical:

    azrael:flows mosser$ tree .
    .
    ├── README.md
    ├── pom.xml
    └──  src
        └── main
            ├── java
            └── resources
                     
### Step #2: Filling the POM description model

The POM file describes the component. The important parts are the following:

  * `groupId` & `artifactId`: pair of unique names used to identify the service in the Maven dependency system;
  * `name`: the name ServiceMix will display when listing components
  * `packaging`: we are building an OSGi `bundle`, not a classical JAR file;
  * `dependencies`: we (initially) rely on 1 artefact from Camel: 
    *  `camel-core` the core of the routing engine (necessary artefacts)
  * `build` / `maven-bundle-plugin`: this plugin adapts the build process to create an OSGi bundle. The important attributes are the following:
    * `Bundle-SymbolicName`: the name that ServiceMix will uses to identify the component;
    * `Export-Package`: the Java packages exported by the components and available for others OSGi components.


### Step #3: Declaring Camel flows to the ESB

We start this cookbook with the definition of a flow that loads a CSV file and extract each entry in the file, transform it into a `Person` and send it to another flow that will handle each citizen.

The Camel framework allows us to define integration flows as _routes_. We need  to declare in the blueprint of the bundle where the routes are located to support a proper deployment of the flows. In the `resources/OSGI_ING/blueprint` directory, we define a `camel-context.xml` file, that basically exposes the contents of a given Java package as integration flows using the `package` node. Any route defined inside this package will be understood as an integration flow by the bundle container.

    <camelContext xmlns="http://camel.apache.org/schema/blueprint">
        <package>fr.unice.polytech.soa1.cookbook.flows</package>
    </camelContext>

## Step #4: Creating a simple flow using the Java DSL

A flow is defined in a simple Java class that extends the `RouteBuilder` superclass defined by Camel. It defines a `configure` method, allowing designers to build one (or more) flow.

We rely on the Java API for Camel, which is an _internal Domain-specific language_ (DSL). It uses the _method chaining_ pattern to support the definition of routes. A route definition starts with a call to the `from` function, and ends with a call to the `to` function.

Our route will do the following:

  1. load a file put in a given directory (_e.g._, `camel/input`) 
  2. transform this file into a collection of CSV entries
  3. split the flow to handle each entry separately
  4. transform a CSV entry into an instance of the `Person` class
  5. send this person to a flow that will handle if properly.

Using the EIP graphical language (which is also a DSL by the way), the flow is defined as the following:
![](https://raw.githubusercontent.com/polytechnice-si/5A-2015-SOA-1/develop/flows/docs/handleCsvFile.png)

Apache Camel provides elements that implements EIP. This is not a one-to-one mapping, for example classical transformation (here file -> CSV) are automatically provided, where custom transformation must be implemented by hand (here entry -> Person).

### Implementing the flow

The flow is implemented in the file named `HandleCSVFile.java`. Restricted to its essence, it is defined as the following :

```java
from("file:camel/input")
   .unmarshal(buildCsvFormat())
   .split(body())
   .process(csv2person)
   .to("direct:handleACitizen")  		
   ;			
```
 * `from`: the URI used in the method asks the routing engine to listen to the directory named `camel/input` (w.r.t the root of the ServiceMix installation).
 * `unmarshal`: runs a transformation from `File` to `List<Map<String,Object>>`. This transformation is configured by the `buildCsvFormat` method (see below)
 * `split`: implements the Splitter integration pattern. We ask Camel to split the flow based on the `body()` of the received message
 * `process`: calls a processor, _i.e._, a function that process a message. Here, we use a processor named `csv2person` to implement our transformation
 * `to`: forward the message to another route. The `direct` URL prefix maps to internal routing inside the camel engine, which is synchronous.

### Starting and ending a flow: From / To 

The `from` and `to` methods allows one to connect an `endpoint` to another one. Endpoints are specified using URIs. The list of URIs supported by Camel is here: [http://camel.apache.org/uris.html]()

### Transforming data with unmarshal

the `unmarshal` method implements a classical transformation that might exists between heterogeneous but standard data format. In our case, the goal is to transform a plain file into a list of CSV entries. 

The list of all supported data formats available in Camel is here: [[http://camel.apache.org/data-format.html]]()

With these classical transformation, you are expected to configure a DataFormat to align it with your very specific need, and then apply the transformation automatically. In our case, the configuration is the following:

  * The CSV file is delimited by commas (",")
  * The first line of the file contains the header (i.e., the title of each column)
  * We want to produce entries as maps, i.e., a data structure where the title of each column is bound to the value associated to this very specific line.

```java
private static CsvDataFormat buildCsvFormat() {
	CsvDataFormat format = new CsvDataFormat();
	format.setDelimiter(",");
	format.setSkipHeaderRecord(true);
	format.setUseMaps(true);
	return format;
}
```

Applying this unmarshalling transformation to our file creates a collection of Map.

ServiceMix is defined in a modular way. By default, it does not load the CSV support. To install this feature in your local setup of the ESB, use the following command in the Karaf shell:

    karaf@root> feature:install camel-csv


### Splitting the message flow

The `split` method triggers the execution of the remaining flow elements on each part of the message used in the splitter. 

In our case, we are splitting the body of the message. Thus, each element in the collection is considered as the message to be transferred to the next processor.


### Transforming a CSV entry into an Object

The class `Person` represents a tax payer. We need to implement a `Processor` that will transform the received map into a real `Person`.

A processor is a class implementing the `Processor` interface. The only method to be override is the `process` one. It takes as input an `Exchange` (stuff exchanged between elements in a flow) and is defined as `void`. As a consequence, your responsibility is to fill the received exchange with the processed element.

In our case, we create an anonymous implementation of a Processor named `csv2person`. It retrieves the body of the exchange, build a person from this very body and put the resulting person inside the exchange. The contents of the `builder` method is not interesting, it basically implements the glue between the two data format (_e.g._, how to extract first and last names fromn the same field in the CSV entry).

```java
private static Processor csv2person = new Processor() {

	public void process(Exchange exchange) throws Exception {
		// retrieving the body of the exchanged message
		Map<String, Object> input = (Map<String, Object>) exchange.getIn().getBody();
		// transforming the input into a person
		Person output = builder(input);
		// Putting the person inside the body of the message
		exchange.getIn().setBody(output);
	}

	private Person builder(Map<String, Object> data) {
		Person p = new Person();
		// set p's attribute based on the contents of data
		return p;
	}

}
```
### Managing endpoints
  
Endpoints are used by Camel to forward messages. We used the `direct` URI prefix, asking for a direct connection between the two flows. This kind of connexion is synchronous and can be understood as a method call in classical object languages: we are sending a message to another entity.

To switch to asynchronous messaging, we have to rely on another kind of connexion between the two flows. For example, JMS messaging will support reliable and asynchronous messaging.

As a consequence, we simply change the URI to a pattern that will bind the endpoint to an ActiveMQ one (JMS implementation provided by apache and optimized for Camel): `to("activemq:handleACitizen);`

By simply changing the pattern, the ESB will create an ActiveMQ queue, support asynchronous messaging and make your flow execution parallel. Automagically.

__Remark__: Everything comes with a cost. Contrarily to the `direct` endpoint which is internal, using a JMS queue in your flow implies that your business objects are consistent with the exchange protocol. In our case, the body of a JMS message must implement `Serializable` (the message body will be null elsewhere).

### Deploying and running the flow

To build the OSGi bundle, we rely on maven to package the code:

    azrael:flows mosser$ mvn clean package
    
Like any OSGi bundle, copy-paste the jar created in the `target` directory in the `deploy` directory of your local ServiceMix installation.

The flow listens to a directory named `camel/input`. This directory is relative to the path of the ServiceMix local installation. If your system is located in the `servicemix` directory:

  1. Go to this directory: `cd /.../servicemix`
  2. Start the ESB: `./bin/servicemix`
  3. The flow listens to the following directory: `./camel/input` (create it if it does not exist)

## Step #5: Consuming Web Services from a Camel Flow

We can now design the flow dedicated to the integration of the Tax Computation System, _i,e,_, the flow that will actually compute the amount of tax each tax payer present in the CSV file have to pay.

The logic of the integration flow is the following:

  1. Call the UUID generator (Restful service) to obtain an UUID associated to the current Person
  2. Based on the income of this person:
    * if the income is greater than 42,000Kr, use the `complex` computation method to obtain a `TaxForm`
    * if the income is lesser than 42,000Kr (and positive), use the `simple` computation method to obtain a `TaxForm`
    * if the income is negative, identify an issue with this citizen (e.g., log it)
  3. In parallel:
    * Write the letter to be sent to the tax payer by snail mail using the `Person` and the `TaxForm` data
    * Trigger an integration flow that will store the amount of tax to be paid in the system 

Using the EIP graphical language, this flow is modeled as the following:
![](https://raw.githubusercontent.com/polytechnice-si/5A-2015-SOA-1/develop/flows/docs/handleACitizen.png)


### Implementing the flow with Camel

The flow is implemented in the file named `HandleACitizen.java`.

```java
from("activemq:handleACitizen")
	.setProperty("person", body())
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
	.multicast()
		.parallelProcessing()
		.to("direct:generateLetter")
		.to(STORE_TAX_FORM)
;
```



### Adapting the exchanged message: Data Enricher, Content Filter, Claim Check.

The flow calls the UUID generator, and as a consequence must store _somewhere_ the received Person. The very same situation occurs with the received UUID, which must be stored as the remaining flow works on a Person. It also happens when the flow must remember which computation method is actually used. These steps are modeled as _Claim Checks_ in the flow, considering that the key maps to the concept one wants to remember.

Using Camel, this _trick_ is done using the `Property` concept. One can store an `Object` in a property using the `setProperty` flow element. To retrieve a property `p`, the [SIMPLE](http://camel.apache.org/simple.html) language allows one to access it as the following: `${property.p}`

The content of the body can be adapted directly using the `setBody` method. For example, calling `setBody(${property.p})` replaces the current body by the value of `p`. In our case, it is useful to replace the computed UUID by a Person, as the remaining flow elements work on a Person.
  
### Routing Messages: Choice and Multicast propagation  

The `choice` construction implements conditional branching. Each branch is designed using a `when` construction, and the default case uses the `otherwise` keyword. Conditions are expressed using the [SIMPLE](http://camel.apache.org/simple.html) language, which allows one to access message attributes in a _simple_ way.

In the designed flow, the default case matches when we do not have enough information to compute the tax to be paid for the current citizen. As a consequence the flow must be ended here for this citizen. We use the `stop` keyword to model this design decision. The `end` keyword is used to close the choice and join the remaining routes to the next flow element.

At the end of the flow, the computed `TaxForm` is processed by two flows in parallel: _(i)_ the `direct:writeLetter` one to generate the letter to be sent and _(ii)_ a JMS queue used to store the computed forms. This parallel propagation of the very same message (the two channels will receive the exact same message) is designed with the `multicast` construction, coupled to a `parallelProcessing` configuration to enact a concurrent propagation.

### Calling a REST Service

To call a REST service, one simply has to perform a _Request/Reply_ call to the service, using the HTTP transport protocol. In our case, it means to wrap our message in a GET http request, reset the body of the message (a GET does not need to send anything) and perform the call. As the call returns a Stream instead of the contents of the response, we apply a data transformation that will flush this stream, and then remove the extra quotes added by the generator but not really useful in our case.

![](https://raw.githubusercontent.com/polytechnice-si/5A-2015-SOA-1/develop/flows/docs/generator.png)

```java
from("direct:generator")
	.setHeader(Exchange.HTTP_METHOD, constant("GET"))
	.setBody(constant(""))
	.to("http://localhost:8181/cxf/demo/generators/demogen")
	.process(readResponseStream)
	.process(uuidCleaner)
;
```

In this implementation, `readResponseStream` and `uuidCleaner` are two processors used to perform the two data transformation at the end of the flow. The details of the implementation are available in the file named `CallExternalPartners.java`.

To suport HTTP endpoints in ServiceMix, install the `camel-http` feature: `feature:install camel-http`

### Calling a SOAP Service

To call a SOAP service, the idea is similar to the REST one. First prepare the request to be sent, then send it using the SOAP protocol on top of HTTP and finally process te response to create a business object.

![](https://raw.githubusercontent.com/polytechnice-si/5A-2015-SOA-1/develop/flows/docs/tcs_soap.png)

```java
from("direct:simpleTaxMethod")
	.bean(RequestBuilder.class, "buildSimpleRequest(${body}, ${property.p_uuid})")
	.to("spring-ws://http://localhost:8181/cxf/TaxComputation")
	.process(result2taxForm)
;
```

The endpoint starting with `spring-ws://` triggers the use of the SOAP protocol, automatically. We do not have to focus on SOAP concepts, and we only focus on the exchanged messages (as XML document) in the request and in the response. To support it in ServiceMix: `feature:install camel-spring-ws `

To build the request, we use a helper bean that is dedicated to this task. A bean is a simple java class, and the `bean` construction allows one to call a given method from a class as a flow activity. The implementation of the bean is a classical java class. Arguments of the called method are bound using the SIMPLE language.

```java
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
	
	// ...
}
```

To retrieve the data useful to fill a `TaxForm` for this citizen, we use XPath on the retrieved XML response with a processor named `result2taxForm`. One can also decide to use JAXB and the `unmarshal` construction to automate this task for complex responses. The XPath processor does not come by default in ServiceMix, use the `feature:install camel-saxon` command to load it

```java
private static Processor result2taxForm = new Processor() {
	private XPath xpath = XPathFactory.newInstance().newXPath();    

	public void process(Exchange exchange) throws Exception {
		Source response = (Source) exchange.getIn().getBody();
		TaxForm result = new TaxForm();
		result.setAmount(Double.parseDouble(xpath.evaluate("//amount/text()", response)));
		result.setDate(xpath.evaluate("//date/text()", response));
		exchange.getIn().setBody(result);
	}
};
```

### Dynamic endpoint

In our example, we produce one letter per citizen. Thus, the output file used to store the letter is dynamically computed based on the current flow execution. In other case, one might have to access to a remote service with an URL associated to the currently handled data (_e.g._, the id of an Order to be added at the end of a GET request URL). 

As endpoints are simple string, dynamic endpoints are done thanks to string manipulation. In our case, the `file` endpoint expects a directory, and the output file can be provided as a parameter using the `fileName` option. The SIMPLE language can be used in a `to` construction to dynamically address an endpoint.

In the following flow, we use a bean to write the letter, and then a dynamic endpoint to store the letter in a file named after the retrieved UUID for this person.

```java
from("direct:generateLetter")
	.bean(LetterWriter.class, "write(${property.person}, ${body}, ${property.tax_computation_method})")
	.to("file:camel/output?fileName=${property.p_uuid}.txt")
```

## Step #6: Exposing Integration Flows as Services

### Flows to interact with the TaxForm database

We need two flows to interact with the database: one to store the computed `TaxForm` inside the system and another one to allow the system to query the stored forms.

![](https://raw.githubusercontent.com/polytechnice-si/5A-2015-SOA-1/develop/flows/docs/db_flows.png)

We expose the storing flow using JMS, to support scalability. This flow is triggered by the `HandleACitizen` one, being asynchronous in the way the form are stored ensure that this one will scale while handling multiple citizen at the same time: forms will be stored in the database when enough resources will be available to read them from the waiting queue. From a business point of view, there is more value in computing the tax form than in accessing it.

**Remark**: using JMS as exchange channel between two flows destroy the stored properties. As a consequence, the unique identifier used to store the tax form must be transferred in the headers of the message in the `HandleACitizen` flow. An alternative is to store in the body of the message   all the necessary information (_i.e._, creating a new serializable class binding a `Person` to a `TaxForm` and instantiating it in the body of the message before pushing it to the JMS queue).

The database is mocked as a static hashmap in a Bean, implemented in the `Database` class. The flow used to access to the content of the database assumes that the body of the exchange is the unique identifier associated to each person in the CSV file. It produces as output the amount of tax to be paid

```java
from("activemq:storeTaxForm)
		.bean(Database.class, "setData(${header.person_uid}, ${body})")
;

from("direct:getTaxForm")
		.bean(Database.class, "getData(${body})")
		.setBody(simple("${body.amount}"))
;		
```

### Service Exposition Architecture 

![](https://raw.githubusercontent.com/polytechnice-si/5A-2015-SOA-1/develop/flows/docs/service_exposition.png)

One of the main advantage of an ESB is the diversity that exists in the multiple connectors available in the bus. We leverage this diversity by exposing the `getTaxForm` integration flow using both REST and SOAP connectors. 

This architecture is implemented in the `TaxFormAccessRoute.java` file.

#### Exposing a flow as a resource

Camel support multiple ways to expose resources over HTTP. The less invasive (_i.e_, the one with few interferences with the other components already deployed in the bus) is to rely on a dedicated servlet (use `feature:install camel-servlet` to install it in ServiceMix). 

This servlet is defined in the `camel-context.xml` file, and basically binds an HTTP listener to a given URL prefix (in our case `/camel/rest`).

```xml
<reference id="httpService" interface="org.osgi.service.http.HttpService"/>

<bean class="org.apache.camel.component.servlet.osgi.OsgiServletRegisterer"
      init-method="register"
      destroy-method="unregister">
    <property name="alias" value="/camel/rest"/>
    <property name="httpService" ref="httpService"/>
    <property name="servlet" ref="camelServlet"/>
</bean>

<bean id="camelServlet" class="org.apache.camel.component.servlet.CamelHttpTransportServlet"/>
```

Then, one can rely on the [internal language available in Camel](http://camel.apache.org/rest-dsl.html) to create resources exposed as REST services. the first point is to bind this language to the servlet just defined. Then, one can define a resources `/taxForm/{uid}`, available as a `get`, and redirect it to a camel route. One should notice that this language is different from the integration flow one, and is much more limited. In our case, we need to retrieve the `uid` from the `headers` and transfer it as the body of the exchange to the `getTaxForm` flow. This can only be done in a regular flow, so we create a route dedicated to this task at the implementation level.

```java
restConfiguration().component("servlet"); 

rest("/taxForm/{uid}")
		.get()
		.to("direct:getTaxFormFromREST")
;

from("direct:getTaxFormFromREST")
		.setBody(simple("${header.uid}"))
		.to("direct:getTaxForm")
;
```

After deployment, the service is available as `http://localhost:8181/camel/rest/taxForm/{uid}`.


#### Exposing a flow as a SOAP service

One can define a SOAP service using a contract-first or code-first approach. In this section, we use a code-first approach as it is much more simple.

The first step is to define the interface of the service to be exposed, as a Java interface.

```java
@WebService(serviceName = "TaxFormAccessService")
public interface TaxFormAccessService {

	@WebMethod(operationName = "retrieveTaxFormFromUID")
	@WebResult(name="amount")
	double getTaxForm(@WebParam(name="request") String request);

}
```

Then, we simply create a CXF endpoint to expose this interface as a SOAP service. Considering that this method accepts a String as input, we can directly forward the contents of the body to the  `getTaxForm` flow. The only precaution is to filter based on the `operationName` header.

```java
from("cxf:/TaxAccessService?serviceClass=fr.unice.polytech.soa1.cookbook.flows.soap.TaxFormAccessService")
		.filter(simple("${in.headers.operationName} == 'retrieveTaxFormFromUID'"))
		.to("direct:getTaxForm")
;
```

The service is available at the following URL: [http://localhost:8181/cxf/TaxAccessService?wsdl](http://localhost:8181/cxf/TaxAccessService?wsdl)
 
**Warning**: when an interface exposes multiple operations, this method must be adapted to fit the different operations. fixing this cookbook is still an ongoing work.

**Tips**: if for any reason the web service layer start to throw exceptions and server errors, use the following command to reset the web service support in ServiceMix: `feature:uninstall cxf`. If you restart the bus, it will automatically re-install CXF in a proper way with the good dependencies.
