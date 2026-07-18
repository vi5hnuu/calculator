package com.vi5hnu.calculator.feature.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.vi5hnu.calculator.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Drives the Google User Messaging Platform (UMP) consent flow and, only once consent allows,
 * initialises the Mobile Ads SDK.
 *
 * The ordering is the whole point: under GDPR the app must gather consent *before* it requests
 * a personalised ad. [canRequestAds] starts false and flips true only after UMP reports the
 * user is either outside a consent region or has answered the form, at which point the banner
 * is allowed to compose.
 *
 * Held by the Activity (the consent form needs an Activity to present), not the Application.
 */
class AdsConsentManager(context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private val mobileAdsInitialised = AtomicBoolean(false)

    /** Observable by Compose; the banner watches this and only appears once it is true. */
    var canRequestAds by mutableStateOf(consentInformation.canRequestAds())
        private set

    /**
     * Requests the latest consent status and shows the form if UMP requires it. Safe to call
     * on every launch — a user who has already consented sees nothing and ads start
     * immediately.
     */
    fun gatherConsentThenInitialize(activity: Activity) {
        // Real geography is used. Do NOT force EEA here: without a GDPR message configured in
        // the AdMob console, forcing EEA leaves canRequestAds() false forever and no ad — test
        // or live — ever shows. To exercise the EEA form during development, first create a
        // GDPR message in AdMob (Privacy & messaging), then temporarily wrap the builder with:
        //   setConsentDebugSettings(
        //     ConsentDebugSettings.Builder(activity)
        //       .setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA)
        //       .addTestDeviceHashedId("<hash from logcat>")
        //       .build())
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Info is up to date; present the form only if one is required and pending.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // Fired whether the form showed, was dismissed, or errored — in every case
                    // canRequestAds now reflects the final decision.
                    initializeAdsIfAllowed(activity)
                }
            },
            {
                // Update failed (e.g. offline, or the consent endpoint is unreachable). Fall
                // back to the cached consent state rather than retrying forever.
                initializeAdsIfAllowed(activity)
            },
        )

        // A returning user whose consent is already on file can start ads without waiting for
        // the round-trip above.
        initializeAdsIfAllowed(activity)
    }

    private fun initializeAdsIfAllowed(context: Context) {
        // Release is strict: real ads only once UMP confirms consent (or that none is needed).
        // Debug additionally allows ads whenever consent can't be resolved — the consent
        // endpoint is often unreachable on a cold emulator, and debug only ever serves Google's
        // test ads, so there is no revenue or compliance concern in showing them anyway.
        canRequestAds = consentInformation.canRequestAds() || BuildConfig.DEBUG
        if (canRequestAds && mobileAdsInitialised.compareAndSet(false, true)) {
            // initialize does disk/network I/O; keep it off the main thread.
            Thread { MobileAds.initialize(context.applicationContext) }.start()
        }
    }
}
