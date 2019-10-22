package com.example.bleinquirer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class LiveObject<ReadType, WriteType>(
    private val value: WriteType,
    private val converter: (WriteType) -> ReadType = fun (value: WriteType): ReadType = value as ReadType) {

    private val live = MutableLiveData<ReadType>()

    fun update(accessor: (WriteType) -> Boolean): Boolean {
        if (accessor(value)) {
            live.postValue(converter(value))
            return true
        }
        return false
    }

    fun live() = live as LiveData<ReadType>
}
