package io.camunda.blueberry.connect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class ZeebeBackupStatusResponse {
    List<InnterZeebeBackupStatusResponse> snapshots;

    public List<InnterZeebeBackupStatusResponse> getSnapshots() {
        return snapshots;
    }

    public static class InnterZeebeBackupStatusResponse {
        private String snapshot;
        private String repository;
        private String state;

        public String getSnapshot() {
            return snapshot;
        }

        public String getRepository() {
            return repository;
        }

        public String getState() {
            return state;
        }
    }
}
