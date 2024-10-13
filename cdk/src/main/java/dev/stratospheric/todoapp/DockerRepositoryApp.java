package dev.stratospheric.todoapp;

import dev.stratospheric.cdk.DockerRepository;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.Objects;

public class DockerRepositoryApp {

    public static void main(String[] args) {
        final var app = new App();

        final var accountId = (String) app.getNode().tryGetContext("accountId");
        Objects.requireNonNull(accountId);

        final var region = (String) app.getNode().tryGetContext("region");
        Objects.requireNonNull(region);

        final var applicationName = (String) app.getNode().tryGetContext("applicationName");
        Objects.requireNonNull(applicationName);

        final var awsEnvironment = makeEnv(accountId, region);

        final var dockerRepositoryStack = new Stack(app, "DockerRepositoryStack", StackProps.builder()
                .stackName(applicationName + "-DockerRepository")
                .env(awsEnvironment)
                .build());

        new DockerRepository(
                dockerRepositoryStack,
                "DockerRepository",
                awsEnvironment,
                new DockerRepository.DockerRepositoryInputParameters(applicationName, accountId)
        );

        app.synth();
    }

    static Environment makeEnv(String accountId, String region) {
        return Environment.builder()
                .account(accountId)
                .region(region)
                .build();
    }
}
