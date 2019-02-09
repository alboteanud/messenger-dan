package com.craiovadata.android.messenger.util

import android.media.MediaDataSource

class MyMediaDataSource(val data: ByteArray) : MediaDataSource() {

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        // Clamp reads past the end of the source.
        if (position >= data.size) return -1 // -1 indicates EOF

        var size2: Int = size
        val endPosition: Int = (position + size).toInt()
        if (endPosition > data.size)
            size2 -= endPosition - data.size

        System.arraycopy(data, position.toInt(), buffer, offset, size2)
        return size2
    }

    override fun getSize(): Long {
        return data.size.toLong()
    }

    override fun close() {
    }
}