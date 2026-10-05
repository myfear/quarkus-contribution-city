package com.themainthread.contributioncity.github;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.themainthread.contributioncity.model.ContributionCalendar;

import io.smallrye.graphql.client.GraphQLError;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GitHubContributionClient {
    private static final String QUERY = """
            query($login: String!) {
              user(login: $login) {
                contributionsCollection {
                  contributionCalendar {
                    totalContributions
                    weeks {
                      firstDay
                      contributionDays {
                        date
                        contributionCount
                        contributionLevel
                      }
                    }
                  }
                }
              }
            }
            """;

    private final GitHubContributionParser parser;

    public GitHubContributionClient(GitHubContributionParser parser) {
        this.parser = parser;
    }

    public ContributionCalendar fetch(String username, String token, String endpoint) {
        // The extension's injected client performs a rateLimit query before invoking the action.
        // Building the client here keeps validation first and execution to one calendar request.
        try (DynamicGraphQLClient client = DynamicGraphQLClientBuilder.newBuilder()
                .url(endpoint).header("Authorization", "Bearer " + token).build()) {
            return fetch(client, username);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize or close the GitHub GraphQL client");
        }
    }

    public ContributionCalendar fetch(DynamicGraphQLClient client, String username) {
        Response response;
        try {
            response = client.executeSync(QUERY, Map.of("login", username));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub GraphQL request was interrupted");
        } catch (ExecutionException e) {
            // Transport exceptions may contain headers; keep them out of Action logs.
            throw new IllegalStateException("GitHub GraphQL request failed; check token permissions and connectivity");
        }
        if (response == null) {
            throw new IllegalStateException("GitHub returned no GraphQL response");
        }
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new IllegalStateException("GitHub returned GraphQL errors: " + response.getErrors().stream()
                    .map(GraphQLError::getMessage).collect(Collectors.joining("; ")));
        }
        return parser.parse(username, response.getData());
    }
}
