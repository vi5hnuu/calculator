package com.vi5hnu.calculator.feature.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.vi5hnu.calculator.BuildConfig
import com.vi5hnu.calculator.core.designsystem.MathProTheme

/**
 * A single anchored adaptive banner, pinned to the bottom of the app.
 *
 * Chosen because it is the least intrusive way to monetise a calculator: it never overlaps a
 * key or a result, it loads once, and it collapses to nothing when there is no network. The
 * ad unit comes from [BuildConfig] — Google's public test unit in debug, the live unit in
 * release — so development traffic never touches the real account.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val colors = MathProTheme.colors

    // @Preview / test harnesses have no Play Services; instantiating an AdView there throws.
    if (LocalInspectionMode.current) return

    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp

    // Reserve the banner's height up front so the keypad above it does not jump when the ad
    // fills a moment after the screen appears.
    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }

    // Keyed on width so a genuine width change (large-screen resize, split-screen) rebuilds
    // the banner at the correct size instead of stretching a stale one.
    val adView = remember(adSize) {
        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = BuildConfig.BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    // AdView holds a WebView and must be torn down with the composable — and with the old
    // instance whenever a width change produces a new one — or it leaks.
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adSize.height.dp)
            .background(colors.backgroundBottom),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
    }
}
