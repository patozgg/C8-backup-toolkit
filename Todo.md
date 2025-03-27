# Todo

# Version 1.0 (first version published)

Publish the first version. This version must run a backup, on S3 and on Azure.

Objective: March 28 

| Title                                       | Who    | Status      |
|---------------------------------------------|--------|-------------|
| Structure the code                          | Joey   |             |
| Create a docker image and a Kubernetes file | Py     | In progress |
| Test on S3 bucket                           | Py     |             |
| Test on Azure container                     | Py     | Done        |
| Documentation                               | Py     |             |  


# Version 2.0 Restore
This version contains:

## Dashboard
The dashboard, with these information
  * Zeebe status (pause/active)
  * Number of backup
  * Backup in progress?
  * Configuration Ok?
  * graph to show the list backup

## Kubernetes access

The access to Kubernetes to get information. 
* On the backup, repository name are retrieved from the pod and not from the application.yaml
* To execute the restore, operation like "scale down", "start a pod" must be accessible.
THe restoration calculate the number of pod to run the restoration (one pode per cluster<size>) and it need to know the number of partition and the number of replication factor.

## Restore
The restoration is available. The UI is available (list the backup) and the restore process is available.
 

## Check the backup
A backup may be partial: for example, a backup id 10 ran on Elastic Search, but not on Zeebe. 
The list of backup must detect that, and display a status (complete/incomplete). This is mandatory before starting a restore.

Note: Optimize backup may not be present, and it's acceptable if Optimize is not started in the cluster.


## Configuration and exploration
on the UI, on the tab "parameters", a section "exploration" should be visible.
This section display
- the name of repository for operate, TaskList,Optimize
- the list of component in the cluster (Operate? TaskList? Optimize?)
- Zeebe information: clusterSize, partitions, replicationfactor.

## Scheduler
The scheduler is active, and is able to start the backup at the correct frequency

## Other container
Minio, Google is integrated.


## Delete old backups
A backup can be deleted. 
A parameter can exist to keep only N backup: when a N+1 is done, then the oldest is deleted.

| Title                  | Who  | Status |
|------------------------|------|--------|
| Dashboard              | Py   |        | 
| Kubernetes access      | Ariv |        |
| Restoration            | Joey |        |
| Check Backup           | ?    |        | 
| Exploration            | Py   |        |
| Scheduler              | ?    |        |
| Minio container        | ?    |        |
| Google Drive container | ?    |        | 
| Delete a backup        | ?    |        | 
| Keep only N backups    | ?    |        |

    