package com.lonnnnnng.biu.download

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadCompletionNotificationCoordinatorTest {
    @Test
    fun removesActiveProgressBeforePostingCompletedNotification() {
        val events = mutableListOf<String>()
        val coordinator = DownloadCompletionNotificationCoordinator(
            postCompletedNotification = { events += "completed" },
            removeForegroundNotification = { events += "remove-progress" },
        )

        coordinator.complete()

        assertEquals(listOf("remove-progress", "completed"), events)
    }

    @Test
    fun removesActiveProgressWhenCompletedTaskCannotBeLoaded() {
        val events = mutableListOf<String>()
        val coordinator = DownloadCompletionNotificationCoordinator(
            postCompletedNotification = null,
            removeForegroundNotification = { events += "remove-progress" },
        )

        coordinator.complete()

        assertEquals(listOf("remove-progress"), events)
    }
}
