package org.wallentines.mdproxy;

public enum TestResult {

    PASS,
    FAIL,
    NOT_ENOUGH_INFO;

    static TestResult fromBoolean(boolean bool) {
        return bool ? PASS : FAIL;
    }

}
