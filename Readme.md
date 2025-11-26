# fhir-flattener

Prototype of a server in front of [Pathling](https://pathling.csiro.au/)
for the [SQL-on-FHIR operation definitions](https://build.fhir.org/ig/FHIR/sql-on-fhir-v2/operations.html), until the Pathling server is ready.

## What is flattening?
FHIR uses a hierarchical tree structure for resources with a mix of complex objects and arrays. 

E.g.
```json
{
  "resourceType": "Patient",
  "id": "1",
  "gender": "male",
  "birthDate": "1959",
  "deceasedBoolean": false,
  "name": [{
      "use": "official",
      "given": ["John"],
      "family": "Jackson"
  }]
}
```
or
```json
{
  "resourceType": "Patient",
  "id": "2",
  "gender": "male",
  "birthDate": "1959",
  "deceasedBoolean": true,
  "name": [{
      "use": "official",
      "given": ["Jack"],
      "family": "Johnson"
  }]
}
```
This makes it difficult to work with the data for statistical analysis.
Here, we would like to have a table like this with the data which an be loaded into SPSS or R dataframes:

|id|gender|given_name|family_name|
|-|-|-|-|
|1|male|John|Jackson|
|2|male|Jack|Johnson|

Using the SQL-on-FHIRv2 operation `ViewDefinition/$run`, we can define a set of FHIRPath-expressions
on the data to get the desired table back:
```json
{
  "resourceType": "https://sql-on-fhir.org/ig/StructureDefinition/ViewDefinition",
  "name": "patient_demographics",
  "status": "draft",
  "resource": "Patient",
  "select": [
    {
      "column": [
        {"name": "id", "path": "getResourceKey()"},
        { "name": "gender", "path": "gender"}
      ]
    },
    {
      "forEach": "name.where(use = 'official').first()",
      "column": [
        {"name": "given_name", "path": "given.join(' ')"},
        {"name": "family_name", "path": "family"}
      ]
    }
  ]
}

```

## Build an executable .jar file
`./gradlew shadowJar`, result .jar is in `build/libs`. Run with `java -DPORT=8000 -jar fhir-flattener-all.jar`.

## Usage example
Use `docker compose` or the .jar file to start the service and make an HTTP request like this: 
```http request
POST http://localhost:8000/fhir/ViewDefinition/$run
Content-Type: application/fhir+json  # must be json, xml is not supported 
Accept: text/csv  # application/x-ndjson, application/json, or application/octet-stream for parquet

{
  "resourceType": "Parameters",
  "parameter": [
    {
      "name": "viewDefinition",
      "resource": {
        "resourceType": "https://sql-on-fhir.org/ig/StructureDefinition/ViewDefinition",
        # put the rest of the view definition here
      }
    },
    # just repeat parameter 'resources' as often as you want to provide the data
    { "name": "resources", "resource": { "resourceType": "Patient", "id": "1", "gender": "female"} },
    { "name": "resources", "resource": { "resourceType": "Patient", "id": "2", "gender": "male" } }
  ]
}
```

If you want to try it directly using curl:
```bash
curl -X POST --location "http://localhost:8000/fhir/ViewDefinition/$run" \
    -H "Content-Type: application/fhir+json" \
    -H "Accept: text/csv" \
    -d '{
          "resourceType": "Parameters",
          "parameter": [
            {
              "name": "viewDefinition",
              "resource": {
                "resourceType": "https://sql-on-fhir.org/ig/StructureDefinition/ViewDefinition",
                "select": [
                  {
                    "column": [
                      {
                        "path": "getResourceKey()",
                        "name": "id"
                      },
                      {
                        "path": "gender",
                        "name": "gender"
                      }
                    ]
                  },
                  {
                    "column": [
                      {
                        "path": "given.join('\'' '\'')",
                        "name": "given_name",
                      },
                      {
                        "path": "family",
                        "name": "family_name"
                      }
                    ],
                    "forEach": "name.where(use = '\''official'\'').first()"
                  }
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
                "id": "IMI-P0000000041",
                "meta": {
                  "profile": [
                    "https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/Patient|2024.0.0"
                  ]
                },
                "gender": "male",
                "birthDate": "1959",
                "deceasedBoolean": false,
                "name": [
                  {
                    "use": "official",
                    "given": [ "John", "James" ],
                    "family": "Bond"
                  }
                ]
              }
            }
          ]
        }'
```
