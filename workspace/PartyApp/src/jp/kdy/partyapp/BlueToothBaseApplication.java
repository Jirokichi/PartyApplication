package jp.kdy.partyapp;

import java.io.IOException;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

public class BlueToothBaseApplication extends Application {

	private static final String TAG = "BlueToothBaseApplication";
	public BluetoothSocket mSocket;
	public boolean parentPlayer;
	
	@Override
	public void onCreate(){
		super.onCreate();
		log("onCreate()");
		
	}
	
	@Override 
	public void onTerminate(){
		super.onTerminate();
		if(mSocket != null){
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally{
				mSocket = null;
			}
		}
		
	}
	
	/*
	 * �A�v���P�[�V�������x����BlueToothAdapter��ۑ�����
	 */
	public void setBluetoothSocket(BluetoothSocket socket){
		mSocket = socket;
	}
	/*
	 * �ۑ����ꂽBlueToothAdapter���擾����
	 */
	public BluetoothSocket getBluetoothSocket(){
		 return this.mSocket;
	}
	
	private void log(String message){
		Log.d(TAG, message);
	}

}
