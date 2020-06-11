package balti.module.baltitoolbox.functions

import android.annotation.SuppressLint
import android.content.Context
import balti.module.baltitoolbox.ToolboxHQ

object SharedPrefs {

    private var sharedPreferences = ToolboxHQ.sharedPreferences

    @SuppressLint("ApplySharedPref")
    private fun <T> putPref(key: String, value: T, immediate: Boolean = false){
        val editor = sharedPreferences.edit()
        when (value) {
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Set<*> -> editor.putStringSet(key, value.map { it.toString() }.toHashSet())
            else -> editor.putString(key, value.toString())
        }
        if (immediate) editor.commit()
        else editor.apply()
    }

    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
    private fun <T> getPref(key: String, defaultValue: T): T {
        val result = when (defaultValue) {
            is Int -> sharedPreferences.getInt(key, defaultValue)
            is Long -> sharedPreferences.getLong(key, defaultValue)
            is Float -> sharedPreferences.getFloat(key, defaultValue)
            is Boolean -> sharedPreferences.getBoolean(key, defaultValue)
            is Set<*> -> sharedPreferences.getStringSet(key, defaultValue.map { it.toString() }.toHashSet())
            else -> sharedPreferences.getString(key, defaultValue.toString()).toString()
        }

        return result as T
    }

    fun resetSharedPrefName(name: String = ""){
        sharedPreferences =
                if (name == "") ToolboxHQ.sharedPreferences
                else ToolboxHQ.context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    fun getPrefInt(key: String, defaultValue: Int): Int = getPref(key, defaultValue)
    fun getPrefLong(key: String, defaultValue: Long): Long = getPref(key, defaultValue)
    fun getPrefFloat(key: String, defaultValue: Float): Float = getPref(key, defaultValue)
    fun getPrefBoolean(key: String, defaultValue: Boolean): Boolean = getPref(key, defaultValue)
    fun getPrefStringSet(key: String, defaultValue: Iterable<String>): Set<String> = getPref(key, defaultValue.toHashSet())
    fun getPrefString(key: String, defaultValue: String = ""): String = getPref(key, defaultValue)

    fun putPrefInt(key: String, value: Int, immediate: Boolean = false) = putPref(key, value, immediate)
    fun putPrefLong(key: String, value: Long, immediate: Boolean = false) = putPref(key, value, immediate)
    fun putPrefFloat(key: String, value: Float, immediate: Boolean = false) = putPref(key, value, immediate)
    fun putPrefBoolean(key: String, value: Boolean, immediate: Boolean = false) = putPref(key, value, immediate)
    fun putPrefStringSet(key: String, value: Iterable<String>, immediate: Boolean = false) = putPref(key, value.toHashSet(), immediate)
    fun putPrefString(key: String, value: String, immediate: Boolean = false) = putPref(key, value, immediate)
}