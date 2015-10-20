#Cookbook: Integration Flows with Apache Camel

  * Author: [Sébastien Mosser](mosser@i3s.unice.fr)
  * Version: 0.1

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
    * if the income is greater than 42,000Kr, use the `complex` computation method
    * if the income is lesser than 42,000Kr and positive, use the `simple` computation method
    * if the income is negative, identify an issue with this citizen (e.g., log it)
  3. In parallel:
    * Write the letter to be sent to the tax payer by snail mail
    * trigger an integration flow that will store the amount of tax to be paid in the system 

  

