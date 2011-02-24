package org.darkgoddess.alertme;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class AlertMeHelp extends Activity {
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.alertme_help);
	}
	

    public void invokeTest() {
		Intent i = new Intent();
		i.setClass(this, AlertMeTest.class);
    	//Intent i = new Intent(this, AlertMeSettings.class);
		//i.putExtra(AlertMeConstants.INTENT_REQUEST_KEY, AlertMeConstants.INVOKE_SETTINGS_CREATE_FIRSTTIME);
        //startActivityForResult(i, AlertMeConstants.INVOKE_SETTINGS_CREATE_FIRSTTIME);
		startActivity(i);
    }
    public void invokeTest(View view) {
    	invokeTest();
    }
}
