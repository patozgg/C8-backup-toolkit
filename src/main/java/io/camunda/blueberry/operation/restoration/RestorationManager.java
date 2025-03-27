package io.camunda.blueberry.operation.restoration;

import io.camunda.blueberry.connect.ZeebeConnect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The restoration manager are in chage of one restoration
 */
@Component
public class RestorationManager {


    public RestorationJob currentRestorationJob;
    @Autowired
    ZeebeConnect zeebeConnect;

    public void startRestoration(Long backupId) {
        // Collect the parameters : number of cluster, partitions, replica factor

        ZeebeConnect.ZeebeInformation zeebeInformation = zeebeConnect.getInformation();
        // Do that in asynchronous : start the new thread to run it

        // start a RestorationJob, and keep it here
    }


}
