package com.arseniy899.artem_app


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import me.dm7.barcodescanner.zbar.Result
import me.dm7.barcodescanner.zbar.ZBarScannerView

/**
 *
 * @property mScannerView ZBarScannerView?
 */
class ScanQrCodeActivity : Activity(), ZBarScannerView.ResultHandler
{
	private var mScannerView: ZBarScannerView? = null
	
	public override fun onCreate(state: Bundle?)
	{
		super.onCreate(state)
		mScannerView = ZBarScannerView(this)    // Programmatically initialize the scanner view
		setContentView(mScannerView)                // Set the scanner view as the content view
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		{
			if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
			{
				requestPermissions(arrayOf(Manifest.permission.CAMERA), 145);
				mScannerView!!.stopCamera()
				return;
			}
		}
		
	}
	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>, grantResults: IntArray
	)
	{
		when(requestCode)
		{
			145 ->
			{
				if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
				{
					mScannerView = ZBarScannerView(this)    // Programmatically initialize the scanner view
					setContentView(mScannerView)                // Set the scanner view as the content view
				}
				else
				{
					finish()
				}
				return
			}
		}
		// other 'switch' lines to check for other
		// permissions this app might request
	}
	public override fun onResume()
	{
		super.onResume()
		mScannerView!!.setResultHandler(this) // Register ourselves as a handler for scan results.
		mScannerView!!.startCamera()          // Start camera on resume
	}
	
	public override fun onPause()
	{
		super.onPause()
		mScannerView!!.stopCamera()           // Stop camera on pause
	}
	
	override fun handleResult(rawResult: Result)
	{
//		mScannerView!!.resumeCameraPreview(this)
		var result = Intent()
		result.putExtra("hash",rawResult.getContents());
		setResult(Activity.RESULT_OK,result);
		finish()
	}
}