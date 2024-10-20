package dev.stratospheric.todoapp;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class DeploymentSequencerApp {

    public static void main(final String[] args) {
        final var app = new App();

        final var applicationName = (String) app.getNode().tryGetContext("applicationName");
        Objects.requireNonNull(applicationName);

        final var region = (String) app.getNode().tryGetContext("region");
        Objects.requireNonNull(region);

        final var accountId = (String) app.getNode().tryGetContext("accountId");
        Objects.requireNonNull(accountId);

        final var githubToken = (String) app.getNode().tryGetContext("githubToken");
        Objects.requireNonNull(applicationName);

        final var awsEnvironment = Environment.builder()
                .account(accountId)
                .region(region)
                .build();

        new DeploymentSequencerStack(
                app,
                "Bootstrap",
                awsEnvironment,
                applicationName,
                githubToken
        );

        app.synth();
    }

    static class DeploymentSequencerStack extends Stack {

        public DeploymentSequencerStack(
                Construct scope,
                String id,
                Environment awsEnvironment,
                String applicationName,
                String githubToken
        ) {
            super(
                    scope,
                    id,
                    StackProps.builder()
                            .stackName(applicationName + "-Deployments")
                            .env(awsEnvironment)
                            .build()
            );

            Objects.requireNonNull(awsEnvironment.getRegion());

            final var deploymentQueue = Queue.Builder.create(this, "deploymentsQueue")
                    .queueName(applicationName + "-deploymentsQueue.fifo")
                    .fifo(true)
                    .build();

            final var eventSource = SqsEventSource.Builder.create(deploymentQueue).build();

            LambdaFunction.Builder.create(
                            new Function(
                                    this,
                                    "deploymentSequencerFunction",
                                    FunctionProps.builder()
                                            .code(Code.fromAsset("./deployment-sequencer-lambda/dist/lambda.zip"))
                                            .runtime(Runtime.NODEJS_20_X)
                                            .handler("index.handler")
                                            .reservedConcurrentExecutions(1)
                                            .events(Collections.singletonList(eventSource))
                                            .environment(Map.of(
                                                    "GITHUB_TOKEN", githubToken,
                                                    "QUEUE_URL", deploymentQueue.getQueueUrl(),
                                                    "REGION", awsEnvironment.getRegion()
                                            ))
                                            .build()
                            )
                    )
                    .build();
        }
    }
}
