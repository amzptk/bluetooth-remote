package com.example.bluetoothremote.shortcut

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AppShortcut(
    val id: String,
    var name: String,
    var command: String,
    var colorHex: String = "#E50914",
    var packageName: String = ""
)

object ShortcutManager {

    private const val PREFS_NAME = "remote_shortcuts_prefs"
    private const val KEY_SHORTCUTS = "custom_shortcuts"

    val PRESET_APPS = listOf(
        AppShortcut("yt", "YouTube", "APP:YOUTUBE", "#CC0000", "com.google.android.youtube.tv"),
        AppShortcut("nf", "Netflix", "APP:NETFLIX", "#E50914", "com.netflix.ninja"),
        AppShortcut("vd", "Vidio", "APP:VIDIO", "#FF007A", "com.vidio.android.tv"),
        AppShortcut("ds", "Disney+", "APP:DISNEY", "#0063E5", "in.startv.hotstar"),
        AppShortcut("pv", "Prime", "APP:PRIME", "#00A8E1", "com.amazon.amazonvideo.livingroom"),
        AppShortcut("sp", "Spotify", "APP:SPOTIFY", "#1DB954", "com.spotify.tv.android"),
        AppShortcut("br", "Browser", "APP:BROWSER", "#4285F4", "com.android.chrome"),
        AppShortcut("kd", "Kodi", "APP:KODI", "#17B2E7", "org.xbmc.kodi")
    )

    fun getDefaultShortcuts(): List<AppShortcut> {
        return listOf(
            AppShortcut("slot_1", "YouTube", "APP:YOUTUBE", "#CC0000", "com.google.android.youtube.tv"),
            AppShortcut("slot_2", "Netflix", "APP:NETFLIX", "#E50914", "com.netflix.ninja"),
            AppShortcut("slot_3", "Vidio", "APP:VIDIO", "#FF007A", "com.vidio.android.tv"),
            AppShortcut("slot_4", "Disney+", "APP:DISNEY", "#0063E5", "in.startv.hotstar")
        )
    }

    fun loadShortcuts(context: Context): MutableList<AppShortcut> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_SHORTCUTS, null) ?: return getDefaultShortcuts().toMutableList()

        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<AppShortcut>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    AppShortcut(
                        id = obj.optString("id", "slot_$i"),
                        name = obj.optString("name", "App"),
                        command = obj.optString("command", ""),
                        colorHex = obj.optString("colorHex", "#3700B3"),
                        packageName = obj.optString("packageName", "")
                    )
                )
            }
            if (list.isEmpty()) getDefaultShortcuts().toMutableList() else list
        } catch (e: Exception) {
            getDefaultShortcuts().toMutableList()
        }
    }

    fun saveShortcuts(context: Context, shortcuts: List<AppShortcut>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (sc in shortcuts) {
            val obj = JSONObject()
            obj.put("id", sc.id)
            obj.put("name", sc.name)
            obj.put("command", sc.command)
            obj.put("colorHex", sc.colorHex)
            obj.put("packageName", sc.packageName)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SHORTCUTS, jsonArray.toString()).apply()
    }
}
