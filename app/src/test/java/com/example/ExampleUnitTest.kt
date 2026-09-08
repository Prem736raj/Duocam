package com.example

import androidx.camera.core.CameraSelector
import com.example.camera.CameraCapabilities
import com.example.camera.CameraCapabilityState
import com.example.db.Script
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun script_metrics_are_derived_from_saved_content() {
    val script = Script(
      title = "Test",
      content = "One two three four",
    )

    assertEquals(4, script.wordCount)
    assertEquals(1, script.getEstimatedReadTimeSeconds(wordsPerMinute = 240))
  }

  @Test
  fun concurrent_capability_is_only_true_for_a_verified_camera_pair() {
    val supported = CameraCapabilities(
      state = CameraCapabilityState.DualCameraSupported,
      concurrentCameraSelectors = CameraSelector.DEFAULT_BACK_CAMERA to
        CameraSelector.DEFAULT_FRONT_CAMERA,
    )
    val singleOnly = CameraCapabilities(
      state = CameraCapabilityState.SingleCameraSupported,
      supportsBack = true,
    )

    assertTrue(supported.supportsConcurrent)
    assertFalse(singleOnly.supportsConcurrent)
  }
}
