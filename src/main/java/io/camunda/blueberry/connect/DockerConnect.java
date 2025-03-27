package io.camunda.blueberry.connect;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;

import java.util.List;


public class DockerConnect {

    public void getPodInformation(String podName, String namespace) {

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerClient dockerClient = DockerClientImpl.getInstance(config)
                .withDockerCmdExecFactory(new NettyDockerCmdExecFactory());

        // List Docker images
        List<Image> images = dockerClient.listImagesCmd().exec();

        for (Image image : images) {
            System.out.println("Image ID: " + image.getId());
            System.out.println("Tags: " + String.join(", ", image.getRepoTags()));
            System.out.println("Size: " + image.getSize() + " bytes");
            System.out.println("--------------");
        }
    }

}
