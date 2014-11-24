package jp.kdy.partyapp;

import java.util.ArrayList;

import jp.kdy.bluetooth.BlueToothConnectionTask;
import jp.kdy.bluetooth.BlueToothConnectionResultReceiver;
import jp.kdy.bluetooth.ManagedDevices;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public abstract class BlueToothBaseActivity extends Activity implements BlueToothConnectionResultReceiver {

	private static final String TAG = "BlueToothBaseActivity";
	private static final int REQUEST_ACTIVITY_ENABLE_BLUETOOTH = 0;
	private static final int REQUEST_ACTIVITY_ENABLE_SEARCHED = 1;
	
	// クラインアントかどうかのフラグ
	boolean isClientDevice = true;
	protected ManagedDevices mDevices = null;
	
	BluetoothAdapter mBtAdapter = null;
	
	
	// Activityは扱いに注意。利用するのはToastだけにするべき
	Activity mActivity;
	protected Context mContext;
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		log("onDestroy()");
		if(mDevices != null){
			mDevices.delete();
			mDevices = null;
		}
	}
	
	/**
	 * デバイス履歴を取得後に呼び出されるメソッド
	 * mDevicesに取得した履歴を保存
	 * 呼び出されるタイミング：
	 * 　初期BluetoothがONのとき -> super.onCreate内
	 * 　初期BluetoothがNGの場合でONにしたとき -> super.onCreate -> sub.onCreate -> ... -> ここ！
	 * 　
	 */
	abstract void didGetHistoryOfDevices();
	
	/**
	 *  新規接続でバイスを検索して見つかった場合に呼ばれるメソッド
	 *  引数からデバイス情報を取得可能だが、mDevices内にすでに保存されている
	 *  in UI Thread;
	 */
	abstract void didDetectedDevice(String result, BluetoothDevice foundDevice); 
	
	/**
	 *  自分のデバイスを検索可能にしたあと、
	 *  その検索が無効化されたときに呼び出されるメソッド
	 *  in UI Thread;
	 */
	abstract void didDisableToBeSearched(); 
	
	/**
	 *  他デバイスから検索される設定を強制的にキャンセルを実施
	 */
	protected void cancelEnableFeatureSearchedByNearDevice(){
		if(mBtAdapter == null){
			log("cancelEnableFeatureSearchedByNearDevice : mBtAdapter = null");
		}
		mBtAdapter.cancelDiscovery();
		didDisableToBeSearched();
	}
	
	
	
	/**
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 * このクラスは必ずActivity起動時にBlueToothが起動しているかどうかを判定する
	 * 有効になっていない場合は、有効にするための要求をする
	 * 有効な場合もしくはユーザーが有効にした場合は、接続履歴を保存する
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState){
		log("BlueToothBaseActivity onCreate");
		super.onCreate(savedInstanceState);
		/*
		 *  setContentViewは親クラスで利用する
		 */
		mActivity = this;
		mContext = this.getApplicationContext();
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		mDevices = new ManagedDevices();
		
		if (hasBlueToothFeature(mBtAdapter)) {
			log("This device supports bluetooth");
			boolean btEnable = mBtAdapter.isEnabled();
			if (btEnable == true) {
				log("Bluetooth is enabled");
				getListOfUsersYouHaveConnected();
				log(this.mDevices.toString());
			} else {
				log("please enable bluetooth");
				Intent btOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(btOn, REQUEST_ACTIVITY_ENABLE_BLUETOOTH);
			}
		} else {
			log("This device does not support bluetooth");
		}
		
		
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int ResultCode, Intent date) {
		log("onActivityResult:" + requestCode + ":" + ResultCode);
		if (requestCode == REQUEST_ACTIVITY_ENABLE_BLUETOOTH) {
			if (ResultCode == Activity.RESULT_OK) {
				// BluetoothがONにされた場合の処理
				log("Bluetooth has been enabled");
				getListOfUsersYouHaveConnected();
				log(this.mDevices.toString());
			} else {
				log("Bluetooth is not enabled yet");
				Toast.makeText(this, "Bluetooth is not enabled yet", Toast.LENGTH_LONG).show();
			}
		}else if(requestCode == REQUEST_ACTIVITY_ENABLE_SEARCHED){
			if (ResultCode <= 0) {
				log("Cancel");
				didDisableToBeSearched();
			} else {
				log("Search Mode has been enabled:"+ResultCode);
				new Handler().postDelayed( delayFunc, ResultCode * 1000);
			}
		}
	}
	
	/**
	 * 他のデバイスを検索するメソッド。なお履歴にある場合は、登録しない。
	 */
	protected void searchNewDevices() {
		log("searchNewDevices");

		// インテントフィルターとBroadcastReceiverの登録
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(DevieFoundReceiver, filter);

		if (mBtAdapter.isDiscovering()) {
			log("mBtAdapter.isDiscovering() = true");
			cancelEnableFeatureSearchedByNearDevice();
		}
		mBtAdapter.startDiscovery();
	}
	
	/**
	 * 自分のデバイスを検索対象にする
	 * @param 
	 * time: 有効時間(seconds)
	 */
	protected void enableFeatureSearchedByNearDevice(int time) {
		log("onDisablingFeatureSearchedByNearDevice");
		Intent discoverableOn = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableOn.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, time);
		this.startActivityForResult(discoverableOn, REQUEST_ACTIVITY_ENABLE_SEARCHED);
	}
	
	/**
	 * クライアントとして通信を開始するためのメソッド
	 * 接続要求したdeviceと接続できた場合：didBlueToothResultReceiverで
	 *  - type		= BlueToothResultReceiver.Type.CONECT
	 *  - isClient 	= true
	 *  - result 	= サーバーとの通信に必要なBluetoothSocket(失敗時はnull)
	 *  として返ってくる
	 */
	protected void startConnectingAsClient(BluetoothDevice device){
		log("startConnectingAsClient");
		isClientDevice = true;
		BlueToothConnectionTask btask = new BlueToothConnectionTask(this, mBtAdapter, true, device, this);
		btask.execute(new Object[] { null });
	}
	
	/**
	 * サーバーとしてクライアントからの接続を待つためのメソッド
	 * 接続要求が受信した場合：didBlueToothResultReceiverで
	 *  - type		= BlueToothResultReceiver.Type.CONECT
	 *  - isClient 	= false
	 *  - result 	= サーバーとの通信に必要なBluetoothSocket(失敗時はnull)
	 *  として返ってくる
	 */
	protected void startConnectingAsServer() {
		log("startConnectingAsServer");
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		isClientDevice = false;
		// 検索対象となっていない場合の処理
		BlueToothConnectionTask btask = new BlueToothConnectionTask(this, mBtAdapter, false, null, this);
		btask.execute(new Object[] { null });
	}
	
	/*
	 * 履歴にないデバイスを探すためのメソッド
	 */
	private final BroadcastReceiver DevieFoundReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			log("onReceive");
			String action = intent.getAction();
			String dName = null;
			BluetoothDevice foundDevice;
			if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				log("Start searching new devices...");
				if(mActivity != null)
					Toast.makeText(mActivity, "Start searching new devices...", Toast.LENGTH_LONG).show();
			}
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				log("Detext new device...");
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if ((dName = foundDevice.getName()) != null) {
					if (foundDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
						log("This device has never been detected:ACTION_FOUND - " + dName);
						
					} else {
						log("This device has ever been detected:ACTION_FOUND - " + dName);
					}
				}
				didDetectedDevice(BluetoothDevice.ACTION_FOUND, foundDevice);
				// nonpairedList.setAdapter(nonPairedDeviceAdapter);
			}
			if (BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
				// 名前が検出された
				log("ACTION_NAME_CHANGED:" + dName);
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (foundDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
					log("接続したことのないデバイス:ACTION_NAME_CHANGED - " + dName);
					mDevices.addNewDevice(foundDevice);
				} else {
					log("接続したことのあるデバイス:ACTION_NAME_CHANGED - " + dName);
				}
				didDetectedDevice(BluetoothDevice.ACTION_NAME_CHANGED, foundDevice);
			}

			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				log("Done searching new devices.");
				didDetectedDevice(BluetoothAdapter.ACTION_DISCOVERY_FINISHED, null);
			}
			log("onReceive - fin");
		}
	};
	
	/*
	 * log用メソッド
	 */
	private void log(String message){
		Log.d(TAG, message);
	}
	
	private final Runnable delayFunc= new Runnable() {
	    @Override
	    public void run() {
	    	didDisableToBeSearched();
	    }
	};
	
	private void getListOfUsersYouHaveConnected(){
		mDevices.updateDeviceHistory(mBtAdapter);
		didGetHistoryOfDevices();
	}
	
	// 端末がBlueToothの機能を持ってるかをチェック
	private boolean hasBlueToothFeature(BluetoothAdapter Bt) {
		if (Bt == null) {
			Bt = BluetoothAdapter.getDefaultAdapter();
		}
		if (!Bt.equals(null)) {
			return true;
		} else {
			return false;
		}
	}
}
