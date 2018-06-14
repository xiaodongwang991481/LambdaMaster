// IEvent.aidl
package com.example.xiaodongwang.lambdamaster;

import com.example.xiaodongwang.lambdamaster.Event;
import com.example.xiaodongwang.lambdamaster.IEventCallback;

// Declare any non-default types here with import statements

interface IEvent {

    void sendMessage(in Event event, in IEventCallback eventCallback);
}
