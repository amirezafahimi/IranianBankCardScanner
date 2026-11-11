/*
 * Copyright 2026 Amirreza Fahimi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.amirezafahimi.cardscanner.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private const val PREFS_NAME = "card_scanner_prefs"
private const val KEY_CAMERA_PERMISSION_PERMANENTLY_DENIED = "camera_permission_permanently_denied"

/**
 * Persists whether camera permission was permanently denied (Don't ask again),
 * so the settings dialog can be shown directly on the next scanner open.
 */
internal object CardScannerCameraPermissionStore {

    fun markPermanentlyDenied(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CAMERA_PERMISSION_PERMANENTLY_DENIED, true)
            .apply()
    }

    fun clearPermanentlyDenied(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CAMERA_PERMISSION_PERMANENTLY_DENIED, false)
            .apply()
    }

    fun isPermanentlyDenied(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CAMERA_PERMISSION_PERMANENTLY_DENIED, false)
    }
}

internal data class CameraPermissionDialogState(
    val showRequest: Boolean,
    val showSettings: Boolean
)

/**
 * Resolves which permission dialog (if any) should be shown when the scanner opens.
 */
internal fun resolveCameraPermissionDialogState(context: Context): CameraPermissionDialogState {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        return CameraPermissionDialogState(showRequest = false, showSettings = false)
    }

    val activity = context.findActivity()
    val shouldShowRationale = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
    } ?: false

    // User changed permission to "Ask every time" — system allows requesting again.
    if (shouldShowRationale && CardScannerCameraPermissionStore.isPermanentlyDenied(context)) {
        CardScannerCameraPermissionStore.clearPermanentlyDenied(context)
    }

    val isPermanentlyDenied = !shouldShowRationale &&
        CardScannerCameraPermissionStore.isPermanentlyDenied(context)

    return if (isPermanentlyDenied) {
        CameraPermissionDialogState(showRequest = false, showSettings = true)
    } else {
        CameraPermissionDialogState(showRequest = true, showSettings = false)
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
