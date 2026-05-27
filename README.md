# hmpps-subject-access-request-lib

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-subject-access-request-lib)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-subject-access-report-lib "Link to report")

Helper library with common code and testing patterns related to subject access requests

## Overview

This library consists of two individual libraries related to subject access requests:

* Common code to be used by subject access request services such as rendering reports from templates.  
* Testing helpers and patterns that are to be used by service teams to help them detect changes to subject access
  request related data and templates. 

## Release Notes

##### [2.x](release-notes/2.x.md)
##### [1.x](release-notes/1.x.md)

## Usage

### Main library

This is only to be used by subject access request related services. Add the following to `build.gradle.kts`

    implementation("uk.gov.justice.service.hmpps:hmpps-subject-access-request-lib:<library-version>")

Once added then access to the common classes and code should be available.

### Test library

This is to be used by services that implement the subject access request APIs for returning data and templates. Add the
following to `build.gradle.kts`

    testImplementation("uk.gov.justice.service.hmpps:hmpps-subject-access-request-test-support:<library-version>") 

Once added then access to the test helper classes and code should be available, further details on how the library can 
help with implementing test is given below. 

## Implementing SAR Tests

As mentioned the main purpose of the test library is to help teams to implement tests in their services to ensure that
any changes to subject access request related data that will be provided via their API and rendered in the final report 
via their subject access request template can be detected. More information on how the various types of test to ensure 
consistency with subject access request related datas can be found [here](https://dsdmoj.atlassian.net/wiki/spaces/HAA/pages/5925503414/HMPPS+Subject+Access+Request+Test+Library).

### Use base class

The class `SarIntegrationTestBase` can be used as the basic starting point for implementing a test. Creating a test that
extends this class will automatically set up the test as a `SpringBootTest` and wire up an instance of
`SarIntegrationTestHelper`, which is the main class that provides the helper functions such as calling the subject
access request API and rendering reports using the template.   

### Use own base class

If you already have an existing base test class that you use for integration tests then there is no need to use the one
provided by the library. When using your existing base class to inherit from you must also import the 
`SarIntegerationHelperConfig` which will allow access to an instance of `SarIntegrationTestHelper`:

    @Import(SarIntegrationTestHelperConfig::class)
    class SubjectAccessRequestIntegrationTest : MyIntegrationTestBase() {
    
        @Autowired
        lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

        ...
    }


When implementing the test interfaces below to add tests to your class you will need to pass the
`SarIntegrationTestHelper` instance when implementing the `getSarHelper` method:

    override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

Also needed when implementing the test interfaces will be an instance of `WebTestClient` which will be used to access
the APIs of your service, assuming this is made available through your existing base class, and via implementing the 
`getWebTestClientInstance` method:   

    override fun getWebTestClientInstance(): WebTestClient = webTestClient

### Test interfaces

There are a number of interfaces that exists which contain the tests that can be added to your test class. The idea of
interfaces is that there is no mandation to implement all the tests giving some flexibility in deciding which tests you
think would be required to catch changes in your service. However, it is recommended that at least the report generation
test provided by `SarReportTest` is implemented to verify the generation of the report HTML and which generates a pdf
version which can provided to the Offender SAR team for reviews when there are any changes.

#### Generating the actual content files

For most of the tests described below there are expected content files that need to be defined that are used to compare
the actual generated content with. It is possible to generate the actual content for the tests in files to make it
easier to initialise the expected content files for the first time and for direct comparisons when differences are
found. To do this you need to run the tests with the environment variable `SAR_GENERATE_ACTUAL` set with a value `true`.
For example if running from the command line:

    SAR_GENERATE_ACTUAL=true ./gradlew test --tests "uk.gov.justice.digital.hmpps.MyTest"

This will generate a file for each of the data schema, API, and report generation tests above in the
`src/test/resources` folder of your project:

| Test type   | Generated file                |
|-------------|-------------------------------|
| Data schema | entity-schema.json.log        |
| API         | sar-api-response.json.log     |
| Report      | sar-generated-report.html.log |

(The files have the extension .log to ensure they are ignored by git and don't get committed)

> **_NOTE:_**  The Report test will always generate a pdf report in `build/test-generated` when run regardless of the
> use of the above flag

#### Data schema tests
 
The interfaces `SarFlywaySchemaTest` and `SarJpaEntitiesTest` when implemented by your test class will add tests that
will validate the database schema for changes.

The interface `SarFlywaySchemaTest` adds a test that checks if the current Flyway schema has changed, and can be used 
where Flyway is used by your service to manage database schema changes. When implemented by a test a
`javax.sql.DataSource` instance needs to be provided via implementing the `getDataSourceInstance` method:

    override fun getDataSourceInstance(): DataSource = dataSource

The expected version of the schema that the test will expect to find will need to be provided by defining the property
`hmpps.sar.tests.expected-flyway-schema-version` in your project.

The interface `SarJpaEntitiesTest` adds a test that generates a JSON representation of the current JPA Entity model of
the service and compares to a known value in a file. This can be used to provide greater detail on differences when
changes are detected compared to the Flyway schema version check, also if Flyway is not currently being used by your
service. When implemented by a test a `jakarta.persistence.EntityManager` instance needs to be provided via implementing
the `getEntityManagerInstance` method:

    override fun getEntityManagerInstance(): EntityManager = entityManager

The location of the known JPA entity model JSON file will need to be provided by defining the property
`hmpps.sar.tests.expected-jpa-entity-schema.path` in your project (to generate the actual entity schema ensure the file
defined by this property exists and run the test with `SAR_GENERATE_ACTUAL=true` see
[here](#generating-the-actual-content-files)).

#### API test

The interface `SarApiDataTest` when implemented adds a test that calls the subject access request data endpoint of your
service and compares the JSON response to a known value in a file. This will verify that there are no changes to the
output of the subject access request data API, and relies on a full set of data related to an offender to be set up by
implementing the `setupTestData` method:

    override fun setupTestData() {
        val job = Job(tile = "Driver")
        jobRepository.save(job)
    }

If is easier to set up the required data via sql scripts and run by annotating tests with the
`org.springframework.test.context.jdbc.@Sql` annotation then this can be done by overriding the test method in your test
class:

    @Test
    @Sql(
        "classpath:test_data/seed-licence-id-1.sql",
    )
    override fun `SAR API should return expected data`() {
        super.`SAR API should return expected data`()
    }

The other method that will need to be implemented for this test is one of either `getPrn` or `getCrn` depending on
whether your service relates to prisons and references offenders with PRNs:

    override fun getPrn(): String? = "A1234AA"

or relates to probation and uses CRNs:

    override fun getCrn(): String? = "X123456"

This offender id should match the one related to the data set up for the test (either in the `setupTestData`
implementation or otherwise).

Additional from and to date values can be provided by overriding `getFromDate` and `getToDate` methods which will ensure
the request made will use those parameters, otherwise the default will be to not use any parameters to restrict the
fetched data to any date range:

    override fun getFromDate(): LocalDate? = LocalDate.parse("2023-01-01")
    override fun getToDate(): LocalDate? = LocalDate.parse("2026-01-01")

If the type of the content section of the response needs to be specified, e.g. there are custom deserialisers that need
to be applied, then the `getContentType` method should be overridden to set the content section type to the specific
class required (the default being Any):

    override fun getContentType(): Class<*> = MySarContentType::class.java 

Once the required methods have been implemented in the test class, two properties will need to be defined. The property
`hmpps.sar.tests.expected-api-response.path` will need to be set to the location of the known expected subject access
request API json response file that the test will compare the actual results with (to generate the actual api response
ensure the file defined by this property exists and run the test with `SAR_GENERATE_ACTUAL=true` see
[here](#generating-the-actual-content-files)). The property `hmpps.sar.tests.attachments-expected` can be set to
indicate if attachments data is to be expected in the response, with a value of either `true` or `false`, although the
default of false will be used of this property is not defined.    

#### Report generation test

The interface `SarReportTest` when implemented adds a test that retrieves the JSON data by calling the subject access
request data endpoint of your service and then uses the subject access request template returned by the template API of
your service to generate an HTML report. This report is then compared with an expected HTML report and fails if any
differences are found.

As with the test provided by the `SarApiDataTest` interface, this relies on a full set of data related to an offender to
be set up by implementing the same `setupTestData` method and uses the same offender id from either `getPrn` or `getCrn`
implementations. The optional `getFromDate` and `getToDate` values will be used to restrict the data used to 
generate the report to a specified range. If the return type of the content section needs to be specified then override
the `getContentType` method. 

The additional setup required for this test is the setting of the property
`hmpps.sar.tests.expected-render-result.path` which defines the location of the expected HTML report to compare with the
actual generated one in the test (to generate the actual rendered HTML ensure the file defined by this property exists
and run the test with `SAR_GENERATE_ACTUAL=true` see [here](#generating-the-actual-content-files)).

If inline image attachments are present in the template (via the use of the `inlineAttachment` or
`inlineAttachmentContent` template helper functions), then the images for those can be provided by overriding the
`getInlineAttachments` method. The implementation should return a map matching the download `url` in the data response
to the resource file path. For example, if the data contains the following:

    "attachment": {
        "contentType": "image/gif",
        "url": "http://image-one"
    }

and the image is located in `src/test/resources/sar/image.gif`, then the `getInlineAttachments` method should be
implemented as below:

    override fun getInlineAttachments(): Map<String, String> = mapOf(
        "http://image-one" to "/sar/image.gif",
    )

Everytime this test is run it will generate pdf of the report as it would appear in the final SAR report, located at
`build/test-generated/sar-generated-report.pdf`. This report can be provided to the Offender SAR team to be used for
reviews of changes made to the report. 

#### Using the helper directly

The `SarIntegrationTestHelper` instance can be used directly to create your own bespoke tests in your test class without
using the interfaces above if you prefer. However, you will need to be confident that they are appropriate and will
provide the level of confidence required to catch any potential changes to the generated subject access report for your
service.

#### Example test class implementing all tests

    class SubjectAccessRequestIntegrationTest: SarIntegrationTestBase(), SarApiDataTest, SarReportTest, SarFlywaySchemaTest, SarJpaEntitiesTest {
        
        @Autowired
        lateinit var dataSource: DataSource
        
        @Autowired
        lateinit var entityManager: EntityManager

        override fun setupTestData() {
            // Data setup 
        }
        
        override fun getPrn(): String? = "A1234AA"
        
        override fun getDataSourceInstance(): DataSource = dataSource
        
        override fun getEntityManagerInstance(): EntityManager = entityManager
    }

#### Example test class using own base class

    @Import(SarIntegrationTestHelperConfig::class)
    class SubjectAccessRequestIntegrationTest : MyIntegrationTestBase(), SarApiDataTest, SarReportTest, SarFlywaySchemaTest, SarJpaEntitiesTest {
    
        @Autowired
        lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper
        
        @Autowired
        lateinit var dataSource: DataSource
        
        @Autowired
        lateinit var entityManager: EntityManager
    
        override fun setupTestData() {
            // Data setup
        }
    
        override fun getPrn(): String? = "A1234AA"
        
        override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

        override fun getWebTestClientInstance(): WebTestClient = webTestClient

        override fun getDataSourceInstance(): DataSource = dataSource

        override fun getEntityManagerInstance(): EntityManager = entityManager
    }

#### Example test application.yml with all test properties required
     
    hmpps:
      sar:
        tests:
          expected-api-response:
            path: /sar/my-service-api-response.json
          expected-render-result:
            path: /sar/my-service-api-expected.html
          attachments-expected: true
          expected-flyway-schema-version: 59
          expected-jpa-entity-schema:
            path: /sar/entity-schema-snapshot.json
 