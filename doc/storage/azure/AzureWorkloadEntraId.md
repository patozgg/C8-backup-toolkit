# Connection with Workload Identity

This operation is valid only if the Camunda cluster is deploying with AKS

# Enable

```shell
$ az aks update --resource-group <GroupName> --name <ClusterName> --enable-oidc-issuer --enable-workload-identity
```

Check with
```shell
$ az aks show   --resource-group pierreyvesTest-rg   --name pierreyves-may-25   --query "{OIDC: oidcIssuerProfile.enabled, WorkloadIdentity: workloadIdentityEnabled}"
{
  "OIDC": true,
  "WorkloadIdentity": true
}
```

> Note: a development cluster does not allow to enabled Workload identity
>

```` 
az aks create \
--resource-group pierreyvesTest-rg \
--name pierreyves-may-25 \
--enable-oidc-issuer \
--enable-workload-identity \
--enable-managed-identity \
--node-count 1 \
--generate-ssh-keys \
--location eastus
````


az aks update \
--resource-group pierreyvesTest-rg \
--name pierreyvesTest \
--enable-workload-identity


