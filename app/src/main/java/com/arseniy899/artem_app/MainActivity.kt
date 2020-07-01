package com.arseniy899.artem_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import com.google.gson.JsonObject
import com.koushikdutta.ion.Ion
import kotlinx.android.synthetic.main.activity_main.*
import com.google.firebase.analytics.FirebaseAnalytics

class MainActivity : AppCompatActivity()
{
	var instName = "";
	var instID = 0;
	var institutesList =  Static.institutes.values.toList();
	private var mFirebaseAnalytics: FirebaseAnalytics? = null;
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main)
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
		Ion.getDefault(this).getConscryptMiddleware().enable(false);
		val bundle = Bundle()
		bundle.putString(FirebaseAnalytics.Param.METHOD, "")
		mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle)
		if(MemoryWork.loadString(this,"user.login").isNotEmpty())
		{
			openAccountActivity()
		}
		else
		{
			createLoginForm();
		}
		Tools.loadSettings(this, Runnable {
			institutesList = Static.institutes.values.toList()
			setSpinnerItems();
		});
	}
	
	private fun openAccountActivity()
	{
		var intent = Intent(this,ProfileActivity::class.java)
		startActivity(intent)
		finish()
	}
	
	fun createLoginForm()
	{
		loginForm.visibility = View.VISIBLE;
		registerForm.visibility = View.GONE;
		loginBtn.setOnClickListener { view ->
			var login = loginInput!!;
			var pwd = passwordInput!!;
			if(login.text!!.isEmpty())
			{
				login.error = getString(R.string.err_input_empty);
				return@setOnClickListener;
			}
			else
				login.error = null;
			if(pwd.text!!.isEmpty())
			{
				pwd.error = getString(R.string.err_input_empty);
				return@setOnClickListener;
			}
			else
				pwd.error = null;
			var map = HashMap<String, String>();
			map.put("login",login.text.toString())
			map.put("passw",pwd.text.toString())
			Web.request(this,"/user.auth.login.php",map,object : WebCallBackInterface
			{
				override fun onSuccess(result: JsonObject)
				{
					val bundle = Bundle()
					bundle.putString(FirebaseAnalytics.Param.METHOD, "")
					mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
					MemoryWork.writeStr(baseContext,"user.login",login.text.toString());
					MemoryWork.writeStr(baseContext,"user.passw",pwd.text.toString());
					openAccountActivity();
				}
			})
		}
		registerBtn.setOnClickListener { view ->
			createRegistForm();
		}
	}
	
	fun createRegistForm()
	{
		val bundle = Bundle()
		bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "register_form_open");
		mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
		
		institutesList = ArrayList(Static.institutes.values);
		loginForm.visibility = View.GONE;
		registerForm.visibility = View.VISIBLE;
		registerFormBtn.setOnClickListener { view ->
			//1. Login
			var login: TextView = findViewById(R.id.regLogin);
			if(login.text.isEmpty())
			{
				login.error = getString(R.string.err_input_empty);
				return@setOnClickListener;
			}
			else
				login.error = null;
			
			//2. Name
			if(name1Input.text!!.isEmpty())
			{
				name1Input.error = getString(R.string.err_input_empty);
				return@setOnClickListener;
			}
			else
				name1Input.error = null;
			
			if(name2Input.text!!.isEmpty())
			{
				name2Input.error = getString(R.string.err_input_empty);
				return@setOnClickListener;
			}
			else
				name2Input.error = null;
			
			if(name3Input.text!!.isEmpty())
			{
				name3Input.error = getString(R.string.err_input_empty);
				return@setOnClickListener;
			}
			else
				name3Input.error = null;
			
			
			//3. Name
			var mail: TextView = findViewById(R.id.regMail);
			if(mail.text.isEmpty())
			{
				mail.error = getString(R.string.err_input_empty);
				return@setOnClickListener;
			}
			else
				mail.error = null;
			
			
			//4. Passwords
			var pwd1: TextView = findViewById(R.id.regPwd1);
			var pwd2: TextView = findViewById(R.id.regPwd2);
			if(pwd1.text.isEmpty())
			{
				pwd1.error = getString(R.string.err_input_empty);
				return@setOnClickListener;
			}
			else if(pwd2.text.isEmpty())
			{
				pwd2.error = getString(R.string.err_input_empty);
				return@setOnClickListener;
			}
			else if(!pwd1.text.toString().equals(pwd2.text.toString()))
			{
				pwd1.error = getString(R.string.err_input_pwd_mismatch);
				return@setOnClickListener;
			}
			else
			{
				pwd1.error = null;
				pwd2.error = null;
			}
			
			val nameCombined = String.format(
					"%s %s %s",
					name1Input.text.toString(),
					name2Input.text.toString(),
					name3Input.text.toString());
			
			var map = HashMap<String, String>();
			map.put("login",login.text.toString())
			map.put("name",nameCombined)
			map.put("mail",mail.text.toString())
			map.put("passw",pwd1.text.toString())
			map.put("repass",pwd2.text.toString())
			map.put("instID", instID.toString())
			Web.request(this,"/user.auth.create.php",map,object : WebCallBackInterface
			{
				override fun onSuccess(result: JsonObject)
				{
					val bundle = Bundle()
					bundle.putString(FirebaseAnalytics.Param.METHOD, "")
					mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
					
					MemoryWork.writeStr(baseContext,"user.login",login.text.toString());
					MemoryWork.writeStr(baseContext,"user.passw",pwd1.text.toString());
					openAccountActivity();
				}
			})
		}
		closeRegForm.setOnClickListener { view ->
			createLoginForm();
		}
		
		setSpinnerItems();
		
		instInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
		{
			override fun onNothingSelected(parent: AdapterView<*>?)
			{
			
			}
			
			override fun onItemSelected(
					parent: AdapterView<*>?,
					view: View?,
					position: Int,
					id: Long
			)
			{
				var name = institutesList.get(position);
				for(st in Static.institutes)
				{
					if(st.value.equals(name))
					{
						instID = st.key;
						instName = name;
						break;
					}
					
				}
			}
			
		}
		
	}
	
	private fun setSpinnerItems()
	{
		val adapter =
			ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, institutesList.toList())
		
		instInput.adapter = adapter
		var i = 0;
		for(st: String in institutesList)
		{
			if(st == instName)
			{
				instInput.setSelection(i)
				break;
			}
			i++;
		}
	}
}
