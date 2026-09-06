package com.example.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

enum class CameraCapabilityState {
    DualCameraSupported,
    SingleCameraSupported,
    PermissionRequired,
    CameraUnavailable,
    InitializationError,
}

data class CameraCapabilities(
    val state: CameraCapabilityState,
    val supportsFront: Boolean = false,
    val supportsBack: Boolean = false,
    val supportsTorch: Boolean = false,
    val backCameraSelector: CameraSelector? = null,
    val frontCameraSelector: CameraSelector? = null,
    val concurrentCameraSelectors: Pair<CameraSelector, CameraSelector>? = null,
    val errorMessage: String? = null,
) {
    val supportsConcurrent: Boolean
        get() = concurrentCameraSelectors != null
}

object CameraCapabilityManager {
    fun inspect(context: Context, onResult: (CameraCapabilities) -> Unit) {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onResult(CameraCapabilities(CameraCapabilityState.PermissionRequired))
            return
        }

        val appContext = context.applicationContext
        val providerFuture = ProcessCameraProvider.getInstance(appContext)
        providerFuture.addListener(
            {
                try {
                    onResult(inspectProvider(providerFuture.get()))
                } catch (error: Throwable) {
                    onResult(
                        CameraCapabilities(
                            state = CameraCapabilityState.InitializationError,
                            errorMessage = error.message ?: "Camera initialization failed.",
                        ),
                    )
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    private fun inspectProvider(provider: ProcessCameraProvider): CameraCapabilities {
        val backSelector = findSelector(provider, CameraSelector.LENS_FACING_BACK)
        val frontSelector = findSelector(provider, CameraSelector.LENS_FACING_FRONT)
        val concurrentSelectors = findConcurrentSelectors(provider)
        val supportsBack = backSelector != null
        val supportsFront = frontSelector != null
        val supportsTorch = backSelector?.let { selector ->
            runCatching { provider.getCameraInfo(selector).hasFlashUnit() }.getOrDefault(false)
        } ?: false

        val state = when {
            concurrentSelectors != null -> CameraCapabilityState.DualCameraSupported
            supportsBack || supportsFront -> CameraCapabilityState.SingleCameraSupported
            else -> CameraCapabilityState.CameraUnavailable
        }

        return CameraCapabilities(
            state = state,
            supportsFront = supportsFront,
            supportsBack = supportsBack,
            supportsTorch = supportsTorch,
            backCameraSelector = backSelector,
            frontCameraSelector = frontSelector,
            concurrentCameraSelectors = concurrentSelectors,
        )
    }

    private fun findSelector(
        provider: ProcessCameraProvider,
        lensFacing: Int,
    ): CameraSelector? {
        return runCatching {
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            if (provider.hasCamera(selector)) selector else null
        }.getOrNull()
    }

    private fun findConcurrentSelectors(
        provider: ProcessCameraProvider,
    ): Pair<CameraSelector, CameraSelector>? {
        return runCatching {
            provider.availableConcurrentCameraInfos.firstNotNullOfOrNull { cameraInfos ->
                val back = cameraInfos.firstOrNull {
                    it.lensFacing == CameraSelector.LENS_FACING_BACK
                }?.cameraSelector
                val front = cameraInfos.firstOrNull {
                    it.lensFacing == CameraSelector.LENS_FACING_FRONT
                }?.cameraSelector
                if (back != null && front != null) back to front else null
            }
        }.getOrNull()
    }
}
