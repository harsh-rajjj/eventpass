package com.eventpass.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private static final long SLOW_EXECUTION_THRESHOLD_MS = 500L;

    /**
     * 1. @Around Advice: Times every method in any *Service class under com.eventpass.
     *    Logs WARN if execution takes > 500ms; otherwise logs DEBUG.
     */
    @Around("execution(* com.eventpass..*Service.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
            String methodName = joinPoint.getSignature().getName();

            if (durationMs > SLOW_EXECUTION_THRESHOLD_MS) {
                log.warn("SLOW SERVICE CALL: {}.{}() took {} ms (threshold={} ms)",
                        className, methodName, durationMs, SLOW_EXECUTION_THRESHOLD_MS);
            } else {
                log.debug("{}.{}() executed in {} ms", className, methodName, durationMs);
            }
        }
    }

    /**
     * 2. @AfterThrowing Advice: Logs whenever any *Service method throws an exception.
     */
    @AfterThrowing(
            pointcut = "execution(* com.eventpass..*Service.*(..))",
            throwing = "ex"
    )
    public void logServiceException(JoinPoint joinPoint, Throwable ex) {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.warn("Exception in {}.{}(): {} - {}",
                className, methodName, ex.getClass().getSimpleName(), ex.getMessage());
    }

    /**
     * 3. @AfterReturning Advice: Logs an audit trail entry whenever a method
     *    annotated with @Audited completes without throwing an exception.
     */
    @AfterReturning(
            pointcut = "@annotation(audited)",
            returning = "result"
    )
    public void logAuditedAction(JoinPoint joinPoint, Audited audited, Object result) {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String actionName = audited.action().isBlank()
                ? className + "." + methodName
                : audited.action();

        log.info("AUDIT action={} method={}.{}() completed successfully",
                actionName, className, methodName);
    }
}