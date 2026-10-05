package com.themainthread.contributioncity.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.themainthread.contributioncity.Fixtures;

import io.smallrye.graphql.client.GraphQLError;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

class GitHubContributionClientTest {
    private final GitHubContributionClient contributionClient = new GitHubContributionClient(new GitHubContributionParser());

    @Test
    void makesExactlyOneQueryWithVariables() throws Exception {
        DynamicGraphQLClient client = mock(DynamicGraphQLClient.class);
        Response response = mock(Response.class);
        when(response.getData()).thenReturn(Fixtures.response("contribution-calendar.json").getJsonObject("data"));
        when(client.executeSync(anyString(), eq(Map.of("login", "fixture-user")))).thenReturn(response);
        assertEquals(44, contributionClient.fetch(client, "fixture-user").totalContributions());
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(client).executeSync(query.capture(), eq(Map.of("login", "fixture-user")));
        assertTrue(query.getValue().contains("user(login: $login)"));
        assertFalse(query.getValue().contains("fixture-user"));
        verifyNoMoreInteractions(client);
    }

    @Test
    void failsOnGraphqlErrorsEvenWithData() throws Exception {
        DynamicGraphQLClient client = mock(DynamicGraphQLClient.class);
        Response response = mock(Response.class);
        GraphQLError error = mock(GraphQLError.class);
        when(error.getMessage()).thenReturn(
                Fixtures.response("graphql-error.json").getJsonArray("errors").getJsonObject(0).getString("message"));
        when(response.getErrors()).thenReturn(List.of(error));
        when(client.executeSync(anyString(), eq(Map.of("login", "missing-user")))).thenReturn(response);
        String message = assertThrows(IllegalStateException.class, () -> contributionClient.fetch(client, "missing-user"))
                .getMessage();
        assertTrue(message.contains("GitHub returned GraphQL errors"));
        assertTrue(message.contains("missing-user"));
    }

    @Test
    void transportFailureDoesNotExposeHeaders() throws Exception {
        DynamicGraphQLClient client = mock(DynamicGraphQLClient.class);
        when(client.executeSync(anyString(), eq(Map.of("login", "fixture-user"))))
                .thenThrow(new ExecutionException(new RuntimeException("Authorization: secret-test-token")));
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> contributionClient.fetch(client, "fixture-user"));
        assertFalse(failure.toString().contains("secret-test-token"));
        assertEquals(null, failure.getCause());
    }

    @Test
    void preservesInterruptStatus() throws Exception {
        DynamicGraphQLClient client = mock(DynamicGraphQLClient.class);
        when(client.executeSync(anyString(), eq(Map.of("login", "fixture-user")))).thenThrow(new InterruptedException());
        try {
            assertThrows(IllegalStateException.class, () -> contributionClient.fetch(client, "fixture-user"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
