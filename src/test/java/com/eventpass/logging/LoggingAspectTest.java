package com.eventpass.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(LoggingAspectTest.AopTestConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class LoggingAspectTest {

    @Autowired
    private DummyTestService dummyTestService;

    @BeforeEach
    void enableDebugLogging() {
        ((Logger) LoggerFactory.getLogger(LoggingAspect.class)).setLevel(Level.DEBUG);
    }

    @Test
    void auditedMethod_logsAuditTrailAndExecutionTime(CapturedOutput output) {
        String result = dummyTestService.performAuditedAction();

        assertThat(result).isEqualTo("OK");
        assertThat(output.getOut())
                .contains("AUDIT action=TEST_AUDIT_ACTION method=DummyTestService.performAuditedAction() completed successfully")
                .contains("DummyTestService.performAuditedAction() executed in");
    }

    @Test
    void throwingMethod_logsExceptionInAfterThrowingAdvice(CapturedOutput output) {
        assertThatThrownBy(() -> dummyTestService.failingAction())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Simulated service failure");

        assertThat(output.getOut())
                .contains("Exception in DummyTestService.failingAction(): IllegalStateException - Simulated service failure");
    }

    @Test
    void slowMethod_logsSlowServiceCallWarning(CapturedOutput output) throws InterruptedException {
        dummyTestService.slowAction();

        assertThat(output.getOut())
                .contains("SLOW SERVICE CALL: DummyTestService.slowAction() took");
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class AopTestConfig {

        @Bean
        LoggingAspect loggingAspect() {
            return new LoggingAspect();
        }

        @Bean
        DummyTestService dummyTestService() {
            return new DummyTestService();
        }
    }

    /**
     * Class name ends with "Service" inside com.eventpass.. so it matches
     * the pointcut: execution(* com.eventpass..*Service.*(..))
     */
    static class DummyTestService {

        @Audited(action = "TEST_AUDIT_ACTION")
        public String performAuditedAction() {
            return "OK";
        }

        public void failingAction() {
            throw new IllegalStateException("Simulated service failure");
        }

        public void slowAction() throws InterruptedException {
            Thread.sleep(550);
        }
    }
}
