import {GetQueueAttributesCommand, SQSClient,} from "@aws-sdk/client-sqs";
import axios from "axios";

export const handler = async (e) => {
    const queueUrl = process.env.QUEUE_URL;
    const region = process.env.REGION;
    const githubToken = process.env.GITHUB_TOKEN;
    const event = new SqsEventWrapper(e);
    const latestDeploymentEvent = event.getLatestDeploymentEvent();
    const github = new GitHub(githubToken);
    const queue = new DeploymentQueue(queueUrl, region);

    console.log(`Received event: ${JSON.stringify(latestDeploymentEvent)}`);

    // If there are more events in the queue, we just finish processing and wait for the next event.
    if (await queue.hasWaitingEvents()) {
        console.log(
            "Skipping this event because there are more events waiting in the queue!"
        );
        return;
    }

    // If the GitHub workflow is currently running, we throw an error to retry this event at a later time.
    if (await github.isWorkflowCurrentlyRunning(latestDeploymentEvent)) {
        console.log(
            "GitHub workflow is currently running - retrying at a later time!"
        );
        throw "retrying later!";
    }

    // Triggering the GitHub workflow.
    await github.triggerWorkflow(latestDeploymentEvent);
}

class GitHub {
    constructor(githubToken) {
        this.githubToken = githubToken;
    }

    /**
     * Calls the GitHub API to check if the workflow defined by the given DeploymentEvent is currently running.
     */
    async isWorkflowCurrentlyRunning(event) {
        console.log(
            `checking if GitHub workflow is running for event: ${JSON.stringify(
                event
            )}`
        );

        const response = await axios.get(
            `https://api.github.com/repos/${event.owner}/${event.repo}/actions/workflows/${event.workflowId}/runs`,
            this.#axiosConfig()
        );

        // checking if the response contains at least one current workflow run that is not 'completed'
        // (see https://docs.github.com/en/rest/reference/actions#list-workflow-runs)
        const inProgressWorkflowRuns = response.data.workflow_runs.filter(
            (run) => {
                return run.status !== "completed";
            }
        );

        return Promise.resolve(inProgressWorkflowRuns.length > 0);
    }

    /**
     * Calls the GitHub API to trigger a GitHub Actions workflow for the given DeploymentEvent.
     */
    async triggerWorkflow(event) {
        console.log(
            `triggering GitHub workflow for event: ${JSON.stringify(event)}`
        );

        const requestData = {
            ref: event.ref,
            inputs: {
                "docker-image-tag": event.dockerImageTag,
            },
        };

        const response = await axios.post(
            `https://api.github.com/repos/${event.owner}/${event.repo}/actions/workflows/${event.workflowId}/dispatches`,
            requestData,
            this.#axiosConfig()
        );
    }

    #axiosConfig() {
        return {
            headers: {
                Authorization: `token ${this.githubToken}`,
            },
        };
    }
}

class DeploymentQueue {
    constructor(queueUrl, region) {
        this.queueUrl = queueUrl;
        this.sqsClient = new SQSClient({region: region});
    }

    async hasWaitingEvents() {

        console.log(
            `checking queue ${this.queueUrl} for waiting events`
        );

        const params = {
            QueueUrl: this.queueUrl,
            AttributeNames: [
                "ApproximateNumberOfMessages",
                "ApproximateNumberOfMessagesDelayed",
                "ApproximateNumberOfMessagesNotVisible",
            ],
        };
        const command = new GetQueueAttributesCommand(params);
        const data = await this.sqsClient.send(command);

        if (data.Attributes === undefined) {
            throw "GetQueueAttributesResult has no attributes!";
        }

        console.log(`GetQueueAttributesResult: ${JSON.stringify(data)}`);

        const waitingMessages =
            parseInt(data.Attributes["ApproximateNumberOfMessages"]) +
            parseInt(data.Attributes["ApproximateNumberOfMessagesDelayed"]) +
            parseInt(data.Attributes["ApproximateNumberOfMessagesNotVisible"]) -
            1; // minus one because the message currently processed by this Lambda is counted as a "not visible" message

        return Promise.resolve(waitingMessages > 0);
    }
}

class SqsEventWrapper {
    constructor(event) {
        this.event = event;
    }

    isEmpty() {
        return this.numberOfDeploymentEvents() == 0;
    }

    /**
     * Returns the number of deployment events in this batch.
     */
    numberOfDeploymentEvents() {
        return this.event.Records.length;
    }

    /**
     * Returns the latest event in this batch of events.
     */
    getLatestDeploymentEvent() {
        return JSON.parse(
            this.event.Records.sort((e1, e2) => {
                return (
                    Number(e1.attributes.SequenceNumber) -
                    Number(e2.attributes.SequenceNumber)
                );
            })
                .reverse()
                .shift()
                .body
        );
    }
}
