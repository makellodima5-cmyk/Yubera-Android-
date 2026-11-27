package com.yubera.app.features.updates
import java.net.URI
import java.util.Locale
object AppUpdateManifestPolicy {
 const val SUPPORTED_SCHEMA_VERSION=1
 const val MAX_APK_BYTES=250L*1024L*1024L
 val ALLOWED_APK_HOSTS=setOf("github.com","objects.githubusercontent.com","release-assets.githubusercontent.com")
 fun isValid(m:AppUpdateManifestDto)=m.schemaVersion==SUPPORTED_SCHEMA_VERSION&&m.versionCode>0&&m.versionName.isNotBlank()&&m.versionName.length<=40&&m.minimumSupportedVersionCode in 1..m.versionCode&&m.apkSha256.matches(Regex("(?i)[0-9a-f]{64}"))&&m.fileSizeBytes in 1..MAX_APK_BYTES&&isAllowedApkUrl(m.apkUrl)
 fun isNewer(manifestVersionCode:Int,installedVersionCode:Int)=manifestVersionCode>installedVersionCode
 fun isAllowedApkUrl(value:String):Boolean{val uri=runCatching{URI(value)}.getOrNull()?:return false;if(!uri.scheme.equals("https",true)||uri.rawUserInfo!=null||uri.port !in listOf(-1,443))return false;val host=uri.host?.lowercase(Locale.ROOT)?.removeSuffix(".")?:return false;return host in ALLOWED_APK_HOSTS}
}
