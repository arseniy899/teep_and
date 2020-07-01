package com.arseniy899.artem_app

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.util.Log
import kotlin.collections.Map.Entry

/**
 * Class to work with constant memory access (SharedPreferences) between app launch. For instance,
 * user settings and data cache.
 * @property context Context local link to context, requiered for SharedPreferences
 * @property sharedPref SharedPreferences link to SharedPreferences
 * @property dumpOfEntries String list all entries in friendly format
 * @constructor
 */
class MemoryWork(context: Context)
{
	var context: Context
	var sharedPref: SharedPreferences

	val dumpOfEntries: String
		get()
		{
			val keys = this.sharedPref.all
			var ret = ""
			for((key, value) in keys)
				ret += "$key=$value\n"
			return ret
		}

	init
	{
		this.context = context.applicationContext
		this.sharedPref = context.getSharedPreferences("main", Context.MODE_PRIVATE)

	}
	
	/**
	 * Store value in memory of type 'Integer'
	 * @param key String unique key of value
	 * @param value Int
	 */
	fun writeInt(key: String, value: Int)
	{
		val editor = this.sharedPref.edit()
		editor.putInt(key, value)
		editor.commit()
		Log.d("MemoryWorkerSave", "$key=$value")
	}
	
	/**
	 * Store value in memory of type 'String'
	 * @param key String unique key of value
	 * @param value String
	 */
	fun writeStr(key: String, value: String)
	{
		val editor = this.sharedPref.edit()
		editor.putString(key, value)
		editor.apply()
		Log.d("MemoryWorkerSave", "$key=$value")
	}
	
	/**
	 * Store value in memory of type 'Boolean'
	 * @param key String unique key of value
	 * @param value Boolean
	 */
	fun writeB(key: String, value: Boolean)
	{
		val editor = this.sharedPref.edit()
		editor.putBoolean(key, value)
		editor.commit()
		Log.d("MemoryWorkerSave", "$key=$value")
	}
	
	/**
	 * Load value from memory of type 'Integer'
	 * @param key String unique key of value
	 * @return Int default is '0'
	 */
	fun loadInt(key: String): Int
	{
		Log.d("MemoryWorkerLoad", key + "=" + this.sharedPref.getInt(key, 0))
		return this.sharedPref.getInt(key, 0)
	}
	
	/**
	 * Load value from memory of type 'String'
	 * @param key String unique key of value
	 * @return String default is empty string
	 */
	fun loadString(key: String): String
	{
		Log.d("MemoryWorkerLoad", key + "=" + this.sharedPref.getString(key, ""))
		return this.sharedPref.getString(key, "") ?: ""
	}
	
	/**
	 * Load value from memory of type 'Boolean'
	 * @param key String unique key of value
	 * @return Boolean default is 'false'
	 */
	fun loadB(key: String): Boolean
	{
		if(key != "debug-on")
			Log.d("MemoryWorkerLoad", key + "=" + this.sharedPref.getBoolean(key, false))
		return this.sharedPref.getBoolean(key, false)
	}
	
	/**
	 * Clears all stored values. For instance, when user logs out and all settings should be wiped.
	 */
	fun clearAll()
	{
		var editor = this.sharedPref.edit()
		editor = editor.clear()
		editor.apply()
	}
	
	/**
	 * Statically accessed equivalents. Required if work need to be done once and declaring new
	 * object instance is too heavy.
	 */
	companion object
	{

		fun writeInt(context: Context, key: String, value: Int)
		{
			val sharedPref = context.getSharedPreferences("main", Context.MODE_PRIVATE)
			val editor = sharedPref.edit()
			editor.putInt(key, value)
			editor.commit()
			if(key != "debug-on")
				Log.d("MemoryWorkerSave", "$key=$value")
		}

		fun writeStr(context: Context, key: String, value: String)
		{
			val sharedPref = context.getSharedPreferences("main", Context.MODE_PRIVATE)
			val editor = sharedPref.edit()
			editor.putString(key, value)
			editor.apply()
			if(key != "debug-on")
				Log.d("MemoryWorkerSave", "$key=$value")
		}

		fun writeB(context: Context, key: String, value: Boolean)
		{
			val sharedPref = context.getSharedPreferences("main", Context.MODE_PRIVATE)
			val editor = sharedPref.edit()
			editor.putBoolean(key, value)
			editor.commit()
			if(key != "debug-on")
				Log.d("MemoryWorkerSave", "$key=$value")
		}

		fun loadInt(context: Context, key: String): Int
		{
			val sharedPref = context.getSharedPreferences("main", Context.MODE_PRIVATE)
			if(key != "debug-on")
				Log.d("MemoryWorkerLoad", key + "=" + sharedPref.getInt(key, 0))
			return sharedPref.getInt(key, 0)
		}

		fun loadString(context: Context, key: String): String
		{
			val sharedPref = context.getSharedPreferences("main", Context.MODE_PRIVATE)
			if(key != "debug-on")
				Log.d("MemoryWorkerLoad", key + "=" + sharedPref.getString(key, ""))
			return sharedPref.getString(key, "") ?: "";
		}

		fun loadB(context: Context, key: String): Boolean
		{
			val sharedPref = context.getSharedPreferences("main", Context.MODE_PRIVATE)
			if(key != "debug-on")
				Log.d("MemoryWorkerLoad", key + "=" + sharedPref.getBoolean(key, false))
			return sharedPref.getBoolean(key, false)
		}

		fun clearCache(context: Context)
		{
			val sharedPref = context.getSharedPreferences("main", Context.MODE_PRIVATE)
			val keys = sharedPref.all
			for((key, value) in keys)
			{
				if(value.toString().length > 100)
					Log.d("ClearCache", "lentg=$key")
				if(key.startsWith("/api/") || value.toString().length > 100)
					sharedPref.edit().remove(key).apply()

			}
			sharedPref.edit().apply()
		}
	}
}
