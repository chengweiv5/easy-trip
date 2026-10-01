package com.yangchengwei.easytrip.settings

import java.net.URI

const val MAX_APK_BYTES = 250L * 1024 * 1024
private const val RELEASE_PREFIX = "/chengweiv5/easy-trip/releases/download/"

data class AppVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    companion object {
        fun parse(value: String): AppVersion? {
            val match = Regex("v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)").matchEntire(value) ?: return null
            val numbers = match.groupValues.drop(1).map { it.toIntOrNull() ?: return null }
            return AppVersion(numbers[0], numbers[1], numbers[2])
        }
    }
}

data class ReleaseAsset(val name: String, val url: String, val size: Long, val sha256: String) {
    fun isTrusted(tag: String): Boolean = runCatching {
        val uri = URI(url)
        AppVersion.parse(tag) != null && name == "easy-trip-$tag-release.apk" &&
            uri.scheme == "https" && uri.host == "github.com" && uri.userInfo == null && uri.port == -1 &&
            uri.rawQuery == null && uri.rawFragment == null && uri.rawPath == "$RELEASE_PREFIX$tag/$name" &&
            size in 1..MAX_APK_BYTES && Regex("[a-fA-F0-9]{64}").matches(sha256)
    }.getOrDefault(false)
}

data class OfficialRelease(
    val tag: String,
    val notes: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val asset: ReleaseAsset?,
)

sealed interface ReleaseDecision {
    data object Current : ReleaseDecision
    data object Ahead : ReleaseDecision
    data class Available(val release: OfficialRelease) : ReleaseDecision
}

fun decideRelease(installedVersion: String, release: OfficialRelease): ReleaseDecision {
    require(!release.draft && !release.prerelease) { "不是正式发布版本" }
    val installed = requireNotNull(AppVersion.parse(installedVersion)) { "当前版本格式无法比较" }
    val remote = requireNotNull(AppVersion.parse(release.tag)) { "正式版本格式无法比较" }
    return when {
        installed > remote -> ReleaseDecision.Ahead
        installed == remote -> ReleaseDecision.Current
        else -> {
            require(release.asset?.isTrusted(release.tag) == true) { "正式安装包缺少可验证的下载信息" }
            ReleaseDecision.Available(release)
        }
    }
}

/** Android's archive metadata is projected here so trust decisions have a pure test seam. */
data class PackageIdentity(val packageName: String, val versionName: String, val versionCode: Long, val signers: Set<String>)

fun requireTrustedUpgrade(installed: PackageIdentity, candidate: PackageIdentity, releaseTag: String) {
    require(candidate.packageName == installed.packageName) { "安装包不是 Easy Trip" }
    val remote = requireNotNull(AppVersion.parse(releaseTag)) { "发布版本无效" }
    val candidateVersion = requireNotNull(AppVersion.parse(candidate.versionName)) { "安装包版本无效" }
    val installedVersion = requireNotNull(AppVersion.parse(installed.versionName)) { "当前版本无效" }
    require(candidateVersion == remote && candidateVersion > installedVersion) { "安装包版本不匹配或不是更新" }
    require(candidate.versionCode > installed.versionCode) { "不能安装相同或更旧构建" }
    require(installed.signers.isNotEmpty() && installed.signers == candidate.signers) { "安装包签名不匹配" }
}
