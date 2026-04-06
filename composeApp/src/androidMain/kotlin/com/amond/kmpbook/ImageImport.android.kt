package com.amond.kmpbook

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImageImportActions(): ImageImportActions {
    val context = LocalContext.current
    var pendingImage by remember { mutableStateOf<LoadedImage?>(null) }
    var imageVersion by remember { mutableIntStateOf(0) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        bitmap?.let {
            pendingImage = LoadedImage(
                image = it.scaledDown().asImageBitmap(),
                widthPx = it.width,
                heightPx = it.height,
                sourceLabel = "카메라 촬영 ${it.width}x${it.height}",
            )
            imageVersion += 1
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            decodeBitmap(context, it)?.let { bitmap ->
                pendingImage = LoadedImage(
                    image = bitmap.asImageBitmap(),
                    widthPx = bitmap.width,
                    heightPx = bitmap.height,
                    sourceLabel = "불러온 이미지 ${bitmap.width}x${bitmap.height}",
                )
                imageVersion += 1
            }
        }
    }

    return remember(imageVersion, pendingImage) {
        ImageImportActions(
            launchCamera = { cameraLauncher.launch(null) },
            launchGallery = { galleryLauncher.launch("image/*") },
            imageVersion = imageVersion,
            consumeImage = {
                val image = pendingImage
                pendingImage = null
                image
            },
        )
    }
}

private fun decodeBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = false
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
    return bitmap?.scaledDown()
}

private fun Bitmap.scaledDown(maxEdge: Int = 1600): Bitmap {
    val largest = maxOf(width, height)
    if (largest <= maxEdge) return this
    val scale = maxEdge.toFloat() / largest.toFloat()
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt(),
        (height * scale).toInt(),
        true,
    )
}
