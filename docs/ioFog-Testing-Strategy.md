# Testing Strategy

Adding tests during the creative initial phases of development can derail progress. Nothing is stable yet, so how can you decide on proper tests? Yet knowing the types of tests that will soon be added can guide development in the right direction. This document lists the types of tests that will be applied to the product as development progresses. It should be expanded and clarified as time goes on.

The tests themselves will be implemented at at the appropriate stages. Unit tests, for example, will be added as soon as code structures exit prototyping and become mostly stable. Performance tests and usability tests will be added when entire end-to-end processes are functioning in the product. Security tests will be added mid-way through the product development but will be kept in mind from the very start of the engineering efforts.

#### Unit Tests

* Outcome-based tests for code classes (don't test each little method, test the actual entry and exit points that will be used in the program flow)
* Test for all input and output conditions in order to catch as many edge cases as possible
* Use unit tests to measure performance of code, but only fail the test if the performance metric has a hard limit that is understood
* Apply unit tests during the build process using standard Java tools and methods

#### Performance Tests

* Test the response time of the command line functionality
* Test the speed of setting up the default ioElement containers
* Test the installation time
* Test the time needed to provision an ioFog instance to a fog controller
* Test the data and message throughput
* Test the speed of setting up additional ioElement containers
* Test the speed of changing configuration and network settings for ioElement containers

#### Usability Tests

* Test the installation process on each version of Linux in the supported version list
* Test the command line functionality thoroughly
* Test using the help information provided by the program to learn how to operate the program
* Test the success rate of provisioning an ioFog instance to a fog controller

#### Security Tests

* Test the ability of a user with insufficient privileges to access the program and perform tasks
* Check the program against the Potential Security Vulnerabilities document guidelines to look for weaknesses
* Hire a 3rd party security expert to run penetration tests and evaluate the product
