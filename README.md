# Cars connector - Metaphactory demo

Prototype code

## Build and run docker image

```shell
docker build -t smartrics/iotics-metaphactory-demo-connector:<tag> .
```

To run, make sure you create a directory locally where logs can be stored.
For example, `/path/to/host/logs`.

```shell
docker run -it -p 8080:8080 -p 443:8443 --env-file .env -v ${PWD}/src/main/resources:/app/resources -v ${PWD}/target/logs:/app/logs  smartrics/iotics-metaphactory-demo-connector:0.1
```

## Test

```shell
curl -G --data-urlencode "query=SELECT * WHERE { ?s ?p ?o }" -H "Accept: application/sparql-results+json" http://localhost:8080/sparql
```