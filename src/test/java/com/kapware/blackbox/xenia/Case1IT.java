package com.kapware.blackbox.xenia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class Case1IT {
    private static final Logger logger =
            LoggerFactory.getLogger(Case1IT.class);
    GenericContainer appContainer;

    @BeforeEach
    void setup() {
        var testedImage = System.getenv("TESTED_IMAGE");
        appContainer = new GenericContainer(testedImage);
        appContainer
                .waitingFor(Wait.forHttp("/events/").forPort(8080))
                .withExposedPorts(8080)
                .start();
        appContainer.followOutput(
                new Slf4jLogConsumer(logger).withPrefix("app"));
    }

    @Test
    void loads() {
        logger.info("Success!");
    }
}

