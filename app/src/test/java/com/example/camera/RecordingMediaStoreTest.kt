package com.example.camera

import android.os.Build
import android.provider.MediaStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecordingMediaStoreTest {

    @Test
    fun api36_usesPrimaryVideoCollectionAndRelativePath() {
        val target = RecordingMediaStore.createTarget(
            mode = CaptureMode.DUAL,
            timestampMs = 1_700_000_000_000L,
            sdkInt = 36,
        )

        assertEquals(
            MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY,
            ),
            target.collectionUri,
        )

        assertEquals(
            "DuoCam_1700000000000_dual.mp4",
            target.contentValues.getAsString(
                MediaStore.Video.Media.DISPLAY_NAME,
            ),
        )

        assertEquals(
            "Movies/DuoCam",
            target.contentValues.getAsString(
                MediaStore.Video.Media.RELATIVE_PATH,
            ),
        )
    }

    @Test
    fun api28_usesLegacyVideoCollectionWithoutRelativePath() {
        val target = RecordingMediaStore.createTarget(
            mode = CaptureMode.SINGLE,
            timestampMs = 1_700_000_000_000L,
            sdkInt = Build.VERSION_CODES.P,
        )

        assertEquals(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            target.collectionUri,
        )

        assertFalse(
            target.contentValues.containsKey(
                MediaStore.Video.Media.RELATIVE_PATH,
            ),
        )

        assertNull(
            target.contentValues.getAsString(
                MediaStore.Video.Media.RELATIVE_PATH,
            ),
        )
    }
}
