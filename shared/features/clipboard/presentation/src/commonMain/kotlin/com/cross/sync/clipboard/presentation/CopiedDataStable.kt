@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.cross.sync.clipboard.presentation

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@Stable
sealed class CopiedDataStable(
    open val id: Uuid,
    val date: Instant,
) {
    @Stable
    class Text(
        id: Uuid = Uuid.random(),
        val text: String,
        date: Instant = Clock.System.now(),
    ) : CopiedDataStable(id, date) {
        override fun hashCode(): Int {
            return text.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this::class != this::class) return false

            return text == (other as? Text)?.text
        }
    }

    @Stable
    class Image(
        id: Uuid = Uuid.random(),
        val imagePath: String,
        date: Instant = Clock.System.now(),
    ) : CopiedDataStable(id, date) {
        override fun hashCode(): Int = imagePath.hashCode()
        override fun equals(other: Any?): Boolean {
            println("$imagePath == ${(other as? Image)?.imagePath}")
            if (other == null || other::class != this::class) return false

            return imagePath == (other as? Image)?.imagePath
        }
    }

    @Stable
    class File(
        id: Uuid = Uuid.random(),
        val filePaths: List<String>,
        date: Instant = Clock.System.now(),
    ) : CopiedDataStable(id, date) {
        override fun hashCode(): Int = filePaths.fold(0) { acc, s -> acc * 31 + s.hashCode() }
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || other::class != this::class) return false

            val o = other as? File ?: return false
            return filePaths == o.filePaths
        }
    }
}

fun CopiedData.toStable(): CopiedDataStable = when (this) {
    is CopiedData.Text -> CopiedDataStable.Text(id, text, date)
    is CopiedData.FormattedText -> CopiedDataStable.Text(id, text, date)
    is CopiedData.Image -> CopiedDataStable.Image(id, imagePath, date)
    is CopiedData.File -> CopiedDataStable.File(id, filePaths, date)
}

//suspend fun CopiedDataStable.toDomain(): CopiedData = when (this) {
//    is CopiedDataStable.Text -> CopiedData.Text(id, text, date)
//    is CopiedDataStable.Image -> CopiedData.Image(id, image.toBase64())
//}

expect fun String.base64ToImageBitmap(): ImageBitmap

//suspend fun ImageBitmap.toBase64(): String {
////    val buffer: IntArray =
////    withContext(Dispatchers.Main){
////        readPixels(buffer = buffer)
////    }
//    return this.toPixelMap().buffer.toBase64()
//}


//fun IntArray.toBase64(): String {
//    val byteBuffer = ByteBuffer.allocate(this.size * Int.SIZE_BYTES)
//    for (i in this) {
//        byteBuffer.putInt(i)
//    }
//    val byteArray = byteBuffer.array()
//    return Base64.encode(byteArray)
//}