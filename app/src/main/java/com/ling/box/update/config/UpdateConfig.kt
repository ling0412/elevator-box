package com.ling.box.update.config

/**
 * 更新相关的配置常量
 */
object UpdateConfig {
    /**
     * GitHub 仓库所有者
     */
    const val GITHUB_OWNER = "ling0412"

    /**
     * GitHub 仓库名称
     */
    const val GITHUB_REPO = "elevator-box"

    /**
     * GitHub API 基础 URL
     */
    const val GITHUB_API_BASE_URL = "https://api.github.com"

    /**
     * GitHub 仓库页面 URL
     */
    const val GITHUB_REPO_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"

    /**
     * GitHub 用户主页 URL
     */
    const val GITHUB_USER_URL = "https://github.com/$GITHUB_OWNER"

    /**
     * 下载页面 URL
     */
    const val DOWNLOAD_URL = "https://alist.satou.uk/"

    /**
     * 获取最新版本的 API URL
     */
    fun getLatestReleaseApiUrl(): String {
        return "$GITHUB_API_BASE_URL/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }
}

