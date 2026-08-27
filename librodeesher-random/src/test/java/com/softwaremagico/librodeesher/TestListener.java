package com.softwaremagico.librodeesher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG listener that logs the life cycle of every test (suite start/end, test start/success/failure)
 * through SLF4J, so a human reviewing the CI/console output can follow what was executed without
 * opening the surefire XML reports.
 *
 * <p>Registered in each module's {@code src/test/resources/testng.xml}. Mirrors the listener used in
 * ThinkMachine-4E so both projects behave consistently.</p>
 */
public class TestListener implements ITestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        LOGGER.info("### Test started '{}' from '{}'.", result.getMethod().getMethodName(),
                result.getTestClass().getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOGGER.info("### Test finished '{}' from '{}' ({}ms).", result.getMethod().getMethodName(),
                result.getTestClass().getName(), result.getEndMillis() - result.getStartMillis());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOGGER.error("### Test failed '{}' from '{}' ({}ms).", result.getMethod().getMethodName(),
                result.getTestClass().getName(), result.getEndMillis() - result.getStartMillis(),
                result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        // Nothing to log: skipped tests are already reported by surefire/testng.
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        // Nothing to log: flaky-but-accepted tests are already reported by surefire/testng.
    }

    @Override
    public void onStart(ITestContext context) {
        LOGGER.info("##### Starting tests from '{}'.", context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        LOGGER.info("##### Tests finished from '{}'.", context.getName());
    }
}
