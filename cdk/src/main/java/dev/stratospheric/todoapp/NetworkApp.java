package dev.stratospheric.todoapp;

import dev.stratospheric.cdk.Network;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.Objects;

public class NetworkApp {
    public static void main(String[] args) {
        final var app = new App();

        final var environmentName = (String) app.getNode().tryGetContext("environmentName");
        Objects.requireNonNull(environmentName);

        final var accountId = (String) app.getNode().tryGetContext("accountId");
        Objects.requireNonNull(accountId);

        final var region = (String) app.getNode().tryGetContext("region");
        Objects.requireNonNull(region);

        final var awsEnvironment = Environment.builder()
                .account(accountId)
                .region(region)
                .build();

        final var networkStack = new Stack(app, "NetworkStack", StackProps.builder()
                .stackName(environmentName + "-Network")
                .env(awsEnvironment)
                .build());

        new Network(
                networkStack,
                "Network",
                awsEnvironment,
                environmentName,
                new Network.NetworkInputParameters()
        );

        app.synth();
    }
}
