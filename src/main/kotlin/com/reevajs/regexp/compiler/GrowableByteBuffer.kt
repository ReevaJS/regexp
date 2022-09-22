package com.reevajs.regexp.compiler

import java.nio.ByteBuffer

class GrowableByteBuffer {
    var buffer = ByteBuffer.allocate(512)
        private set

    val size = buffer.capacity()

    var position: Int
        get() = buffer.position()
        set(value) {
            buffer.position(value)
        }

    fun finalize(): ByteArray {
        val bufferSize = position
        position = 0
        val array = ByteArray(bufferSize)
        buffer.get(array)
        buffer.clear()
        return array
    }

    fun writeByte(v: Byte) = apply {
        ensureCapacity(1)
        buffer.put(v)
    }

    fun writeShort(v: Short) = apply {
        ensureCapacity(2)
        buffer.putShort(v)
    }

    fun writeInt(v: Int) = apply {
        ensureCapacity(4)
        buffer.putInt(v)
    }

    fun writeLong(v: Long) = apply {
        ensureCapacity(8)
        buffer.putLong(v)
    }

    fun writeBytes(array: ByteArray) = apply {
        ensureCapacity(array.size)
        buffer.put(array)
    }

    fun readByte() = buffer.get()

    fun readShort() = buffer.short

    fun readInt() = buffer.int

    fun readLong() = buffer.long

    private fun ensureCapacity(n: Int) {
        if (position + n >= size) {
            val newBuffer = ByteBuffer.allocate(size * 2)
            newBuffer.put(buffer)
            buffer = newBuffer
        }
    }
}
