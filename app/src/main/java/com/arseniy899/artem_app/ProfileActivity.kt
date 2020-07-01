package com.arseniy899.artem_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.appcompat.app.AppCompatActivity
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.koushikdutta.ion.Ion
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.item_event_history.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ProfileActivity : AppCompatActivity()
{
	
	var adapter : HistoryListAdapter? = null;
	private var mFirebaseAnalytics: FirebaseAnalytics? = null;
	/**
	 *
	 * @param savedInstanceState Bundle?
	 */
	override fun onCreate(savedInstanceState: Bundle?)
	{
		val intent = intent
		val data = intent.data
		if(data != null && data.isHierarchical)
		{
			val uri = this.intent.dataString ?: "";
			if(uri.isNotEmpty())
			{
				Log.i("MyApp", "Deep link clicked " + uri)
				// http://r-ho.ml:81/event.roles.acquire.php?lk=50b9b3630e020e78d31716361575586f
				var map = HashMap<String, String>();
				map.put("lk", uri.substring(uri.indexOf("=")+1));
				Web.request(this, "/event.roles.acquire.api.php", map, object : WebCallBackInterface
				{
					override fun onSuccess(result: JsonObject)
					{
						var data = result.getAsJsonObject("data");
						var name = data.get("eventName").asString
						var roleName = data.get("roleName").asString
						
						MaterialStyledDialog.Builder(this@ProfileActivity)
							.setTitle("Приглашение принято!")
							.setHeaderDrawable(R.drawable.header)
							.setDescription("Ваше приглашение было обработано и вам назначена роль '$name' на событии '$roleName'")
							.setPositiveText("Понятно")
							.show()
					}
				})
			}
		}
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_profile)
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
		fab.setOnClickListener { view ->
			val bundle = Bundle()
			bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "scan_qr_press");
			mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
			
			startQRscanner();
		}
//		if(Static.institutes.isEmpty())
		Tools.loadSettings(this, Runnable {});
		adapter = HistoryListAdapter(this);
		/*var map = HashMap<String, String>();
		Web.request(this,"/user.points.get.php",map,object : WebCallBackInterface
		{
			override fun onSuccess(result: JsonObject)
			{
				points = result.getAsJsonObject("data").get("points").asInt;
				//drawPoints();
			}
		})*/
		historyList.adapter = adapter
//		if(Static.userName.isEmpty())
		getUserInfo()
		eventsLay.setOnClickListener {view ->
			historyLayout.visibility = View.VISIBLE
			statLayout.visibility = View.GONE
			
		}
		closeHistory.setOnClickListener {view ->
			historyLayout.visibility = View.GONE
			statLayout.visibility = View.VISIBLE
			
		}
		editProfile.setOnClickListener {view ->
			var intent = Intent(this,ProfileEditActivity::class.java)
			startActivityForResult(intent, 105)
		}
	}
	
	fun getUserInfo()
	{
		var map = HashMap<String, String>();
		Web.request(this, "/user.info.get.php", map, object : WebCallBackInterface
		{
			override fun onSuccess(result: JsonObject)
			{
				var data = result.getAsJsonObject("data");
				var history = data.getAsJsonArray("events");
				Static.userName = data.get("name").asString;
				Static.userLogin = data.get("login").asString;
				Static.userMail = data.get("email").asString;
				Static.instID = data.get("instID").asInt;
				Static.instName = Static.institutes.get(Static.instID).toString();
				name.text = Static.userName;
				instName.text = Static.instName;
				adapter?.jsToObj(history);
				counterEvents.text = history.size().toString();
				Static.avatarID = data.get("avatarID").asString;
				//drawPoints();
				Ion
					.with(baseContext)
//					.load(Web.getWebAddr(baseContext)+"/avatar.get.php?q=1")
					.load(Web.getWebAddr(baseContext)+"/avatar.get.php?avatarID="+Static.avatarID)
					.withBitmap()
					.intoImageView(avatar);
				
				mFirebaseAnalytics?.setUserProperty("inst", Static.instID.toString())
				mFirebaseAnalytics?.setUserProperty("login", Static.userLogin)
				mFirebaseAnalytics?.setUserProperty("eventCount", counterEvents.text.toString())
			}
		})
		
	}
	
	fun startQRscanner()
	{
		var intent = Intent(this,ScanQrCodeActivity::class.java);
		startActivityForResult(intent, 150);
	}
	fun drawPoints()
	{
		pointsTV.text = "Ваш счет: ${Static.points}"
	}
	
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
	{
		if(resultCode == Activity.RESULT_OK && requestCode == 150)
		{
			var hash = data?.getStringExtra("hash");
			Snackbar.make(historyList,"Scanned: "+hash,1500).show();
			var map = HashMap<String, String>();
			hash?.let { map.put("hash", it) }
			Web.request(this,"/event.checkin.php",map,object : WebCallBackInterface
			{
				override fun onSuccess(result: JsonObject)
				{
					var data = result.getAsJsonObject("data");
					var name = data.get("name").asString
					//points += data.get("points").asInt;
					val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					val currentDate = sdf.format(Date())
					adapter?.addItem(Event(	data.get("id").asInt,
										   	currentDate,
										  	data.get("name").asString,
										  	data.get("role").asInt,
										  	data.get("roleName").asString))
					counterEvents.text = adapter?.count.toString();
					drawPoints();
					MaterialStyledDialog.Builder(this@ProfileActivity)
						.setTitle("Замётано!")
						.setHeaderDrawable(R.drawable.header)
						.setDescription("Вы успешно отметились на событии ${name}")
						.setPositiveText("Понятно")
						.show()
				}
			})
		}
		else if(resultCode == Activity.RESULT_OK && requestCode == 105)
		{
			name.text = Static.userName;
			instName.text = Static.instName;
			Ion
				.with(baseContext)
				.load(Web.getWebAddr(baseContext)+"/avatar.get.php?avatarID="+Static.avatarID)
				.withBitmap()
				.intoImageView(avatar);
		}
		super.onActivityResult(requestCode, resultCode, data)
	}
	class Event
	{
		var id = 0;
		var dtStr = "";
		var name = "";
		var roleID = 0;
		var roleName = "";
		
		constructor(id: Int, dtStr: String, name: String, roleID: Int, roleName: String)
		{
			this.id = id
			this.dtStr = Tools.formatDateTime(dtStr);
			this.name = name
			this.roleID = roleID
			this.roleName = roleName
		}
		
		constructor(dtStr: String, name: String, roleID: Int, roleName: String)
		{
			this.dtStr = Tools.formatDateTime(dtStr);
			this.name = name
			this.roleID = roleID
			this.roleName = roleName
		}
		
	}
	class HistoryListAdapter(val context : Context) : BaseAdapter()
	{
		private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		var items = ArrayList<Event>();
		fun jsToObj(array : JsonArray)
		{
			items.clear();
			for(el : JsonElement in array)
			{
				val obj = el.asJsonObject;
				items.add(Event(obj.get("date").asString,obj.get("eventName").asString,obj.get("role").asInt,obj.get("roleName").asString))
			}
			notifyDataSetChanged()
		}
		fun addItem(item : Event)
		{
			items.add(0,item);
			notifyDataSetChanged();
		}
		override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View
		{
			val rowView = inflater.inflate(R.layout.item_event_history, parent, false)
			val event = getItem(position);
			rowView.title.text = event.name;
			rowView.date.text = event.dtStr;
			rowView.role.text = event.roleName;
			return rowView
		}
		
		override fun getItem(position: Int): Event
		{
			return items.get(position);
		}
		
		override fun getItemId(position: Int): Long
		{
			return items.get(position).id.toLong();
		}
		
		override fun getCount(): Int
		{
			return items.size;
		}
	}
}
