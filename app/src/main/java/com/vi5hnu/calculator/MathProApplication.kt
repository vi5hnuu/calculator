package com.vi5hnu.calculator

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// The Mobile Ads SDK is deliberately NOT initialised here. Under GDPR it must not run until
// UMP consent has been gathered, and the consent form needs an Activity — so both live in
// MainActivity via AdsConsentManager.
@HiltAndroidApp
class MathProApplication : Application()
