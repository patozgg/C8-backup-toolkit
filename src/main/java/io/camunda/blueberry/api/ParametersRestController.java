package io.camunda.blueberry.api;

import io.camunda.blueberry.config.BlueberryConfig;
import io.camunda.blueberry.platform.PlatformManager;
import io.camunda.blueberry.platform.rule.Rule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("blueberry")

public class ParametersRestController {
    Logger logger = LoggerFactory.getLogger(ParametersRestController.class);

    @Autowired
    private BlueberryConfig blueberryConfig;

    /**
     * Check the system
     * Does Zeebe declare a container? A Type storage?
     * Does Elasticsearch as repository for each component like OperateConnect?
     *
     * @return
     */
    @GetMapping(value = "/api/parameters/getall", produces = "application/json")
    public Map<@NotNull String, Object> getAll() {
        try {
            logger.debug("Rest [/api/parameters/getall]");
            return blueberryConfig.getAll();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
