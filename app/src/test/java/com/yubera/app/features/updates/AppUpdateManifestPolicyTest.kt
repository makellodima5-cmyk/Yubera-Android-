package com.yubera.app.features.updates
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class AppUpdateManifestPolicyTest {
 private fun valid()=AppUpdateManifestDto(versionCode=3,versionName="0.3.0",minimumSupportedVersionCode=1,releaseNotes="notes",apkUrl="https://github.com/makellodima5-cmyk/Yubera-Android-/releases/download/v0.3.0/yubera-0.3.0.apk",apkSha256="a".repeat(64),fileSizeBytes=1024,publishedAt="2026-11-27T00:00:00Z")
 @Test fun validManifestAccepted()=assertTrue(AppUpdateManifestPolicy.isValid(valid()))
 @Test fun versionCodeMustIncrease(){assertTrue(AppUpdateManifestPolicy.isNewer(3,2));assertFalse(AppUpdateManifestPolicy.isNewer(2,2));assertFalse(AppUpdateManifestPolicy.isNewer(1,2))}
 @Test fun httpRejected()=assertFalse(AppUpdateManifestPolicy.isValid(valid().copy(apkUrl="http://github.com/app.apk")))
 @Test fun arbitraryHostRejected()=assertFalse(AppUpdateManifestPolicy.isValid(valid().copy(apkUrl="https://evil.example/app.apk")))
 @Test fun invalidChecksumRejected()=assertFalse(AppUpdateManifestPolicy.isValid(valid().copy(apkSha256="bad")))
 @Test fun invalidMinimumVersionRejected()=assertFalse(AppUpdateManifestPolicy.isValid(valid().copy(minimumSupportedVersionCode=4)))
 @Test fun oversizedApkRejected()=assertFalse(AppUpdateManifestPolicy.isValid(valid().copy(fileSizeBytes=AppUpdateManifestPolicy.MAX_APK_BYTES+1)))
}
