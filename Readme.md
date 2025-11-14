# fhir-flattener

Prototype of a server in front of [Pathling](https://pathling.csiro.au/)
for the [SQL-on-FHIR operation definitions](https://build.fhir.org/ig/FHIR/sql-on-fhir-v2/operations.html), until the Pathling server is ready.

## Build an executable .jar file
`./gradlew shadowJar`, result .jar is in `build/libs`



## Usage example
Just use docker compose to start the service: 
```http request
POST http://localhost:8000/fhir/ViewDefinition/$run
Content-Type: application/fhir+json # must be json currently 
Accept: text/csv # application/x-ndjson, application/json, application/octet-stream
```
``` json
{
  "resourceType": "Parameters",
  "parameter": [
    {
      "name": "viewDefinition",
      "resource": {
        "resourceType": "https://sql-on-fhir.org/ig/StructureDefinition/ViewDefinition",
        "select": [
          { "column": [
              { "path": "getResourceKey()", "name": "id" },
              { "path": "gender", "name": "gender" }
          ] }
        ],
        "name": "patient_demographics",
        "status": "draft",
        "resource": "Patient"
      }
    },
    {
      "name": "resources",
      "resource": {
        "resourceType": "Patient",
        [...]        
      }
    },
    // just repeat parameter for additional resources
    { "name": "resources", "resource": { "resourceType": "Patient", [...] } }
  ]
}
```