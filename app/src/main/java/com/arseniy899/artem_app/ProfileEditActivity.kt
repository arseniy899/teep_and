package com.arseniy899.artem_app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonObject
import com.koushikdutta.ion.Ion
import kotlinx.android.synthetic.main.activity_profile_edit.*
import java.io.File


class ProfileEditActivity : AppCompatActivity()
{
	var instName = "";
	var instID = 0;
	var institutesList = ArrayList(Static.institutes.values);
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_profile_edit)
		
		Ion
			.with(baseContext)
			//.load(Web.getWebAddr(baseContext) + "/avatar.get.php")
			.load(Web.getWebAddr(baseContext)+"/avatar.get.php?avatarID="+Static.avatarID)
			.withBitmap()
			.intoImageView(avatar);
		loginInput.setText(Static.userLogin);
		mailInput.setText(Static.userMail);
		var nameParts = Static.userName.split(" ");
		name1Input.setText(nameParts[0])
		if(nameParts.size > 1)
			name2Input.setText(nameParts[1])
		if(nameParts.size > 2)
			name3Input.setText(nameParts[2])
		back.setOnClickListener { view ->
			finish()
		}
		avatar.setOnClickListener { view ->
			ImagePicker.with(this)
				.crop(1f, 1f)               //Crop Square image(Optional)
				.compress(1024)         //Final image size will be less than 1 MB(Optional)
				.maxResultSize(
						1080,
						1080
				)  //Final image resolution will be less than 1080 x 1080(Optional)
				.start { resultCode, data ->
					if(resultCode == Activity.RESULT_OK)
					{
						//Image Uri will not be null for RESULT_OK
						val fileUri = data?.data
						
						//You can get File object from intent
						val file: File? = ImagePicker.getFile(data);
						avatar.setImageURI(fileUri);
						Ion
							.with(baseContext)
							.load(Web.getWebAddr(baseContext) + "/avatar.upload.php")
							.setMultipartFile("avatar", file)
							.asJsonObject()
							.withResponse()
							.fail { e ->
								Unit
								e.printStackTrace()
								Log.e("Profile/Avatar/Set", "RESP EXC:" + e.stackTrace);
								Toast.makeText(baseContext, "Ошибка загрузки", Toast.LENGTH_SHORT)
									.show();
							}
							.success { it ->
								Log.i("Profile/Avatar/Set", "RESP:" + it.result);
								Toast.makeText(
										baseContext,
										getString(R.string.saved),
										Toast.LENGTH_SHORT
								).show();
								val respObj = it.result.getAsJsonObject("responce")
								
								if(respObj.get("error").asInt != 0)
								{
									Snackbar.make(view, respObj.get("desc").asString, Snackbar.LENGTH_LONG)
										.setActionTextColor(Color.rgb(255, 0, 0)).show()
									
									when(respObj.get("error").asInt)
									{
									
									}
								}
								else
									Static.avatarID = respObj.getAsJsonObject("data").get("avatarID").asString;
								setResult(Activity.RESULT_OK);
							}
						//You can also get File Path from intent
//						val filePath:String = ImagePicker.getFilePath(data)
					}
					else if(resultCode == ImagePicker.RESULT_ERROR)
					{
						Toast.makeText(baseContext, ImagePicker.getError(data), Toast.LENGTH_SHORT)
							.show()
					}
				}
		}
		saveBtn.setOnClickListener { view ->
			
			if(mailInput.text!!.isEmpty())
			{
				mailInput.error = getString(R.string.err_input_empty);
				return@setOnClickListener;
			}
			else
				mailInput.error = null;
			
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
			
			
			var map = HashMap<String, String>();
			map.put("mail", mailInput.text.toString())
			map.put("instID", instID.toString())
			val nameCombined = String.format(
					"%s %s %s",
					name1Input.text.toString(),
					name2Input.text.toString(),
					name3Input.text.toString());
			map.put("name", nameCombined);
			
			Web.request(this, "/user.info.set.php", map, object : WebCallBackInterface
			{
				override fun onSuccess(result: JsonObject)
				{
					Toast.makeText(baseContext, getString(R.string.saved), Toast.LENGTH_LONG).show();
					Static.userName = nameCombined;
					Static.userMail = mailInput.text.toString();
					Static.instName = instName;
					Static.instID = instID;
					setResult(Activity.RESULT_OK);
					finish();
				}
			})
		}
		
		showPwdChange.setOnClickListener { v ->
			registerForm.visibility = View.GONE;
			pwdChangeForm.visibility = View.VISIBLE;
			
		}
		closePwd.setOnClickListener { v ->
			registerForm.visibility = View.VISIBLE;
			pwdChangeForm.visibility = View.GONE;
			
		}
		saveBtn2.setOnClickListener { view ->
			
			/*if(pwdNowInput.text!!.isEmpty())
			{
				pwdNowInput.error = getString(R.string.err_input_empty);
				return@setOnClickListener;
			}
			else
				pwdNowInput.error = null;
			*/
			if(pwdNew1Input.text!!.isEmpty())
			{
				pwdNew1Input.error = getString(R.string.err_input_empty);
				return@setOnClickListener;
			}
			else
				pwdNew1Input.error = null;
			
			if(pwdNew2Input.text!!.isEmpty())
			{
				pwdNew2Input.error = getString(R.string.err_input_empty);
				return@setOnClickListener;
			}
			else
				pwdNew2Input.error = null;
			
			if(pwdNew2Input.text!!.trim().toString() != pwdNew1Input.text!!.trim().toString())
			{
				pwdNew2Input.error = getString(R.string.err_input_pwd_mismatch);
				return@setOnClickListener;
			}
			else
				pwdNew2Input.error = null;
			
			
			var map = HashMap<String, String>();
			map.put("passw", pwdNowInput.text.toString())
			map.put("passw_new", pwdNew1Input.text.toString())
			map.put("passw_new_re", pwdNew2Input.text.toString())
			
			
			Web.request(this, "/user.password.set.php", map, object : WebCallBackInterface
			{
				override fun onSuccess(result: JsonObject)
				{
					Toast.makeText(baseContext, getString(R.string.saved), Toast.LENGTH_LONG).show();
					closePwd.callOnClick();
				}
			})
		}
		
		instName = Static.instName
		instID = Static.instID
		
		val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, institutesList)
		
		instInput.setAdapter(adapter)
		var i = 0;
		for(st : String in institutesList)
		{
			if(st.equals(instName))
			{
				instInput.setSelection(i)
				break;
			}
			i++;
		}
		
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
	
	
}
