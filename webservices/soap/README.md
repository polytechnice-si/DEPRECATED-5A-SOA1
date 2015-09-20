# Cookbook: SOAP Web Service Implementation

  * Author: [Sébastien Mosser](mosser@i3s.unice.fr)
  * Version: 0.9

## Technological environment

  * Build: Maven (3)
  * Language: Java (8)
  * Deployment: OSGi components on top of Apache Service Mix (6.0.0)
  * Test: SoapUI (5.2.0) / JUnit (4.12)

## Specifications

The _Tax Computation System_ (TCS) implements a Web Service used to compute the amount of taxes to be payed according to the financial information of a given taxpayer (anonymized). Two methods can be used to perform such a computation:

  * The _simple_ method is only based on the income of the taxpayer (e.g., taxes = 20% of the income).
  * The _advanced_ method taxes both income and assets of the taxpayer. Using this method, the amount of taxes is adapted to the geographical area of the taxpayer (urban or countryside).

## Development

The first steps are dedicated to the setup of a Maven environment, and the implementation of the service.
 
### Step #1: Setting up the environment

We are using Maven to support the build. Here is the structure of a valid Maven project:

    azrael:soap mosser$ tree .
    .
    ├── pom.xml
    ├── src
    │   └── main
    │       ├── java
    │       └── resources
    └── test

  * The `pom.xml` file describes the project
  * The `src/main/java` directory contains the source code of the service
  * The `src/main/resources` directory contains additional resources
  * The `test` directory contains the unit testing source code for the service

### Step #2: Filling the POM description model

The __Project Object Model__ (POM) describes the service, its dependencies and how to build it. The important attributes are:

  * `groupId` & `artifactId`: pair of unique names used to identify the service in the Maven dependency system;
  * `name`: the name ServiceMix will display when listing components
  * `packaging`: we are building an OSGi `bundle`, not a classical JAR file;
  * `dependencies`: we rely on 2 artefacts from CXF: 
    *  `cxf-rt-frontend-jaxws` to access to the SOAP-based stack
    *  `cxf-rt-transports-http` to access to the HTTP transport layer
  * `build` / `maven-bundle-plugin`: this plugin adapts the build process to create an OSGi bundle. The important attributes are the following:
    * `Bundle-SymbolicName`: the name that ServiceMix will uses to identify the component;
    * `Export-Package`: the Java packages exported by the components and available for others OSGi components.

### Step #3: Implementing the Web Service

Like any web service, the TCS service is described by an interface: `TaxComputationService`. It exposes 2 operations, one for each available tax computation mechanism. The contents of this class is annotated with elements such as `@WebService`, `@WebMethod` and `@WebParam`, which are self-explaining.

The interface is then implemented with a regular class: `TaxComputationImpl`. One can notice that the `@WebService` annotation used here is more detailed than in the interface, as it refers to a concrete implementation instead of an interface definition.

The data structures used by the operations (_i.e._, `SimpleTaxRequest`, `AdvancedTaxRequest` and `TaxComputation`) are annotated with `@XML*` elements, describing how to serialize the class into an XML document.

**In this course, we are not interested by the contents of the operations. Mocks will always be preferred to complex implementations.**

## Deployment

Considering the implemented service, we need to make it compliant with the OSGi standard, and then package it before deploying it on top of our ESB.

### Step #4: Describing the OSGi component

In the `resources` directory, we rely on a file named `OSGI-INF/blueprint/blueprint.xml` that describes the service as an OSGi component. This file basically activates some CXF features (such as logging), and binds the service concrete implementation to a given endpoint URL:

```xml
<jaxws:endpoint id="taxcomp"
                implementor="fr.unice.polytech.soa1.cookbook.TaxComputationImpl"
                address="/TaxComputation">
</jaxws:endpoint>
```

### Step #5: Packaging the bundle

Let's rely on maven black magic: `mvn clean package`. The command produces in the `target` directory a JAR file that contains our bundle. The name of the file is based on the `artifactId` and `version` attributes,  here `ws-soap-1.0.jar`.

### Step #6: Deploying on top of ServiceMix

We consider here an up and running instance of ServiceMix (if not, install ServiceMix on your computer, and start it by running the `bin/servicemix` script.

```
azrael:bin mosser$ ./servicemix
Java HotSpot(TM) 64-Bit Server VM warning: ignoring option MaxPermSize=256M; support was removed in 8.0
Please wait while Apache ServiceMix is starting...
100% [========================================================================]

Karaf started in 10s. Bundle stats: 233 active, 233 total
 ____                  _          __  __ _      
/ ___|  ___ _ ____   _(_) ___ ___|  \/  (_)_  __
\___ \ / _ \ '__\ \ / / |/ __/ _ \ |\/| | \ \/ /
 ___) |  __/ |   \ V /| | (_|  __/ |  | | |>  < 
|____/ \___|_|    \_/ |_|\___\___|_|  |_|_/_/\_\

  Apache ServiceMix (6.0.0)

Hit '<tab>' for a list of available commands
and '[cmd] --help' for help on a specific command.
Hit '<ctrl-d>' or 'osgi:shutdown' to shutdown ServiceMix.

karaf@root>
``` 

To deploy the component, copy-paste the JAR file into the `deploy` directory of your local service mix instance. It triggers the hot deployment process available on the bus (look at the log file `data/log/servicemix.log`). 

The `bundle:list` command of the ServiceMix shell lists all the bundles known by the bus, including our new one at the bottom of the list:

```
karaf@root>bundle:list
START LEVEL 100 , List Threshold: 50
 ID | State  | Lvl | Version                            | Name                                      
----------------------------------------------------------------------------------------------------
...
234 | Active |  80 | 1.0                                | SOA1 :: SOAP-based implementation 
```

This commands tells us that our bundle is deployed under the id `234`. This key will be used by other commands to control its lifecycle:

  * `bundle:stop 234`: to stop the bundle
  * `bundle:start 234`: to start the bundle again
  * `bundle:uninstall 234`: to uninstall the bundle (removing it completely from ServiceMix). This can also be done by removing the jar file from the `deploy` directory.

Remark: an uninstalled bundle cannot be re-installed with the hot-deployment feature as is. Recreating the JAR is the most simple way to benefit from the hot deployment again,

### Step #7: Accessing the web service

Web services deployed on top of ServiceMix are available under the `cxf` url prefix, through an HTTP server deployed on port 8181. We named the service `TaxComputation` in the OSGi blueprint. As a consequence, the service contract is available at the following url: `http://localhost:8181/cxf/TaxComputation?wsdl`

## Testing

### Step #8: Unit testing

Service can (**must?**) be unit tested as classical code, from an internal point of view. Maven expects the JUnit test code to be under the `test/java` directory. Actually, unit testing should have been done **before** the deployment phase.

### Step #9: Functional testing with SoapUI

We consider here an up and running installation of SoapUI (Open-source version). Create a new _SOAP Project_, give the url of the WSDL contract, and tick the "_create requests for all operations_" box.

SoapUI allows one to declare SOAP messages, and execute the requests by sending the SOAP messages and retrieving the associated responses.

For example, here is a valid SOAP message for the `simple` operation:

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:cook="http://cookbook.soa1.polytech.unice.fr/">
   <soapenv:Header/>
   <soapenv:Body>
      <cook:simple>
         <simpleTaxInfo>
            <id>foo-bar-geek</id>
            <income>100</income>
         </simpleTaxInfo>
      </cook:simple>
   </soapenv:Body>
</soapenv:Envelope>
```

And here is the associated response message returned by the service:

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:simpleResponse xmlns:ns2="http://cookbook.soa1.polytech.unice.fr/">
         <simple_result>
            <amount>20.0</amount>
            <date>Tue Sep 01 15:34:09 CEST 2015</date>
            <identifier>foo-bar-geek</identifier>
         </simple_result>
      </ns2:simpleResponse>
   </soap:Body>
</soap:Envelope>
```

SoapUI also sllows one to describe TestSuite as scenarios, supporting the exchange of multiple messages with a service according to a given scenario.
