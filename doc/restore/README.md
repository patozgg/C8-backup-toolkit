# Restore

# Stop all component

The idea is to stop all component, but keep the PCV

Run a

```shell
kubectl get pods

NAME                                    READY   STATUS    RESTARTS   AGE
camunda-elasticsearch-master-0          1/1     Running   0          3m19s
camunda-elasticsearch-master-1          1/1     Running   0          3m19s
camunda-elasticsearch-master-2          1/1     Running   0          3m19s
camunda-operate-76bb6d896c-2zm7b        1/1     Running   0          3m20s
camunda-optimize-7f4cd5f9b6-vf7ms       1/1     Running   0          3m20s
camunda-tasklist-59474455b4-2fzfl       1/1     Running   0          3m20s
camunda-zeebe-0                         1/1     Running   0          3m19s
camunda-zeebe-1                         1/1     Running   0          3m19s
camunda-zeebe-2                         1/1     Running   0          3m19s
camunda-zeebe-gateway-d7c97f64c-wkdfq   1/1     Running   0          3m20s

```

and check how many instance on each component

| Component    | Number of instances |
|--------------|--------------------:| 
| zeebe        |                   3 |
| zeebeGateway |                   1 |
| Operate      |                   1 |
| TaskList     |                   1 |


Check if all repositories exist in Elasticsearch. If you start from a complete empty database, this configuration must be executed.

```shell
curl -X GET "http://localhost:9200/_snapshot/_all?pretty"
```

# Scale down components

```shell
kubectl scale sts/camunda-zeebe --replicas=0
kubectl scale deploy/camunda-zeebe-gateway --replicas=0
kubectl scale deploy/camunda-operate --replicas=0
kubectl scale deploy/camunda-tasklist --replicas=0
kubectl scale deploy/camunda-optimize --replicas=0
```

only Elasticsearch is up and running

```shell
kubectl get pods

NAME                                    READY   STATUS    RESTARTS   AGE
camunda-elasticsearch-master-0          1/1     Running   0          3m19s
camunda-elasticsearch-master-1          1/1     Running   0          3m19s
camunda-elasticsearch-master-2          1/1     Running   0          3m19s
```


# Delete all index in Elastic search
When Operate, Tasklist and Optimize start, they created indexes. It must be purged

In the k8 folder, a es-delete-all-indices.yaml is present. This kubernetes file will create a pod, which execute this deletion.
The script is present under `doc/restore/k8`, so do a `cd` in that folder if needed.

```shell
kubectl apply -f k8/es-delete-all-indices.yaml
```
When the deletion is performed, the pod will stop and move to the state "Completed"

```shell
kubernetes get pods
es-delete-all-indices-job-bs2h9   0/1     Completed   0          18s
```
Remove the pod

```shell
kubectl delete -f es-delete-all-indices.yaml
```

Run the command to verify that all index are deleted
```shell
curl -X GET "http://localhost:9200/_cat/indices?v"
health status index uuid pri rep docs.count docs.deleted store.size pri.store.size dataset.size
```
The list must be empty

# Restore the backup

Put the backup ID to restore in a secret. We want to restore the backupID 12

```shell
kubectl create secret generic backup-timeid --from-literal=backupTimeId=12
```

Run the pod, the script is present in `doc/restore/k8` 

```shell
kubectl apply -f es-snapshot-restore.yaml
```

Monitor the execution. At the end, the pod status changed to Completed

```shell
kubernetes get pods
es-snapshot-restore-job-pdvzz   0/1     Completed   0          18s
```
Remove the pod

```shell
kubectl delete -f es-snapshot-restore.yaml
```
Check in Elasticsearch: indexes must be restored

```shell
curl -X GET "http://localhost:9200/_cat/indices?v"
health status index                                     uuid                   pri rep docs.count docs.deleted store.size pri.store.size dataset.size
green  open   operate-message-8.5.0_                    kGn9TkwRRqGlRaoW1anvaQ   1   0          0            0       249b           249b         249b
green  open   operate-batch-operation-1.0.0_            QimDEIwKSwmUYqD8Bc1KRw   1   0          0            0       249b           249b         249b
green  open   operate-web-session-1.1.0_                CWDoCuBiTsO9i5xQwuduxg   1   0          0            0       249b           249b         249b
green  open   operate-flownode-instance-8.3.1_          gVKhEQLZR0WiXGfvQ5kIOw   1   0         12            0     23.6kb         23.6kb       23.6kb
green  open   operate-decision-8.3.0_                   VfEt4Nj5RnKSUtT08V-TFg   1   0          0            0       249b           249b         249b
green  open   operate-user-task-8.5.0_                  U_nEvmNSQauEmp915agkOQ   1   0          0            0       249b           249b         249b
green  open   operate-event-8.3.0_                      yZGw2XLERUSrw0wUsmof3Q   1   0         12            5     33.6kb         33.6kb       33.6kb
green  open   operate-job-8.6.0_                        A_OE0XWVQBKVHF2DjJtqXw   1   0          6            0     22.9kb         22.9kb       22.9kb
green  open   operate-variable-8.3.0_                   4xWHq0ZuQpS4Vi4atJ53jQ   1   0         28            0       23kb           23kb         23kb
green  open   operate-post-importer-queue-8.3.0_        1xWpxnqeQ16_Bx7SxK4H7w   1   0          0            0       249b           249b         249b
green  open   operate-list-view-8.3.0_                  F96hokUKTbG6NOzpWVsYtg   1   0         46            5    124.2kb        124.2kb      124.2kb
green  open   operate-decision-instance-8.3.0_          y4dEs61MSGqcPT8RKbMiOQ   1   0          0            0       249b           249b         249b
green  open   operate-migration-steps-repository-1.1.0_ fpLS4XtpQvuF9K2CmEPQYA   1   0         27            1     29.5kb         29.5kb       29.5kb
green  open   operate-user-1.2.0_                       YCSNPijSS5OQVhHTjaNKRQ   1   0          3            0      6.7kb          6.7kb        6.7kb
green  open   operate-sequence-flow-8.3.0_              WGxc-bbrQ8G-troFT0jEwA   1   0          6            0     13.6kb         13.6kb       13.6kb
green  open   operate-metric-8.3.0_                     G2oNlA6MQoK2DiZRZbhsyQ   1   0          6            0     11.7kb         11.7kb       11.7kb
green  open   operate-operation-8.4.1_                  AXbJWKmSTey0yfzHl_AanA   1   0          0            0       249b           249b         249b
green  open   operate-import-position-8.3.0_            9FgKNWwcQ6CleKdCQcSIeg   1   0         12            7     17.5kb         17.5kb       17.5kb
green  open   operate-incident-8.3.1_                   omR5v_KXRkGWagXYBKYkdw   1   0          0            0       249b           249b         249b
green  open   operate-decision-requirements-8.3.0_      21FcaK25TJuzpHrqk5aoNg   1   0          0            0       249b           249b         249b
green  open   operate-process-8.3.0_                    8hFENmo0TOCbtHWUweN-AA   1   0          2            2    181.2kb        181.2kb      181.2kb
```


> Note: in the yaml file, the list of repository is give
> ```
> # Define the list of repository names
> repositories="operaterepository optimizebackup tasklistrepository zeeberecordrepository"
> ```
> This list may be adapted, according to the reposuitories created in Elasticsearch. The current values are the one created in the backup procedure.
>
      

# Restore Zeebe - the configuration

The last section involves restoring each Zeebe PVC. A separate job is required for each instance. 

Attention: the script must be adapted

* one file per node (depends on cluster size) and each file must have a uniq `ZEEBE_BROKER_CLUSTER_NODEID` value
* the version must be changed to be on the exact same version as the cluster
* the connection to the storage must be set correctly
* * each pod claim a PV according to the node


> **Attention** : this configuration is crucial to run the restoration without issue

The configuration has to be done one time at the beginning, and when the server is updated to keep the restoration process on the same version number.


For example, configure the pod with these variables 

```
    - name: ZEEBE_BROKER_DATA_BACKUP_STORE
      value: "AZURE"
    - name: ZEEBE_BROKER_DATA_BACKUP_AZURE_CONNECTIONSTRING
      value: Defa.....net
    - name: ZEEBE_BROKER_DATA_BACKUP_AZURE_BASEPATH
      value: zeebecontainer
```
The backupId is retrieved from the same secret as Elasticsearch

````
        - name: ZEEBE_RESTORE_FROM_BACKUP_ID
          valueFrom:
            secretKeyRef:
              name: backup-timeid
              key: backupTimeId
 ````

Each pod must have the correct configuration in terms of **partition count**, **cluster size** and **replication factor**. These parameters pilot the retrieval
```
        - name: ZEEBE_BROKER_CLUSTER_PARTITIONSCOUNT
          value: "3"
        - name: ZEEBE_BROKER_CLUSTER_CLUSTERSIZE
          value: "3"
        - name: ZEEBE_BROKER_CLUSTER_REPLICATIONFACTOR
          value: "3"

```

And finally, each yaml file has a unique configuration in term if NODEID, starting at 0 and finishing at <ClusterSize-1> 

```
        - name: ZEEBE_BROKER_CLUSTER_NODEID
          value: "0"
```

the PV must be claim according to the NODEID
```
      volumes:
      - name: data
        persistentVolumeClaim:
          claimName: data-camunda-zeebe-0
```

# Restore Zeebe - the execution

Run each pod one by one
```shell
kubectl apply -f restore/zeebe-restore-job-0.yaml
```
Check logs on each pod
```
2025-02-15 02:19:22.364 [] [] [] INFO 
      io.camunda.zeebe.restore.RestoreManager - Successfully restored partition 2 from backup 12. Backup description: BackupDescriptorImpl[snapshotId=Optional[9-1-12-10-2], checkpointPosition=62, numberOfPartitions=3, brokerVersion=8.6.9]
2025-02-15 02:19:22.364 [] [] [] INFO
      io.camunda.zeebe.restore.RestoreManager - Successfully restored partition 3 from backup 12. Backup description: BackupDescriptorImpl[snapshotId=Optional[23-1-46-68-2], checkpointPosition=69, numberOfPartitions=3, brokerVersion=8.6.9]
2025-02-15 02:19:22.364 [] [] [] INFO
      io.camunda.zeebe.restore.RestoreManager - Successfully restored partition 1 from backup 12. Backup description: BackupDescriptorImpl[snapshotId=Optional[11-1-50-17-0], checkpointPosition=134, numberOfPartitions=3, brokerVersion=8.6.9]
2025-02-15 02:19:22.373 [] [main] [] INFO
      io.camunda.zeebe.restore.RestoreApp - Successfully restored broker from backup 12
Restoration complete.
```

# Scale back the application

Use the value saved during the exploration to restart all components, with the correct value

```shell
kubectl scale sts/camunda-zeebe --replicas=3
kubectl rollout status sts/camunda-zeebe
kubectl scale deploy/camunda-zeebe-gateway --replicas=1
kubectl scale deploy/camunda-operate --replicas=1
kubectl scale deploy/camunda-tasklist --replicas=1
kubectl scale deploy/camunda-optimize --replicas=1
```

Check the system.
