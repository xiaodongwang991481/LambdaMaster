package com.example.xiaodongwang.lambdamaster

import android.os.Parcel
import android.os.Parcelable

data class Lambda(val name: String, val package_name: String) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(package_name)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "name=$name,package_name=$package_name"
    }

    override fun equals(other: Any?): Boolean {
        val otherLambda = other as? Lambda
        if (otherLambda == null) {
            return false
        } else {
            return name == otherLambda.name
        }
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object CREATOR : Parcelable.Creator<Lambda> {
        override fun createFromParcel(parcel: Parcel): Lambda {
            return Lambda(parcel)
        }

        override fun newArray(size: Int): Array<Lambda?> {
            return arrayOfNulls(size)
        }
    }
}