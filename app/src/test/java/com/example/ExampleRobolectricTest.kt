package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Pixel Donation", appName)
  }

  @Test
  fun `test viewModel initialization and login`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MainViewModel(app)
    viewModel.login("test@example.com", "mypassword")
    // Let's print out if any error or state changed
    println("Current User after login: ${viewModel.currentUser.value}")
  }
}
