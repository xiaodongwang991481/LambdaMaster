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

    companion object CREATOR : Parcelable.Creator<Lambda> {
        override fun createFromParcel(parcel: Parcel): Lambda {
            return Lambda(parcel)
        }

        override fun newArray(size: Int): Array<Lambda?> {
            return arrayOfNulls(size)
        }
    }
}