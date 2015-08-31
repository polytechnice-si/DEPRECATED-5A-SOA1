# SOA: Integration & ESB Cookbook

  * Author: [Sébastien Mosser](mosser@i3s.unice.fr)
  * Reviewer: [Mireille Blay-Fornarino](blay@i3s.unice.fr)
  * Version: 2.0

This repository is a set or _recipes_ to be used as a support for the SOA-1 course. The goal of a _recipe_ is to describe an up and running system, and how one can reproduce it step by step.

**This is not a lab session. Students are not expected to reproduce this system as is. The point is to adapt these recipes to your own problem.**

## Technological Environment

The first version of this course (2012) was using Petals ESB. It then evolves to Mule (2013, 2014). Based on the feedback received from the students and according to the 8 weeks time box of the course, we finally decided to re-implement it (again...) using the open source suite developed by Apache. Thus, the technological stack is the following:

  * Enterprise Service Bus: [Apache Service Mix](http://servicemix.apache.org/) (6.0.0)
  * Routing: [Apache Camel](http://camel.apache.org/)
  * Service Definition: [Apache CXF](http://cxf.apache.org/)
    * SOAP: JAX-WS specification
    * REST: JAX-RS specification

## Use Case

The integration scenario developed in this cookbook binds a _Tax Paying System_ (TPS) to a _Tax Payer Information System_ (TAIS). The TAIS contains personal information about each taxpayer in the country. The TPS implementation and hosting are outsourced. Taxes can be computed using the "simple" or "advanced" method, according to a threshold based on raw income (changing each year). The goal of the integration is to support:

  * The automation of a mailing campaign, sending to each tax payer the tax form with the computed amount of taxes;
  * The definition of a web-based system where a tax payer can consult the amount of taxes to be payed.

## Repository Architecture

The file system is organized as the following:

  * `webservices/soap`: example of SOAP-based web services
  * `webservices/rest`: example of REST-based web services
  * `flows`: integration flows binding TPS and TAIS together.
