package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.state.AtlasConstants
import com.example.ui.viewmodel.AtlasViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Atlas Area 7`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Atlas Area 7", appName)
    }

    @Test
    fun `portal viewModel manages page loading popup and fullscreen state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AtlasViewModel(app)

        assertEquals(AtlasConstants.PORTAL_URL, viewModel.uiState.value.currentUrl)
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.onPageFinished(AtlasConstants.PORTAL_URL, "ATLAS | AZKO AREA 7", canGoBack = true)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.hasFinishedInitialLoad)
        assertTrue(viewModel.uiState.value.canGoBack)
        assertNull(viewModel.uiState.value.errorMessage)

        viewModel.openPopup()
        assertTrue(viewModel.uiState.value.isPopupOpen)
        viewModel.updatePopupProgress(100, "https://docs.google.com/spreadsheets/d/123", "Sales Report", true)
        assertEquals("Sales Report", viewModel.uiState.value.popupTitle)
        assertFalse(viewModel.uiState.value.isPopupLoading)
        viewModel.closePopup()
        assertFalse(viewModel.uiState.value.isPopupOpen)

        viewModel.toggleFullscreen()
        assertTrue(viewModel.uiState.value.isFullscreen)
        viewModel.setFullscreen(false)
        assertFalse(viewModel.uiState.value.isFullscreen)
    }
}
