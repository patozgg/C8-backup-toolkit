# backup


According to the documentation https://docs.camunda.io/docs/8.5/self-managed/operational-guides/backup-restore/backup-and-restore/, the serie of command to run are

The backup ID is 11

# Optimize

Port forward the service
````shell
kubectl port-forward svc/camunda-optimize 8092:8092 -n camunda
````
Start the backup

````shell
curl -X POST "http://localhost:8092/actuator/backups" \
-H 'Content-Type: application/json' \
-d '{ "backupId": 11 }'
````

Check the status
````shell
curl -X GET "http://localhost:8092/actuator/backups/11"
````

wait for an answer COMPLETE
```json
{"backupId":11,"failureReason":null,"state":"COMPLETED"}
```


# operate
````shell
kubectl port-forward svc/camunda-operate 9600:9600 -n camunda
````

Start the backup
````shell
curl -X POST 'http://localhost:9600/actuator/backups' \
-H 'Content-Type: application/json' \
-d '{ "backupId": 11 }'
````


Check the status
````shell
curl -X GET "http://localhost:9600/actuator/backups/11"
````

Wait for the status COMPLETED

```json
{"backupId":11,"state":"COMPLETED"}
```

# tasklist

````shell
kubectl port-forward svc/camunda-tasklist 9600:9600 -n camunda
````

Start the backup
````shell
curl -X POST 'http://localhost:9600/actuator/backups' \
-H 'Content-Type: application/json' \
-d '{ "backupId": 11 }'
````


Check the status
````shell
curl -X GET "http://localhost:9600/actuator/backups/11"
````

Wait for the status COMPLETED

```json
{"backupId":11,"state":"COMPLETED"}
```


# Zeebe

There are two operations in Zeebe: zeeberecord (on ElasticSearch) and Zeebe itself.

Port-forward the server
```shell
kubectl port-forward svc/camunda-zeebe-gateway 9600:9600 -n camunda
kubectl port-forward svc/camunda-elasticsearch 9200:9200 -n camunda
```

1. pause the exporting

```shell
curl -X POST "http://localhost:9600/actuator/exporting/pause"   -H 'Content-Type: application/json'    -d '{}'
```

2. Backup zeebe record

```shell
curl -X PUT http://localhost:9200/_snapshot/zeeberecordrepository/11 -H 'Content-Type: application/json'   \
-d '{ "indices": "zeebe-record*", "feature_states": ["none"]}'
```

3. backup Zeebe 
```shell
curl -X POST "http://localhost:9600/actuator/backups"  -H 'Content-Type: application/json'  -d "{\"backupId\": \"11\"}"
```

4. Monitor the backup
```shell
curl -s "http://localhost:9600/actuator/backups/11"
```
wait for the status
```json
{"backupId":11,"state":"COMPLETED"}
```

5. Resume Zeebe

```shell
curl -X POST "http://localhost:9600/actuator/exporting/resume"  -H 'Content-Type: application/json'    -d '{}'
```

