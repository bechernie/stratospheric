package dev.stratospheric.todoapp;

import dev.stratospheric.cdk.ApplicationEnvironment;
import dev.stratospheric.cdk.Network;
import dev.stratospheric.cdk.Service;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ServiceApp {
    public static void main(String[] args) {
        final var app = new App();

        final var environmentName = (String) app.getNode().tryGetContext("environmentName");
        Objects.requireNonNull(environmentName);

        final var applicationName = (String) app.getNode().tryGetContext("applicationName");
        Objects.requireNonNull(applicationName);

        final var accountId = (String) app.getNode().tryGetContext("accountId");
        Objects.requireNonNull(accountId);

        final var springProfile = (String) app.getNode().tryGetContext("springProfile");
        Objects.requireNonNull(springProfile);

        final var dockerRepositoryName = (String) app.getNode().tryGetContext("dockerRepositoryName");
        Objects.requireNonNull(dockerRepositoryName);

        final var dockerImageTag = (String) app.getNode().tryGetContext("dockerImageTag");
        Objects.requireNonNull(dockerImageTag);

        final var region = (String) app.getNode().tryGetContext("region");
        Objects.requireNonNull(region);

        final var awsEnvironment = Environment.builder()
                .account(accountId)
                .region(region)
                .build();

        final var applicationEnvironment = new ApplicationEnvironment(applicationName, environmentName);

        final var serviceStack = new Stack(
                app,
                "ServiceStack",
                StackProps.builder()
                        .stackName(applicationEnvironment.prefix("Service"))
                        .env(awsEnvironment)
                        .build()
        );

        final var dockerImageSource = new Service.DockerImageSource(dockerRepositoryName, dockerImageTag);

        final var networkOutputParameters = Network.getOutputParametersFromParameterStore(serviceStack, applicationEnvironment.getEnvironmentName());

        final var serviceInputParameters = new Service.ServiceInputParameters(dockerImageSource, environmentVariables(springProfile))
                .withHealthCheckIntervalSeconds(30);

        new Service(
                serviceStack,
                "Service",
                awsEnvironment,
                applicationEnvironment,
                serviceInputParameters,
                networkOutputParameters
        );

        app.synth();
    }

    static Map<String, String> environmentVariables(String springProfile) {
        final var vars = new HashMap<String, String>();
        vars.put("SPRING_PROFILES_ACTIVE", springProfile);
        return vars;
    }
}
