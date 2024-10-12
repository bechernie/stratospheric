package com.myorg;

import dev.stratospheric.cdk.SpringBootApplicationStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;

import java.util.Objects;

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        final var accountId = (String) app.getNode().tryGetContext("accountId");
        Objects.requireNonNull(accountId, "context variable 'accountId' must not be null");

        final var region = (String) app.getNode().tryGetContext("region");
        Objects.requireNonNull(region, "context variable 'region' must not be null");

        new SpringBootApplicationStack(app, "SpringBootApplication", makeEnv(accountId, region), "docker.io/bechernie/todo-app-v1:latest");

        app.synth();
    }

    static Environment makeEnv(String accountId, String region) {
        return Environment.builder()
                .account(accountId)
                .region(region)
                .build();
    }

}

