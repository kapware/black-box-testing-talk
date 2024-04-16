package com.kapware.blackbox.case1;

import com.github.dockerjava.api.command.InspectContainerResponse;
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
    private static Logger logger = LoggerFactory.getLogger(Case1IT.class);
    GenericContainer appContainer;

    @BeforeEach
    void setup() {
        var testedImage = System.getenv("TESTED_IMAGE");
        appContainer = new AppContainer(testedImage);
        appContainer.waitingFor(Wait.forHttp("/events/").forPort(8080));
        appContainer.withExposedPorts(8080);
        appContainer.start();
    }

    @Test
    void loads() {
        logger.info("Success!");
    }

    static class AppContainer extends GenericContainer {
        AppContainer(String dockerImageName) {
            super(dockerImageName);
        }

        @Override
        protected void containerIsStarted(InspectContainerResponse containerInfo) {
            super.containerIsStarted(containerInfo);
            followOutput(new Slf4jLogConsumer(logger).withPrefix("app"));
        }
    }
}
