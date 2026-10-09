package com.scrnlimit.screen.time.control.block

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.scrnlimit.screen.time.control.ScrnLimitApp
import com.scrnlimit.screen.time.control.domain.Catalog
import com.scrnlimit.screen.time.control.domain.GuardPolicy

class FeedAccessibilityService : AccessibilityService() {
    private var last = 0L

    override fun onServiceConnected() {
        instance = this
        MonitorService.start(this)
    }

    override fun onDestroy() {
        if (instance == this) instance = null
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val now = System.currentTimeMillis()
        if (now - last < 450L) return
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return
        val graph = runCatching { (application as ScrnLimitApp).graph }.getOrNull() ?: return
        val settings = graph.repo.settings.value
        val sites = graph.repo.websites.value.filter { it.enabled }.map { it.host }
        val monitor = MonitorService.instance ?: return
        if (!settings.blockReels && sites.isEmpty()) {
            last = now
            monitor.onReels(false)
            monitor.onSiteClear()
            return
        }
        last = now
        val hits = harvest()
        if (pkg in Catalog.browsers) {
            val host = findHost(hits)
            val shorts = settings.blockReels && looksLikeShort(hits)
            if (host != null && GuardPolicy.hostBlocked(host, sites)) {
                monitor.onSite(host)
                monitor.onReels(false)
            } else if (shorts) {
                monitor.onSiteClear()
                monitor.onReels(true)
            } else {
                monitor.onSiteClear()
                monitor.onReels(false)
            }
            return
        }
        if (pkg in Catalog.reelsPackages) {
            if (!settings.blockReels) {
                monitor.onReels(false)
                return
            }
            monitor.onReels(score(hits) >= 2 || playerOpen() || feedTabSelected())
        }
    }

    private fun harvest(): List<Hit> {
        val out = ArrayList<Hit>(160)
        collectRoots().forEach { root ->
            val budget = intArrayOf(400)
            walk(root, 0, out, budget)
            runCatching { root.recycle() }
        }
        return out
    }

    private fun playerOpen(): Boolean {
        val roots = collectRoots()
        var found = false
        for (root in roots) {
            if (!found) {
                for (id in playerIds) {
                    val nodes = runCatching { root.findAccessibilityNodeInfosByViewId(id) }.getOrNull()
                    if (!nodes.isNullOrEmpty()) {
                        found = true
                        nodes.forEach { runCatching { it.recycle() } }
                        break
                    }
                }
            }
            runCatching { root.recycle() }
        }
        return found
    }

    private fun collectRoots(): List<AccessibilityNodeInfo> {
        val fromWindows = windows?.mapNotNull { it.root }.orEmpty()
        if (fromWindows.isNotEmpty()) return fromWindows
        val active = rootInActiveWindow ?: return emptyList()
        return listOf(active)
    }

    private fun walk(node: AccessibilityNodeInfo, depth: Int, out: MutableList<Hit>, budget: IntArray) {
        if (budget[0] <= 0 || depth > 16) return
        budget[0]--
        out.add(
            Hit(
                text = node.text?.toString(),
                desc = node.contentDescription?.toString(),
                id = node.viewIdResourceName,
                selected = node.isSelected,
            ),
        )
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            walk(child, depth + 1, out, budget)
            runCatching { child.recycle() }
        }
    }

    private fun score(hits: List<Hit>): Int {
        var total = 0
        hits.forEach { hit ->
            val text = hit.text.orEmpty()
            val desc = hit.desc.orEmpty()
            val id = hit.id.orEmpty()
            if (desc.contains("shorts player", true) || desc.contains("reels player", true)) total += 3
            if (desc.contains("reel by", true)) total += 3
            if (
                id.contains("clips_viewer", true) ||
                id.contains("reel_viewer", true) ||
                id.contains("shorts_player", true) ||
                id.contains("reel_watch_fragment", true) ||
                id.contains("reel_player_page", true) ||
                id.contains("reel_watch_player", true)
            ) {
                total += 3
            }
            if (hit.selected && (feedTab(text) || feedTab(desc))) total += 2
            if (desc.contains("swipe up", true) && (desc.contains("reel", true) || desc.contains("short", true))) total += 2
        }
        return total
    }

    private fun feedTab(value: String): Boolean {
        val clean = value.trim()
        return clean.equals("Reels", true) ||
            clean.equals("Shorts", true) ||
            clean.startsWith("Reels,", true) ||
            clean.startsWith("Shorts,", true) ||
            clean.startsWith("Reels tab", true) ||
            clean.startsWith("Shorts tab", true)
    }

    private fun feedTabSelected(): Boolean {
        val roots = collectRoots()
        var found = false
        for (root in roots) {
            if (!found) {
                for (word in listOf("Reels", "Shorts")) {
                    val nodes = runCatching { root.findAccessibilityNodeInfosByText(word) }.getOrNull().orEmpty()
                    for (node in nodes) {
                        if (!found) {
                            val text = node.text?.toString().orEmpty()
                            val desc = node.contentDescription?.toString().orEmpty()
                            if (node.isSelected && (feedTab(text) || feedTab(desc))) found = true
                        }
                        runCatching { node.recycle() }
                    }
                    if (found) break
                }
            }
            runCatching { root.recycle() }
        }
        return found
    }

    private fun findHost(hits: List<Hit>): String? {
        val preferred = hits.filter {
            val id = it.id.orEmpty()
            id.contains("url", true) || id.contains("omnibox", true) || id.contains("location", true)
        }
        return parse(preferred)
    }

    private fun parse(hits: List<Hit>): String? {
        hits.forEach { hit ->
            val raw = listOfNotNull(hit.text, hit.desc).joinToString(" ")
            val match = URL.find(raw) ?: return@forEach
            val host = match.groupValues[1].lowercase().removePrefix("www.")
            if (host.contains('.') && !host.endsWith(".android") && host.length > 3) return host
        }
        return null
    }

    private fun looksLikeShort(hits: List<Hit>): Boolean = hits.any { hit ->
        val blob = "${hit.text.orEmpty()} ${hit.desc.orEmpty()}"
        blob.contains("/shorts/") || blob.contains("/reel/") || blob.contains("/reels/")
    }

    companion object {
        var instance: FeedAccessibilityService? = null
        private val URL = Regex("""(?:https?://)?(?:www\.)?([a-z0-9.-]+\.[a-z]{2,})(?:/[^\s]*)?""", RegexOption.IGNORE_CASE)
        private val playerIds = listOf(
            "com.google.android.youtube:id/reel_watch_fragment_root",
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/reel_watch_player",
        )

        fun pressBack() {
            instance?.performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }
}

private data class Hit(
    val text: String?,
    val desc: String?,
    val id: String?,
    val selected: Boolean,
)
