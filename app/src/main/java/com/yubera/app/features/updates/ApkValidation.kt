package com.yubera.app.features.updates
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.security.MessageDigest
object ApkValidation {
 fun matchesInstalledPackage(context:Context,apk:File,expectedVersionCode:Int):Boolean=runCatching{
  val pm=context.packageManager
  val flags=if(Build.VERSION.SDK_INT>=28)PackageManager.GET_SIGNING_CERTIFICATES else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
  val archive=pm.getPackageArchiveInfo(apk.absolutePath,flags)?:return false
  val installed=pm.getPackageInfo(context.packageName,flags)
  archive.packageName==context.packageName&&PackageInfoCompat.getLongVersionCode(archive)==expectedVersionCode.toLong()&&signerDigests(archive).isNotEmpty()&&signerDigests(archive)==signerDigests(installed)
 }.getOrDefault(false)
 private fun signerDigests(info:PackageInfo):List<String>{
  val signatures=if(Build.VERSION.SDK_INT>=28)info.signingInfo?.apkContentsSigners?.toList().orEmpty()else{@Suppress("DEPRECATION") info.signatures?.toList().orEmpty()}
  return signatures.map{MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).joinToString(""){"%02x".format(it)}}.sorted()
 }
}
