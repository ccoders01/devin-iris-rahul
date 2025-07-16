package com.facility.management.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TestExecutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TestExecutionService.class);
    
    public TestExecutionResult executeSeleniumTests(String testClassName) {
        try {
            logger.info("Executing Selenium tests for class: {}", testClassName);
            
            String testPattern = testClassName;
            if (testClassName.contains("*")) {
                testPattern = "*Test";
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                "mvn", "test", 
                "-Dtest=" + testPattern,
                "-DfailIfNoTests=false",
                "-Dmaven.test.failure.ignore=true"
            );
            processBuilder.directory(new File("."));
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return new TestExecutionResult(false, "Test execution timed out", new ArrayList<>());
            }
            
            int exitCode = process.exitValue();
            String output = new String(process.getInputStream().readAllBytes());
            
            logger.info("Maven test execution completed with exit code: {}", exitCode);
            logger.debug("Maven output: {}", output);
            
            List<TestResult> testResults = parseSurefireReports();
            boolean allPassed = testResults.isEmpty() || testResults.stream().allMatch(TestResult::isPassed);
            
            return new TestExecutionResult(allPassed, output, testResults);
            
        } catch (Exception e) {
            logger.error("Error executing Selenium tests", e);
            return new TestExecutionResult(false, "Error executing tests: " + e.getMessage(), new ArrayList<>());
        }
    }
    
    private List<TestResult> parseSurefireReports() {
        List<TestResult> results = new ArrayList<>();
        Path surefireReportsDir = Paths.get("target/surefire-reports");
        
        if (!Files.exists(surefireReportsDir)) {
            logger.warn("Surefire reports directory not found");
            return results;
        }
        
        try {
            Files.walk(surefireReportsDir)
                    .filter(path -> path.toString().endsWith(".xml"))
                    .filter(path -> path.getFileName().toString().startsWith("TEST-"))
                    .forEach(xmlFile -> {
                        try {
                            results.addAll(parseTestResultXml(xmlFile.toFile()));
                        } catch (Exception e) {
                            logger.error("Error parsing test result XML: {}", xmlFile, e);
                        }
                    });
        } catch (IOException e) {
            logger.error("Error reading surefire reports directory", e);
        }
        
        return results;
    }
    
    private List<TestResult> parseTestResultXml(File xmlFile) throws Exception {
        List<TestResult> results = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        
        NodeList testCases = document.getElementsByTagName("testcase");
        for (int i = 0; i < testCases.getLength(); i++) {
            Element testCase = (Element) testCases.item(i);
            String className = testCase.getAttribute("classname");
            String methodName = testCase.getAttribute("name");
            String time = testCase.getAttribute("time");
            
            boolean passed = testCase.getElementsByTagName("failure").getLength() == 0 &&
                           testCase.getElementsByTagName("error").getLength() == 0;
            
            String errorMessage = "";
            if (!passed) {
                NodeList failures = testCase.getElementsByTagName("failure");
                NodeList errors = testCase.getElementsByTagName("error");
                if (failures.getLength() > 0) {
                    errorMessage = failures.item(0).getTextContent();
                } else if (errors.getLength() > 0) {
                    errorMessage = errors.item(0).getTextContent();
                }
            }
            
            results.add(new TestResult(className, methodName, passed, errorMessage, time));
        }
        
        return results;
    }
    
    public static class TestExecutionResult {
        private final boolean allPassed;
        private final String output;
        private final List<TestResult> testResults;
        
        public TestExecutionResult(boolean allPassed, String output, List<TestResult> testResults) {
            this.allPassed = allPassed;
            this.output = output;
            this.testResults = testResults;
        }
        
        public boolean isAllPassed() { return allPassed; }
        public String getOutput() { return output; }
        public List<TestResult> getTestResults() { return testResults; }
    }
    
    public static class TestResult {
        private final String className;
        private final String methodName;
        private final boolean passed;
        private final String errorMessage;
        private final String executionTime;
        
        public TestResult(String className, String methodName, boolean passed, String errorMessage, String executionTime) {
            this.className = className;
            this.methodName = methodName;
            this.passed = passed;
            this.errorMessage = errorMessage;
            this.executionTime = executionTime;
        }
        
        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public boolean isPassed() { return passed; }
        public String getErrorMessage() { return errorMessage; }
        public String getExecutionTime() { return executionTime; }
    }
}
