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
	
	// �N���C���A���g���ǂ����̃t���O
	boolean isClientDevice = true;
	protected ManagedDevices mDevices = null;
	
	BluetoothAdapter mBtAdapter = null;
	
	
	// Activity�͈����ɒ��ӁB���p����̂�Toast�����ɂ���ׂ�
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
	 * �f�o�C�X�������擾��ɌĂяo����郁�\�b�h
	 * mDevices�Ɏ擾����������ۑ�
	 * �Ăяo�����^�C�~���O�F
	 * �@����Bluetooth��ON�̂Ƃ� -> super.onCreate��
	 * �@����Bluetooth��NG�̏ꍇ��ON�ɂ����Ƃ� -> super.onCreate -> sub.onCreate -> ... -> �����I
	 * �@
	 */
	abstract void didGetHistoryOfDevices();
	
	/**
	 *  �V�K�ڑ��Ńo�C�X���������Č��������ꍇ�ɌĂ΂�郁�\�b�h
	 *  ��������f�o�C�X�����擾�\�����AmDevices���ɂ��łɕۑ�����Ă���
	 *  in UI Thread;
	 */
	abstract void didDetectedDevice(String result, BluetoothDevice foundDevice); 
	
	/**
	 *  �����̃f�o�C�X�������\�ɂ������ƁA
	 *  ���̌��������������ꂽ�Ƃ��ɌĂяo����郁�\�b�h
	 *  in UI Thread;
	 */
	abstract void didDisableToBeSearched(); 
	
	/**
	 *  ���f�o�C�X���猟�������ݒ�������I�ɃL�����Z�������{
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
	 * ���̃N���X�͕K��Activity�N������BlueTooth���N�����Ă��邩�ǂ����𔻒肷��
	 * �L���ɂȂ��Ă��Ȃ��ꍇ�́A�L���ɂ��邽�߂̗v��������
	 * �L���ȏꍇ�������̓��[�U�[���L���ɂ����ꍇ�́A�ڑ�������ۑ�����
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState){
		log("BlueToothBaseActivity onCreate");
		super.onCreate(savedInstanceState);
		/*
		 *  setContentView�͐e�N���X�ŗ��p����
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
				// Bluetooth��ON�ɂ��ꂽ�ꍇ�̏���
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
	 * ���̃f�o�C�X���������郁�\�b�h�B�Ȃ������ɂ���ꍇ�́A�o�^���Ȃ��B
	 */
	protected void searchNewDevices() {
		log("searchNewDevices");

		// �C���e���g�t�B���^�[��BroadcastReceiver�̓o�^
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
	 * �����̃f�o�C�X�������Ώۂɂ���
	 * @param 
	 * time: �L������(seconds)
	 */
	protected void enableFeatureSearchedByNearDevice(int time) {
		log("onDisablingFeatureSearchedByNearDevice");
		Intent discoverableOn = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableOn.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, time);
		this.startActivityForResult(discoverableOn, REQUEST_ACTIVITY_ENABLE_SEARCHED);
	}
	
	/**
	 * �N���C�A���g�Ƃ��ĒʐM���J�n���邽�߂̃��\�b�h
	 * �ڑ��v������device�Ɛڑ��ł����ꍇ�FdidBlueToothResultReceiver��
	 *  - type		= BlueToothResultReceiver.Type.CONECT
	 *  - isClient 	= true
	 *  - result 	= �T�[�o�[�Ƃ̒ʐM�ɕK�v��BluetoothSocket(���s����null)
	 *  �Ƃ��ĕԂ��Ă���
	 */
	protected void startConnectingAsClient(BluetoothDevice device){
		log("startConnectingAsClient");
		isClientDevice = true;
		BlueToothConnectionTask btask = new BlueToothConnectionTask(this, mBtAdapter, true, device, this);
		btask.execute(new Object[] { null });
	}
	
	/**
	 * �T�[�o�[�Ƃ��ăN���C�A���g����̐ڑ���҂��߂̃��\�b�h
	 * �ڑ��v������M�����ꍇ�FdidBlueToothResultReceiver��
	 *  - type		= BlueToothResultReceiver.Type.CONECT
	 *  - isClient 	= false
	 *  - result 	= �T�[�o�[�Ƃ̒ʐM�ɕK�v��BluetoothSocket(���s����null)
	 *  �Ƃ��ĕԂ��Ă���
	 */
	protected void startConnectingAsServer() {
		log("startConnectingAsServer");
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		isClientDevice = false;
		// �����ΏۂƂȂ��Ă��Ȃ��ꍇ�̏���
		BlueToothConnectionTask btask = new BlueToothConnectionTask(this, mBtAdapter, false, null, this);
		btask.execute(new Object[] { null });
	}
	
	/*
	 * �����ɂȂ��f�o�C�X��T�����߂̃��\�b�h
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
				// ���O�����o���ꂽ
				log("ACTION_NAME_CHANGED:" + dName);
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (foundDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
					log("�ڑ��������Ƃ̂Ȃ��f�o�C�X:ACTION_NAME_CHANGED - " + dName);
					mDevices.addNewDevice(foundDevice);
				} else {
					log("�ڑ��������Ƃ̂���f�o�C�X:ACTION_NAME_CHANGED - " + dName);
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
	 * log�p���\�b�h
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
	
	// �[����BlueTooth�̋@�\�������Ă邩���`�F�b�N
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
