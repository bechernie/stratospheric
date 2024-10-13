package dev.stratospheric.todoapp;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.Objects;

public class BootstrapApp {

    public static void main(final String[] args) {
        final var app = new App();

        final var region = (String) app.getNode().tryGetContext("region");
        Objects.requireNonNull(region);

        final var accountId = (String) app.getNode().tryGetContext("accountId");
        Objects.requireNonNull(accountId);

        final var awsEnvironment = Environment.builder()
                .account(accountId)
                .region(region)
                .build();

        new Stack(
                app,
                "Bootstrap",
                StackProps.builder()
                        .env(awsEnvironment)
                        .build()
        );

        app.synth();
    }

}
