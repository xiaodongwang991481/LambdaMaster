package com.example.xiaodongwang.lambdamaster

import android.os.Parcel
import android.os.Parcelable

data class RabbitMQMessage(val actionName: String, val properties: String) {
}