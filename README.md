clear;curl -X POST -H "Content-Type: application/json" --data @src/main/resources/workflows/01-basic.json http://localhost:8080/workflows/workflows

clear;curl -X POST -H "Content-Type: application/json" --data @src/main/resources/workflows/01-basic-instance.json http://localhost:8080/workflows/instances/6
