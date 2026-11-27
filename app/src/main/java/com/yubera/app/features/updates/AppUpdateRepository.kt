package com.yubera.app.features.updates
import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
@Singleton
class AppUpdateRepository @Inject constructor(private val client:OkHttpClient,private val json:Json,@ApplicationContext private val context:Context) {
 suspend fun check():AppUpdateCheckResult=withContext(Dispatchers.IO) {
  val installed=runCatching { PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName,0)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() }
   .getOrElse { return@withContext AppUpdateCheckResult.Failed("Не удалось определить текущую версию приложения.") }
  try {
   val manifest=fetchLatestManifest();require(AppUpdateManifestPolicy.isValid(manifest))
   if(!AppUpdateManifestPolicy.isNewer(manifest.versionCode,installed)){clearStoredUpdate();AppUpdateCheckResult.UpToDate}
   else {val update=manifest.toUpdateInfo();val mandatory=installed<manifest.minimumSupportedVersionCode;storeUpdate(update,mandatory);AppUpdateCheckResult.Available(update,mandatory)}
  }catch(_:Exception){AppUpdateCheckResult.Failed("Проверка обновлений временно недоступна.")}
 }
 fun storedUpdate():AppUpdateInfo? {
  val p=context.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE);val code=p.getInt(KEY_VERSION_CODE,0).takeIf{it>0}?:return null
  val name=p.getString(KEY_VERSION_NAME,null)?.takeIf{it.isNotBlank()}?:return null
  val url=p.getString(KEY_APK_URL,null)?.takeIf(AppUpdateManifestPolicy::isAllowedApkUrl)?:return null
  val sha=p.getString(KEY_SHA256,null)?.takeIf{it.matches(Regex("(?i)[0-9a-f]{64}"))}?:return null
  return AppUpdateInfo(code,name,p.getInt(KEY_MIN_VERSION_CODE,1),p.getString(KEY_NOTES,"").orEmpty(),url,sha.lowercase(),p.getLong(KEY_SIZE,-1).takeIf{it>0},p.getString(KEY_PUBLISHED,"").orEmpty())
 }
 fun storedMandatory()=context.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE).getBoolean(KEY_MANDATORY,false)
 private suspend fun fetchLatestManifest():AppUpdateManifestDto {
  val release=json.parseToJsonElement(fetchText(LATEST_RELEASE_URL,setOf(API_HOST),MAX_RELEASE_JSON_BYTES)).jsonObject
  val tag=release["tag_name"]?.jsonPrimitive?.contentOrNull.orEmpty();require(tag.startsWith("v")&&tag.length<=45)
  val assets=release["assets"]?.jsonArray?:error("Release assets missing")
  val asset=assets.firstOrNull{it.jsonObject["name"]?.jsonPrimitive?.contentOrNull==MANIFEST_ASSET_NAME}?:error("Manifest missing")
  val url=asset.jsonObject["browser_download_url"]?.jsonPrimitive?.contentOrNull?:error("Manifest URL missing")
  val manifest=json.decodeFromString<AppUpdateManifestDto>(fetchText(url,RELEASE_ASSET_HOSTS,MAX_MANIFEST_BYTES))
  require(tag=="v${manifest.versionName}");return manifest
 }
 private fun fetchText(initial:String,hosts:Set<String>,maxBytes:Long):String {
  var url=URL(initial)
  for(redirectCount in 0..MAX_REDIRECTS){
   validateUrl(url,hosts)
   val request=Request.Builder().url(url).header("Accept",if(url.host==API_HOST)"application/vnd.github+json" else "application/octet-stream")
    .header("User-Agent",USER_AGENT).header("X-GitHub-Api-Version",API_VERSION).get().build()
   val response=client.newBuilder().followRedirects(false).followSslRedirects(false).build().newCall(request).execute()
   if(response.code in REDIRECT_CODES){val location=response.header("Location");response.close();require(redirectCount<MAX_REDIRECTS&&location!=null);url=URL(url,location);validateUrl(url,hosts);continue}
   response.use { r->require(r.isSuccessful);val body=r.body?:error("Empty body");require(body.contentLength()<=maxBytes||body.contentLength()==-1L)
    val output=java.io.ByteArrayOutputStream();body.byteStream().use{input->val buffer=ByteArray(DEFAULT_BUFFER_SIZE);var total=0L;while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=maxBytes);output.write(buffer,0,n)}}
    return output.toByteArray().toString(Charsets.UTF_8)
   }
  };error("Too many redirects")
 }
 private fun validateUrl(url:URL,hosts:Set<String>){require(url.protocol.equals("https",true)&&url.userInfo==null&&url.port in listOf(-1,443));require(url.host.lowercase() in hosts)}
 private fun storeUpdate(u:AppUpdateInfo,m:Boolean){context.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE).edit().putInt(KEY_VERSION_CODE,u.versionCode).putString(KEY_VERSION_NAME,u.versionName).putInt(KEY_MIN_VERSION_CODE,u.minimumSupportedVersionCode).putString(KEY_NOTES,u.releaseNotes).putString(KEY_APK_URL,u.apkUrl).putString(KEY_SHA256,u.apkSha256).putLong(KEY_SIZE,u.fileSizeBytes?:-1).putString(KEY_PUBLISHED,u.publishedAt).putBoolean(KEY_MANDATORY,m).apply()}
 private fun clearStoredUpdate()=context.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE).edit().clear().apply()
 private fun AppUpdateManifestDto.toUpdateInfo()=AppUpdateInfo(versionCode,versionName,minimumSupportedVersionCode,releaseNotes,apkUrl,apkSha256.lowercase(),fileSizeBytes,publishedAt)
 companion object {
  const val API_HOST="api.github.com";const val LATEST_RELEASE_URL="https://api.github.com/repos/makellodima5-cmyk/Yubera-Android-/releases/latest"
  const val MANIFEST_ASSET_NAME="yubera-update.json";const val USER_AGENT="YuberaAndroid";const val API_VERSION="2022-11-28"
  const val MAX_RELEASE_JSON_BYTES=1024L*1024L;const val MAX_MANIFEST_BYTES=64L*1024L;const val MAX_REDIRECTS=3
  val REDIRECT_CODES=setOf(301,302,303,307,308);val RELEASE_ASSET_HOSTS=setOf("github.com","objects.githubusercontent.com","release-assets.githubusercontent.com")
  const val PREFERENCES="yubera_app_updates";const val KEY_VERSION_CODE="version_code";const val KEY_VERSION_NAME="version_name";const val KEY_MIN_VERSION_CODE="minimum_version_code";const val KEY_NOTES="release_notes";const val KEY_APK_URL="apk_url";const val KEY_SHA256="apk_sha256";const val KEY_SIZE="apk_size";const val KEY_PUBLISHED="published_at";const val KEY_MANDATORY="mandatory"
 }
}
