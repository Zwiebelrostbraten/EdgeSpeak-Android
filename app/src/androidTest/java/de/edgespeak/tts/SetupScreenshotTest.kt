package de.edgespeak.tts

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.TypedValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SetupScreenshotTest {
 @Test fun captureActualSetupInLightAndDark() {
  val i = InstrumentationRegistry.getInstrumentation()
  val ctx = i.targetContext
  val manager = ctx.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
  val originalMode = manager.nightMode
  val prefs = ctx.getSharedPreferences("settings", 0)
  val hadConsent = prefs.contains("consent"); val consent = prefs.getBoolean("consent", false)
  val hadVoice = prefs.contains("voice"); val voice = prefs.getString("voice", null)
  val output = File(ctx.filesDir, "test-output").apply { mkdirs() }
  fun shell(command: String) { i.uiAutomation.executeShellCommand(command).use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() } } }
  try {
   prefs.edit().putBoolean("consent", false).commit()
   for ((name, mode) in listOf("light" to Configuration.UI_MODE_NIGHT_NO, "dark" to Configuration.UI_MODE_NIGHT_YES)) {
    File(output, "setup-$name.png").delete()
    shell("cmd uimode night ${if (name == "dark") "yes" else "no"}")
    val deadline = SystemClock.uptimeMillis() + 10000
    while (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != mode && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
    assertEquals("Night configuration did not settle", mode, ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
    val activity = i.startActivitySync(Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    try {
     i.waitForIdleSync()
     i.runOnMainSync {
      assertEquals(mode, activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
      val value = TypedValue()
      assertTrue(activity.theme.resolveAttribute(android.R.attr.colorBackground, value, true))
      val brightness = android.graphics.Color.red(value.data) + android.graphics.Color.green(value.data) + android.graphics.Color.blue(value.data)
      assertTrue("Native base theme must match $name", if (name == "light") brightness > 600 else brightness < 300)
     }
     // Wait for a rendered window rather than sleeping for an arbitrary duration.
     val drawn = java.util.concurrent.CountDownLatch(1)
     i.runOnMainSync { activity.window.decorView.viewTreeObserver.addOnDrawListener(object : android.view.ViewTreeObserver.OnDrawListener {
      override fun onDraw() { drawn.countDown() }
     }); activity.window.decorView.invalidate() }
     assertTrue("Activity never drew", drawn.await(10, java.util.concurrent.TimeUnit.SECONDS))
     i.waitForIdleSync()
     val bitmap = i.uiAutomation.takeScreenshot()
     assertNotNull("UiAutomation screenshot unavailable", bitmap)
     try { File(output, "setup-$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } } finally { bitmap.recycle() }
    } finally { i.runOnMainSync { activity.finish() }; i.waitForIdleSync() }
   }
  } finally {
   shell("cmd uimode night ${when(originalMode) { UiModeManager.MODE_NIGHT_YES -> "yes"; UiModeManager.MODE_NIGHT_NO -> "no"; else -> "auto" }}")
   val edit = prefs.edit()
   if (hadConsent) edit.putBoolean("consent", consent) else edit.remove("consent")
   if (hadVoice) edit.putString("voice", voice) else edit.remove("voice")
   check(edit.commit())
  }
 }
}
