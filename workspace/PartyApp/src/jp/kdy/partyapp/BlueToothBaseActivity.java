package jp.kdy.partyapp;

import static jp.kdy.partyapp.KYUtils.log;

import java.util.ArrayList;

import jp.kdy.bluetooth.ManagedDevices;
import jp.kdy.bluetooth.connection.BlueToothConnectionForReceiveTask;
import jp.kdy.bluetooth.connection.BlueToothConnectionForSendTask;
import jp.kdy.bluetooth.connection.BlueToothConnectionResultReceiver;
import jp.kdy.bluetooth.connection.BlueToothConnectionTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

public abstract class BlueToothBaseActivity extends FragmentActivity implements BlueToothConnectionResultReceiver {
	
	//startActivity用(継承する場合はこの値を再定義する必要がある)
	protected static enum ActivityRequestCode {ENABLE_BLUETOOTH, ENABLE_SEARCHED, ACTIVITY_MARUBATSU, ACTIVITY_HATENA}; 
		
	// クラインアントかどうかのフラグ
	protected boolean isClientDevice = false;
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
				startActivityForResult(btOn, ActivityRequestCode.ENABLE_BLUETOOTH.ordinal());
			}
		} else {
			log("This device does not support bluetooth");
		}
	}
	
	/**
	 *　他のアクティビティから戻ってきた際に呼び出されるメソッド
	 *　このクラス(BleuToothBaseActivity)を継承するアクティビティでもonActivityResultを定義する場合は、
	 *　そのメソッド内でsuperで呼び出す必要と、requestCodeがかぶらないようにする必要がある。
	 */
	@Override
	protected void onActivityResult(int requestCode, int ResultCode, Intent date) {
		log(String.format("(requestCode, resultCode, data)=(%s, %s, %s)", requestCode, ResultCode, date));
		if (requestCode == ActivityRequestCode.ENABLE_BLUETOOTH.ordinal()) {
			log("Bluetoothを有効にするためのダイアログから戻ってきた場合");
			if (ResultCode == Activity.RESULT_OK) {
				// BluetoothがONにされた場合の処理
				log("Bluetooth has been enabled");
				getListOfUsersYouHaveConnected();
				log(this.mDevices.toString());
			} else {
				log("Bluetooth is not enabled yet");
				Toast.makeText(this, "Bluetooth is not enabled yet", Toast.LENGTH_LONG).show();
			}
		}else if(requestCode == ActivityRequestCode.ENABLE_SEARCHED.ordinal()){
			log("Bluetoothが有効になっている近くの端末を検索するためのダイアログから戻ってきた場合");
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
		this.startActivityForResult(discoverableOn, ActivityRequestCode.ENABLE_SEARCHED.ordinal());
	}
	
	/**
	 * クライアント(ここでは親機)として他の端末との通信を確立するためのメソッド
	 * 接続要求を出したdeviceと接続できた場合：didBlueToothResultReceiverメソッドで
	 *  - type		= BlueToothResultReceiver.Type.CONECT
	 *  - isClient 	= true
	 *  - result 	= サーバーとの通信に必要なBluetoothSocket(失敗時はnull)
	 *  として返ってくる
	 *  
	 *  @param
	 *  	device 接続したい相手のデバイス情報
	 */
	protected void startConnectingAsClient(BluetoothDevice device){
		log("startConnectingAsClient");
		isClientDevice = true;
		String title = String.format("%sの検索", device.getName());
		String message = "検索中...";
		BlueToothConnectionTask btask = new BlueToothConnectionForSendTask(this, mBtAdapter, this, title, message, device);
		btask.execute(new Object[] { null });
	}
	
	/**
	 * サーバー(子機)としてクライアント(親機)からの接続を待つためのメソッド
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
		String title = String.format("待機");
		String message = "親機からの接続待機中...";
		BlueToothConnectionTask btask = new BlueToothConnectionForReceiveTask(this, mBtAdapter, this, title, message);
		btask.execute(new Object[] { null });
	}
	
	/**
	 * Socketが有効かどうかを判定するメソッド
	 * 
	 * @param socket
	 * @return
	 */
	protected boolean isSocketWorking(BluetoothSocket socket) {
		boolean result = true;

		if (KYUtils.DEBUG)
			return result;

		if (socket == null) {
			result = false;
		}

		log(socket.toString());
		// else {
		// isConnected()は接続確認をしたデバイスがある場合trueなだけで、現在も通信可能かどうかを保障しないため
		// log("socket.isConnected():" + socket.isConnected());
		// if (!socket.isConnected()) {
		// result = false;
		// } else {
		// }
		// }
		return result;
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
				log("1. Start searching new devices...");
				if(mActivity != null)
					Toast.makeText(mActivity, "Start searching new devices...", Toast.LENGTH_LONG).show();
			}
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				log("2. Detext new device...");
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
				log("3. ACTION_NAME_CHANGED:" + dName);
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (foundDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
					log("接続したことのないデバイス:ACTION_NAME_CHANGED - " + dName);
					mDevices.addNewDevice(foundDevice, true);
				} else {
					log("接続したことのあるデバイス:ACTION_NAME_CHANGED - " + dName);
					mDevices.updateDeviceRecentSearched(foundDevice, true);
				}
				didDetectedDevice(BluetoothDevice.ACTION_NAME_CHANGED, foundDevice);
			}

			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				log("4. Done searching new devices.");
				didDetectedDevice(BluetoothAdapter.ACTION_DISCOVERY_FINISHED, null);
			}
			log("onReceive - fin");
		}
	};
	
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
