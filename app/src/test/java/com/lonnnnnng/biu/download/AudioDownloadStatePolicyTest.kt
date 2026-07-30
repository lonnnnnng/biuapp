package com.lonnnnnng.biu.download

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioDownloadStatePolicyTest {
    @Test
    fun lifecycleOnlyAllowsRecoverableDownloadTransitions() {
        val expected = mapOf(
            AudioDownloadStatus.QUEUED to setOf(
                AudioDownloadStatus.RESOLVING,
                AudioDownloadStatus.PAUSED,
                AudioDownloadStatus.CANCELLED,
            ),
            AudioDownloadStatus.RESOLVING to setOf(
                AudioDownloadStatus.DOWNLOADING,
                AudioDownloadStatus.PAUSED,
                AudioDownloadStatus.FAILED,
                AudioDownloadStatus.CANCELLED,
            ),
            AudioDownloadStatus.DOWNLOADING to setOf(
                AudioDownloadStatus.PAUSED,
                AudioDownloadStatus.PUBLISHING,
                AudioDownloadStatus.FAILED,
                AudioDownloadStatus.CANCELLED,
            ),
            AudioDownloadStatus.PAUSED to setOf(
                AudioDownloadStatus.QUEUED,
                AudioDownloadStatus.CANCELLED,
            ),
            AudioDownloadStatus.PUBLISHING to setOf(
                AudioDownloadStatus.PAUSED,
                AudioDownloadStatus.COMPLETED,
                AudioDownloadStatus.FAILED,
                AudioDownloadStatus.CANCELLED,
            ),
            AudioDownloadStatus.COMPLETED to emptySet(),
            AudioDownloadStatus.FAILED to setOf(
                AudioDownloadStatus.QUEUED,
                AudioDownloadStatus.CANCELLED,
            ),
            AudioDownloadStatus.CANCELLED to setOf(AudioDownloadStatus.QUEUED),
        )

        val actual = AudioDownloadStatus.entries.associateWith(AudioDownloadStatePolicy::allowedTargets)

        assertEquals(expected, actual)
    }
}
