package io.camunda.blueberry.platform.rule;


import io.camunda.blueberry.config.BlueberryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Operate define a repository, and the repository exist in ElasticSearch
 */
@Component
public class RuleZeebeContainer implements Rule {


    @Autowired
    BlueberryConfig blueberryConfig;

    @Override
    public boolean validRule() {
        // is Operate is define in the cluster?
        return false;
    }

    @Override
    public String getName() {
        return "Zeebe Container";
    }

    public String getExplanations() {
        return "Zeebe must define a container to backup the data.";
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

        // get the Pod description
        RuleInfo ruleInfo = new RuleInfo(this);
        if (validRule()) {
            ruleInfo.setStatus( RuleStatus.FAILED);
            ruleInfo.addDetails("Not implemented yet");
        } else
            ruleInfo.setStatus( RuleStatus.DEACTIVATED);
        return ruleInfo;
    }
}
