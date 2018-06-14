// IEventCallback.aidl
package com.example.xiaodongwang.lambdamaster;

// Declare any non-default types here with import statements

interface IEventCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void eventCallback(String name, String payload);
}
