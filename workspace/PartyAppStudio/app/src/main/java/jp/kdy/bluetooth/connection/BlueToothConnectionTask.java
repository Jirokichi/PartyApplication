package jp.kdy.bluetooth.connection;

import java.util.UUID;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

import static jp.kdy.partyapp.KYUtils.*;

public abstract class BlueToothConnectionTask extends AsyncTask<Object, Integer, BluetoothSocket> implements OnCancelListener {

	// BlueToothコネクションのためのインスタンス
	public static final UUID TECHBOOSTER_BTSAMPLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	static BluetoothAdapter myBlueToothAdapter;
    private BlueToothConnectionResultReceiver mReceiver;
	 
    // プログレスダイアログのために必要なインスタンス
    Context mContext;
    private String mTitle = "Please wait";
    private String mMessage = "Loading data...";
    private ProgressDialog progressDialog;
	
	public BlueToothConnectionTask(Context context, BluetoothAdapter btAdapter, BlueToothConnectionResultReceiver receiver, String title, String message){
		log("BlueToothConnectionTask()");
		this.mContext = context;
		myBlueToothAdapter = btAdapter;
		this.mReceiver = receiver;

		if(mTitle != null){
			mTitle = title;
		}
		if(mMessage != null){
			mMessage = message;
		}
	}
	
	@Override
	protected void onPreExecute() {
		// This method is in UI Thread
		log("onPreExecute");
		
		progressDialog = new ProgressDialog(mContext);
		
		progressDialog.setTitle(mTitle);
		progressDialog.setMessage(mMessage);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(true);
		progressDialog.setOnCancelListener(this);
		progressDialog.show();
	}
	
	/*
	 * このクラスを継承したクラスで実装する
	 */
	@Override
	abstract protected BluetoothSocket doInBackground(Object... params);
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		log("onProgressUpdate");
	}

	@Override
	protected void onCancelled(){
		log("onCancelled");
		mReceiver.didBlueToothConnectionResultReceiver(null, isClient(), true);
	}
	
	@Override
	protected void onPostExecute(BluetoothSocket result) {
		log("onPostExecute - " + result);
		
		progressDialog.dismiss();
		mReceiver.didBlueToothConnectionResultReceiver(result, isClient(), false);
	}
	
	
	/**
	 * このクラスを継承したクラスがクライアントなのかを判定するためのメソッド
	 * @return
	 */
	abstract protected boolean isClient();
	
	/**
	 * プログレスダイアログのキャンセル
	 */
	@Override
	public void onCancel(DialogInterface dialog) {
		onDialogCancel();
		cancel(true);
	}
	
	/**
	 * プログレスダイアログのキャンセル時に実施しなければならない処理
	 */
	abstract protected void onDialogCancel();
	
	
}
