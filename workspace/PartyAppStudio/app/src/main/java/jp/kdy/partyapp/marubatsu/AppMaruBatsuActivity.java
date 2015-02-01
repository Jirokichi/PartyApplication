package jp.kdy.partyapp.marubatsu;

import jp.kdy.partyapp.R;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class AppMaruBatsuActivity extends FragmentActivity{

	private static final String TAG = "AppMaruBatsuActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate");
		setContentView(R.layout.activity_app_marubatsu);
	}

	private void log(String message) {
		Log.d(TAG, message);
	}

}
