package com.arseniy899.artem_app

import android.content.Context
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.ion.Ion
import android.app.Activity
import org.json.JSONObject
import java.nio.file.Files.size
import android.os.Looper
import com.koushikdutta.ion.future.ResponseFuture
import com.koushikdutta.ion.ProgressCallback
import android.widget.RelativeLayout
import android.view.ViewGroup
import android.R
import android.R.attr.logo
import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import com.google.android.material.snackbar.Snackbar
import android.os.Build
import com.koushikdutta.ion.builder.Builders
import android.content.pm.PackageManager
import android.graphics.Color
import android.widget.FrameLayout
import android.net.ConnectivityManager
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.koushikdutta.async.future.FailCallback
import com.koushikdutta.async.kotlin.await
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Interface of callback methods for usage with Web requests (API)
 */
interface WebCallBackInterface
{
	/**
	 * Callback method being called when an error accours
	 * @param code Int
	 * 		-3 : No internet connection<br></br>
	 * 		-1 : Too much request are proceeding already (max. amount is taken numberFrom MAX_WEB_REQUESTS[RemConf] )<br></br>
	 * 		-6 : There is already a request with the same parameters<br></br>
	 * 		4  : Login error (session expired)
	 * @param error String
	 * 		Message with description from server
	 */
	fun onError(code: Int, error: String)
	{
	}
	
	/**
	 * Callback method being called when the request is completed (with any result)
	 */
	fun onCompleted()
	{
	}
	
	/**
	 * Callback method being called right after the request was sent to network
	 */
	fun onStarted()
	{
	}
	
	/**
	 * Callback method being called when the successful result was received
	 * @param result JsonObject Parsed JSON from result string
	 */
	fun onSuccess(result: JsonObject)
	{
	}
	
	/**
	 * Callback method to show error message to user in UI. Can be overiden for hiding or another UI
	 * display.
	 * @param view View Parent view to show SnackBar
	 * @param error String Descriptive message being shown to the user
	 * @param clickListener OnClickListener? Callback method for action button. Typically - retry
	 */
	fun errorShow(view: View, error: String, clickListener: View.OnClickListener?)
	{
		Snackbar.make(view, error, Snackbar.LENGTH_LONG).setAction("Заново", clickListener)
			.setActionTextColor(Color.rgb(255, 0, 0)).show()
	}
	
	
}
/**
 * Static class for proceeding Web-requests to API server
 * */
@SuppressLint("StaticFieldLeak")
object Web {
	/** Array for storing list of requests' URI  */
	var reportedInvalidJson = ""
	var requests: ArrayList<String> = ArrayList();
	/** Array for storing list of requests' URI called with activity context  */
	var requestsUI:ArrayList<String> = ArrayList();
	/** Variable for storing progress bar view */
	var progressBar: View? = null
	/**
	 * Rootview of activity if given context is activity
	 */
	var rootView: FrameLayout? = null;
	/**
	 * View of request proceeding snackbar
	 */
	var snackbar: Snackbar? = null
	private var lastReqID = 0
	/**
	 * Perform request to web-server without storing result in memory
	 * @param context If context is activity, loading bar and request proceeding snackbar with text 'Запрос обрабатывается' will be shown
	 * @param page Link to page to proceed with. E.g. /rs/knock-knock.rs.php
	 * @param map Map of request body (POST data) in format key, data
	 * @param callback Resulting callback for actions after request
	 */
	fun request(
		context: Context,
		page: String,
		map: Map<String, String>,
		callback: WebCallBackInterface
	)
	{
		request(context, page, map, callback, "Запрос обрабатывается")
	}
	
	/**
	 * Perform request to web-server and storer result in memory
	 * @param context If context is activity, loading bar and request proceeding snackbar with specified text will be shown
	 * @param page Link to page to proceed with. E.g. /rs/knock-knock.rs.php
	 * @param map Map of request body (POST data) in format key, data
	 * @param callback Resulting callback for actions after request
	 * @param text Text to be shown on request proceeding snackbar
	 */
	fun requestMem(
		context: Context,
		page: String,
		map: Map<String, String>,
		callback: WebCallBackInterface?,
		text: String
	)
	{
		val memoryWorker = MemoryWork(context)
		val gson = Gson()
		if(callback != null)
		{
			val arr = gson.fromJson<JsonObject>(
				memoryWorker.loadString(getRequestUri(map, page)),
				object : TypeToken<JsonObject>()
				{
				
				}.type
			)
			if(arr != null)
				callback.onSuccess(arr)
		}
		request(context, page, map, callback, text)
	}
	
	/**
	 * Get URI (id-name) of request to determine it for storing in memory
	 * @param map list of request parameters
	 * @param page Link to page to proceed with. E.g. /rs/knock-knock.rs.php
	 * @return String with specific name unique for this requests
	 */
	fun getRequestUri(map: Map<String, String>?, page: String): String
	{
		var line = "$page,"
		if(map != null)
		{
			val keyValues = "id mode page accid fileid folder name query"
			val keyVal =
				keyValues.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			for((key, value) in map)
			{
				if(keyValues.contains(key.toLowerCase()))
					line += "$key=$value,"
				else
				{
					for(st in keyVal)
						if(key.toLowerCase().contains(st))
							line += "$key=$value,"
				}
			}
		}
		return line
	}
	
	fun getWebAddr(context: Context): String
	{
		
		//return "http://r-ho.ml:81/api"
		return Static.remoteConfig.getString("api_url");
	}
	
	/**
	 * Determine if device has network connection (E.g. over WiFi or GSM)
	 * @param context
	 * @return true if has
	 */
	fun isNetworkConnected(context: Context): Boolean
	{
		val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
		
		return cm.activeNetworkInfo != null
	}
	/**
	 * Gett main (the most parent, root) view of specified activity
	 * @param activity
	 * @return
	 */
	fun getRootView(activity : Activity) : View
	{
		return activity.getWindow ().getDecorView ().findViewById (android.R.id.content);
	}
	/**
	 * Perform request to web-server without storing result in memory. Error codes called with onError:<br></br>
	 * -3 : No internet connection<br></br>
	 * -1 : Too much request are proceeding already (max. amount is taken numberFrom MAX_WEB_REQUESTS[FirebaseRemoteConfig] )<br></br>
	 * -6 : There is already a request with the same parameters<br></br>
	 * 4  : Login error (session expired)=
	 * @param context If context is activity, loading bar and request proceeding snackbar with specified text will be shown
	 * @param page Link to page to proceed with. E.g. /rs/knock-knock.rs.php
	 * @param map Map of request body (POST data) in format key, data
	 * @param callback Resulting callback for actions after request
	 * @param text Text to be shown on request proceeding snackbar
	 */
	fun request(
		context: Context?,
		page: String,
		map: Map<String, String>?,
		callback: WebCallBackInterface?,
		text: String
	)
	{
		
		val curReqId = lastReqID
		lastReqID++
		if(context is Activity)
			rootView = getRootView(context as Activity) as FrameLayout
		if(!isNetworkConnected(context!!))
		{
			if(callback != null)
			{
				callback.onError(-3, "Отсутствует интернет-подключение на устройстве")
				if(context is Activity)
					rootView?.let {
						callback.errorShow(
							it,
							"Отсутствует интернет-подключение на устройстве",
							null
						)
					}
				callback.onCompleted()
			}
			return
		}
		val uri = getRequestUri(map, page)
		if(requests.size > 0 && requests.contains(uri))
		{
			Log.i("WEB/RequestsCount", "Too much same requests $requests")
			if(callback != null)
			{
				callback.onError(-6, "Слишком много идентичных запросов")
				callback.onCompleted()
			}
			return
		}
		requests.add(uri)
		//        curConnections++;
		if(callback != null)
			callback.onStarted()
		val url: String = if (!page.contains("http")) getWebAddr(context) + page else page
		//		Ion.getDefault (context.getApplicationContext ()).configure ().userAgent ("cloff_app_android");
		var version = ""
		try
		{
			version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
		} catch(e: PackageManager.NameNotFoundException)
		{
			version = "undefined"
			e.printStackTrace()
		}
		
		var isReqUI = false
		val load = Ion.with(context.applicationContext).load(url)
			.userAgent("android-app/v." + version + ";and=" + System.getProperty("os.version") + ";m=" + Build.MODEL + "/" + Build.BRAND)
		if(context is Activity && progressBar == null && /*rootView.findViewById (R.id.swipeRefresh) == null &&*/ Looper.getMainLooper().thread === Thread.currentThread())
		{
			//            curConnectionsUI++;
			//            if(!uri.contains ("/rs/login.rs.php"))
			requestsUI.add(uri)
			snackbar = rootView?.let {
				Snackbar.make(it, text, Snackbar.LENGTH_SHORT)
					.setAction("Загрузка", null)
			}
			Log.d("WEB/REQUESTS_UI", "requestsUI.size ()=" + requestsUI.size)
			val inflater =
				context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
			isReqUI = true
			
			Log.d("WEB/Started", "requestsUI.size ()=" + requestsUI.size)
			if(text.length > 0)
			{
				snackbar?.show()
			}
			
		}
		if(map != null)
			for((key, value) in map)
				load.setBodyParameter(key, value)
		
		
		var mapJs: JSONObject? = null
		if(map != null)
			mapJs = JSONObject(map)
		if(isReqUI)
			Log.i("WEB/REQUEST_UI[$curReqId]:$url :", mapJs!!.toString() + "")
		else
		Log.i("WEB/REQUEST_UI[$curReqId]:$url :", mapJs!!.toString() + "")
		var th = load
			.asString();
		var clickListener: View.OnClickListener = object : View.OnClickListener
		{
			override fun onClick(v: View)
			{
				snackbar?.show()
//				fut.setCallback(th)
				rootView?.removeView(v)
				request(context, url, map, callback, text)
			}
		}
		
//			var result = th.await();
		var responce = th.withResponse();
		responce.done { e, responce ->
			
			var result = responce.result
			if(callback != null)
				callback.onCompleted()
			requests.remove(uri)
			snackbar?.dismiss()
			
			if(context is Activity && Looper.getMainLooper().thread === Thread.currentThread())
			{
				synchronized(requestsUI) {
					requestsUI.remove(uri)
				}
				//
				if(progressBar != null && requestsUI.size <= 0)
				{
					//                        progressBar.setIndeterminate (false);
					progressBar?.setVisibility(View.GONE)
					rootView?.removeView(progressBar)
					progressBar = null
					Log.d(
						"WEB/RESULT_UI",
						"requestsUI.size ()=" + requestsUI.size
					)
				}
			}
			if(e != null)
			{
				Log.e("WEB[$curReqId]:$url/Error", e.toString())
				
				if(callback != null)
				{
					
					if(e.javaClass == TimeoutException::class.java)
					{
						callback.onError(-2, "Ошибка подключения к серверу")
						if(context is Activity)
							rootView?.let {
								callback.errorShow(
									it,
									"Ошибка подключения к серверу",
									clickListener
								)
							}
					}
					else if(e.javaClass == JsonParseException::class.java)
					{
						callback.onError(-4, "Ошибка получения данных с сервера")
						//							Log.e ("WEB:"+ConstStat.DOMEN+""+url+"/Error",r);
						if(context is Activity)
							rootView?.let {
								callback.errorShow(
									it,
									"Ошибка получения данных с сервера",
									clickListener
								)
							}
					}
					else
					{
						callback.onError(-5, "Ошибка взаимодействия с сервером")
						if(context is Activity)
							rootView?.let {
								callback.errorShow(
									it,
									"Ошибка взаимодействия с сервером",
									clickListener
								)
							}
					}
					
				}
			}
			else
			{
				Log.i("WEB/RESULT[$curReqId]:$url :", result + "")
				val gson = Gson()
				if(result.contains("{") && result.contains("\"") && result.contains("}"))
				{
					result = result.replace(" (\r|\n|\t)".toRegex(), "")
				}
				result = result.replace(",}", "}")
				result = result.replace(",]", "]")
				try
				{
					val respObj = gson.fromJson(result, JsonObject::class.java).getAsJsonObject("responce")
					
					if(respObj.get("error").asInt != 0)
					{
						
						if(callback != null)
						{
							callback.onError(
								respObj.get("error").asInt,
								respObj.get("desc").asString
							)
							if(context is Activity)
								rootView?.let {
									callback.errorShow(
										it,
										respObj.get("desc").asString,
										clickListener
									)
								}
						}
						when(respObj.get("error").asInt)
						{
							1000 -> {
								var intent = Intent(context,MainActivity::class.java)
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								context.startActivity(intent)
								MemoryWork.writeStr(context,"user.login","")
							}
						}
					}
					else if(callback != null)
					{
						val memoryWork = MemoryWork(context)
						memoryWork.writeStr(uri, respObj.toString())
						//                        if(context instanceof Activity)
						try
						{
							callback.onSuccess(respObj.asJsonObject)
						} catch(ex: NullPointerException)
						{
							if(!url.contains("login"))
							{
								Log.e("WEB/RESULT/PARSE", "ex: $ex")
								ex.printStackTrace()
								
							}
						}
						
					}
				} catch(ex: NullPointerException)
				{
					if(reportedInvalidJson.length == 0)
						reportedInvalidJson =
							MemoryWork.loadString(context, "reportedInvalidJson")
					Log.e("WEB/RESULT/PARSE", "ex: $ex")
					ex.printStackTrace()
					
				} catch(ex: JsonParseException)
				{
					if(reportedInvalidJson.length == 0)
						reportedInvalidJson =
							MemoryWork.loadString(context, "reportedInvalidJson")
					if(!reportedInvalidJson.contains("$uri;") && !url.contains("login.rs"))
					{
						Log.d("WEB/RESULT/PARSE/Not-JS", "result: $result")
						Log.e("WEB/RESULT/PARSE/", "ex: $ex")
						val obj: JSONObject
						if(map != null)
							obj = JSONObject(map)
						else
							obj = JSONObject()
						ex.printStackTrace()
						
						reportedInvalidJson += "$uri;"
						MemoryWork.writeStr(context, "reportedInvalidJson", reportedInvalidJson)
					}
					
				}
				
			}
		}
		
		
		
	}
}

/**
 * Static class for globally used fields
 */
object Static
{
	/**
	 * Instance of Firebase Remote Config
	 */
	val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
	/**
	 *
	 */
	var points = 0;
	var userName = "";
	var userLogin = "";
	var userMail = "";
	var institutes = HashMap<Int, String>();
	var instID = 0;
	var avatarID = "";
	var instName = "";
}

/**
 * Static class of tools methods (can be used from any place of project with no extra dependecnies)
 */
object Tools
{
	var monsShort = arrayOf(
			"",
			"янв",
			"фев",
			"мар",
			"апр",
			"мая",
			"июня",
			"июля",
			"авг",
			"сен",
			"окт",
			"нояб",
			"дек"
	)
	/**
	 * Array for storing names of months (detailed)
	 */
	var monsLong = arrayOf(
			"",
			"января",
			"февраля",
			"марта",
			"апреля",
			"мая",
			"июня",
			"июля",
			"августа",
			"сентября",
			"октября",
			"ноября",
			"декабря"
	)
	
	/**
	 * Method for formating date and time string to user-friendly format
	 * @param dateTimeIn String
	 *    Acceptable formats: 	dd.MM.YYYY, dd.MM.YYYY HH:mm:ss, YYYY.MM.dd, YYYY.MM.dd HH:mm:ss,
	 *    						dd-MM-YYYY, dd-MM-YYYY HH:mm:ss, YYYY-MM-dd, YYYY-MM-dd HH:mm:ss,
	 *    						HH:mm:ss,dd.MM.YYYY, dd.MM.YYYY HH:mm, YYYY.MM.dd, YYYY.MM.dd HH:mm,
	 *    						dd-MM-YYYY, dd-MM-YYYY HH:mm, YYYY-MM-dd, YYYY-MM-dd HH:mm,  HH:mm
	 * @return String User friendly string:
	 * 			If date is today, result is HH:mm
	 * 			If date is in current year, result is dd.<month short name>
	 * 			Else: dd.<month short name>.YYYY
	 */
	fun formatDateTime(dateTimeIn: String): String
	{
		//19.08.2016 13:13:10
		//1.0.201 1:3
		var dateTime = dateTimeIn.replace("-".toRegex(), ".")
		if(dateTime.contains(" ") && !dateTime.contains(".") && !dateTime.contains(":"))
		{
			return dateTime
		}
		var isTime = false
		var isDate = false
		if(dateTime.contains(":"))
		{
			isTime = true
		}
		if(dateTime.contains("."))
		{
			isDate = true
		}
		val c = Calendar.getInstance()
		val splitted = dateTime.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		var date: Array<String>? = null
		var time: Array<String>? = null
		if(isDate && isTime)
		{
			if(splitted[0].contains("."))
			{
				date =
					splitted[0].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				time =
					splitted[1].split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			}
			else
			{
				date =
					splitted[1].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				time =
					splitted[0].split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			}
		}
		else if(isDate)
		{
			date = dateTime.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		}
		else if(isTime)
		{
			time = dateTime.split("\\:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		}
		var day = 0
		var mon = 0
		var year = 0
		var deltaDay = 0
		var deltaMon = 0
		var deltaYear = 0
		if(isDate)
		{
			if(date!![0].length == 2)
			{
				day = Integer.parseInt(date[0])
				year = Integer.parseInt(date[2])
			}
			else
			{
				
				day = Integer.parseInt(date[2])
				year = Integer.parseInt(date[0])
			}
			deltaDay = c.get(Calendar.DAY_OF_MONTH) - day
			mon = Integer.parseInt(date[1])
			deltaMon = c.get(Calendar.MONTH) + 1 - mon
			
			deltaYear = c.get(Calendar.YEAR) - year
			
		}
		
		
		var h = 0
		var m = 0
		var s = -1
		if(isTime)
		{
			h = Integer.parseInt(time!![0])
			m = Integer.parseInt(time[1])
			if(time.size == 3)
			{
				s = Integer.parseInt(time[2])
			}
		}
		if(deltaDay < 1 && isTime && deltaMon == 0 && deltaYear == 0)
		{
			return String.format("%02d:%02d",h,m)
		}
		return if(deltaMon <= 12 && deltaYear == 0 && isDate)
		{
			(if(day >= 10) "" else "0") + day + " " + monsShort[mon]
		}
		else (if(day >= 10) "" else "0") + day + " " + monsShort[mon] + " " + year
	}
	
	/**
	 * Method to load settings from server corresponding to user and from remote config
	 * @param context Context
	 * @param callback Runnable Callback of successfull settings being received
	 */
	fun loadSettings(context: Context, callback : Runnable)
	{
		Static.remoteConfig.setDefaults(com.arseniy899.artem_app.R.xml.remote_config_defaults);
		var map = HashMap<String, String>();
		Web.requestMem(context.applicationContext, if(Static.userLogin.isNotBlank()) "/settings.get.php" else "/settings.get.public.php", map, object : WebCallBackInterface
		{
			override fun onSuccess(result: JsonObject)
			{
				var data = result.getAsJsonObject("data");
				var inst = data.getAsJsonArray("inst");
				for(el : JsonElement in inst)
				{
					val obj = el.asJsonObject;
					Static.institutes.set(obj.get("id").asInt,obj.get("name").asString);
					callback.run();
				}
			}
		},"")
		val configSettings = FirebaseRemoteConfigSettings.Builder()
			.setDeveloperModeEnabled(true)
			.build()
		Static.remoteConfig.setConfigSettings(configSettings);
		Static.remoteConfig.fetch()
			.addOnCompleteListener() { task ->
				if (task.isSuccessful) {
					val updated = task.result
					Log.i("RemoteConfig", "Config params updated: $updated")
					Static.remoteConfig.activateFetched();
				} else {
					Log.e("RemoteConfig", "Failed to fetch config")
					
				}
			}
	}
}