package io.camunda.blueberry.platform.rule;


import io.camunda.blueberry.config.BlueberryConfig;
import io.camunda.blueberry.connect.CamundaApplicationInt;
import io.camunda.blueberry.connect.ElasticSearchConnect;
import io.camunda.blueberry.connect.KubernetesConnect;
import io.camunda.blueberry.connect.OperationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Operate define a repository, and the repository exist in ElasticSearch
 */
@Component
public class RuleOptimizeRepository implements Rule {


    @Autowired
    BlueberryConfig blueberryConfig;

    @Autowired
    KubernetesConnect kubernetesConnect;

    @Autowired
    ElasticSearchConnect elasticSearchConnect;

    @Override
    public boolean validRule() {
        // is Operate is define in the cluster?
        return blueberryConfig.getOptimizeActuatorUrl() != null;
    }

    @Override
    public String getName() {
        return "Optimize Repository";
    }

    public String getExplanations() {
        return "Optimize must define a repository name. Elastsearch must define this repository, and map it to a valid container.";
    }

    @Override
    public List<String> getUrlDocumentation() {
        return List.of();
    }


    @Override
    public RuleInfo check() {
        return operation(false);
    }

    @Override
    public RuleInfo configure() {
        return operation(true);
    }

    private RuleInfo operation(boolean execute) {

        boolean accessPodRepository = false;
        boolean accessElasticsearchRepository = false;
        boolean createElasticsearchRepository = false;


        // get the Pod description
        RuleInfo ruleInfo = new RuleInfo(this);

        if (!validRule()) {
            return ruleInfo;
        }            // ---------- First step, ask Operate for the name of the repository
        ruleInfo.setStatus(RuleStatus.INPROGRESS);
        String optimizeRepository = getRepositoryByConfiguration(ruleInfo);
        if (optimizeRepository == null) {
            ruleInfo.setStatus(RuleStatus.FAILED);
        }

        ruleInfo.addVerificationsAssertBoolean("Access pod repository, retrieve [" + optimizeRepository + "]", optimizeRepository != null,"From Configuration");


                //------------ Second step, verify if the repository exists in elasticSearch
        if (ruleInfo.inProgress()) {
            OperationResult operationResult = elasticSearchConnect.existRepository(optimizeRepository);
            accessElasticsearchRepository = operationResult.resultBoolean;
            ruleInfo.addVerificationsButWillBeFixed("Check Elasticsearch repository [" + optimizeRepository + "] :"
                            + operationResult.details,
                    accessElasticsearchRepository ? RuleStatus.CORRECT : RuleStatus.FAILED,
                    operationResult.command);

                // if the repository exist, then we stop the rule execution here
            if (accessElasticsearchRepository) {
                ruleInfo.addDetails("Repository exist in Elastic search");
            } else {
                // if we don't execute the rule, we stop here on a failure
                if (!execute) {
                    ruleInfo.addError("Repository does not exist in Elastic search, and must be created");
                    ruleInfo.setStatus(RuleStatus.FAILED);
                }
            }
        }


        // Third step, create the repository if asked
        if (execute && ruleInfo.inProgress()) {

            OperationResult operationResult = elasticSearchConnect.createRepository(optimizeRepository,
                    blueberryConfig.getContainerType(),
                    blueberryConfig.getOptimizeContainerBasePath());
            if (operationResult.success) {
                ruleInfo.addDetails("Repository is created in ElasticSearch");
            } else {
                ruleInfo.addError("Error when creating the repository in ElasticSearch :" + operationResult.details);
                ruleInfo.setStatus(RuleStatus.FAILED);
            }
            ruleInfo.addVerifications("Check Elasticsearch repository [" + optimizeRepository

                            + "] basePath[" + blueberryConfig.getOperateContainerBasePath()
                            + "] " + operationResult.details,
                    operationResult.success? RuleStatus.CORRECT: RuleStatus.FAILED,
                    operationResult.command);

        }

        // Still in progress at this point? All is OK then
        if (ruleInfo.inProgress()) {
            ruleInfo.setStatus(RuleStatus.CORRECT);
        }
        return ruleInfo;
    }

    private String getRepositoryByConfiguration(RuleInfo ruleInfo) {
        ruleInfo.addDetails("Access RepositoryName from Blueberry configuration");
        return blueberryConfig.getOptimizeRepository();
    }

    private String getRepositoryByOperateEnvironment(RuleInfo ruleInfo) {
        ruleInfo.addDetails("Access RepositoryName exploring Operate environment");
        return null;
    }

    private String getRepositoryKubernetes(RuleInfo ruleInfo) {
        OperationResult operationResult = kubernetesConnect.getRepositoryName(CamundaApplicationInt.COMPONENT.OPTIMIZE, blueberryConfig.getNamespace());
        if (!operationResult.success) {
            ruleInfo.addDetails("Can't access the Repository name in the pod, or does not exist");
            ruleInfo.addDetails(operationResult.details);
            ruleInfo.setStatus(RuleStatus.FAILED);
        } else {
            ruleInfo.addDetails("Access RepositoryName exploring Kubernetes environment");
            return operationResult.resultSt;
        }
        return null;
    }

}
