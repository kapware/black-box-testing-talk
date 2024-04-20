package com.kapware.blackbox.case1;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Testcontainers
class Case1IT {
    private static Logger logger = LoggerFactory.getLogger(Case1IT.class);
    Network network;
    GenericContainer appContainer;
    WireMockContainer meetupApiContainer;
    HttpClient httpClient;

    @BeforeEach
    void setup() {
        network = Network.newNetwork();
        meetupApiContainer = new WireMockContainer("wiremock/wiremock:2.35.0");
        meetupApiContainer
                .withNetwork(network)
                .withNetworkAliases("meetup-api")
                .start();
        meetupApiContainer.followOutput(new Slf4jLogConsumer(logger).withPrefix("meetup-api"));
        WireMock.configureFor(meetupApiContainer.getHost(), meetupApiContainer.getPort());
        WireMock.stubFor(
                WireMock.get("/events?only=id,name,time&status=upcoming,past&key=boguskey&group_urlname=agroup")
                        .willReturn(WireMock.okJson(
// language=json
"""
{
    "results": [
        {
            "id":123,
            "name": "abc"
        }
    ]
}
""")));

        var testedImage = System.getenv("TESTED_IMAGE");
        appContainer = new GenericContainer(testedImage);
        appContainer
                .dependsOn(meetupApiContainer)
                .withNetwork(network)
                .waitingFor(Wait.forHttp("/events/").forPort(8080))
                .withExposedPorts(8080)
                .withEnv("meetup.key", "boguskey")
                .withEnv("meetup.groupUrlName", "agroup")
                .withEnv("meetup.url", "http://meetup-api:8080/")
                .start();
        appContainer.followOutput(new Slf4jLogConsumer(logger).withPrefix("app"));

        httpClient = HttpClient.newHttpClient();
    }

    @Test
    void refreshesEventsFromMeetup() throws IOException, InterruptedException {
        // given:
        // when: sending refresh request
        httpClient.send(
                HttpRequest.newBuilder(
                                URI.create("http://" + appContainer.getHost() + ":"
                                        + appContainer.getMappedPort(8080)
                                        + "/events/refresh"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        // then:
        var eventsResponse = httpClient.send(
                HttpRequest.newBuilder(
                                URI.create("http://" + appContainer.getHost() + ":"
                                        + appContainer.getMappedPort(8080)
                                        + "/events/"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(eventsResponse.body(), hasJsonPath("$[0].id", equalTo(123)));
        assertThat(eventsResponse.body(), hasJsonPath("$[0].name", equalTo("abc")));
    }
}
