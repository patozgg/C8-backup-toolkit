# Google storage

This backup guide is based on Camunda 8.6.x

# Pre requisites


Storage creation  [Prerequisite](GooglePrerequisite.md)




# Principal

The main idea when using Google to backup Camunda 8 is that Camunda 8  needs a Google service account (as seen in prerequisite). The service account is then authorized to read/write to the storage bucket.



# Connection

For Camunda 8 to use the service account a role must be assigned to it and  the key must be configured.
Please follow the next two guides.

Service account creation [Service Account](serviceaccountcreation.md)
Helm configuration and backup test [Helm and backup](keyandhelm.md)
