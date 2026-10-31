package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notification.PrayerNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context and verify elapsed time formatting`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Noor Prayer", appName)
    assertEquals("12m 34s", PrayerNotificationHelper.formatElapsedSecondsOrHours(12 * 60 + 34))
    assertEquals("1h 15m", PrayerNotificationHelper.formatElapsedSecondsOrHours(75 * 60 + 20))
  }
}
