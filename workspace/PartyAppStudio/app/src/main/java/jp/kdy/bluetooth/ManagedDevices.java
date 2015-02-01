package jp.kdy.bluetooth;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import static jp.kdy.partyapp.KYUtils.*;

public class ManagedDevices {

	public class KYDevice {
		public KYDevice(BluetoothDevice device) {
			this.device = device;
		}

		/*
		 * デバイスの名前とBluetoothアドレスをもつインスタンス
		 */
		public BluetoothDevice device = null;
		
		/*
		 * アプリを起動してからデバイス検索を実施して発見したデバイスかどうかの判定
		 */
		public boolean searchedRecently = false;
		
		/*
		 * 通信を開始しているかどうか
		 */
		public boolean isConnected = false;
		
		/*
		 * 最後に接続した日時
		 */
		public String connectedDate;
		
	}

	public ArrayList<KYDevice> deviceInHistory = null;
	public boolean hasDevicesHistory = false;
	public int mSize = 0;

	@Override
	public String toString() {
		String value = null;
		value = "hasDevicesHistory(" + mSize + "):" + hasDevicesHistory;
		value += "¥n";
		if (deviceInHistory == null) {
			return value + "deviceInHistory = null";
		}

		for (KYDevice kydevice : deviceInHistory) {
			value += kydevice.device.getName() + ":" + kydevice.device.getAddress() + ":" + kydevice.searchedRecently;
		}

		return value;
	}

	public ManagedDevices() {
		deviceInHistory = new ArrayList<KYDevice>();
	}

	public void updateDeviceHistory(BluetoothAdapter bt) {
		setListOfUsersYouHaveConnected(bt);
	}

	public void delete() {
		if (deviceInHistory != null) {
			deviceInHistory = null;
		}
	}

	// BluetoothAdapterから、接続履歴のあるデバイスの情報を取得
	private void setListOfUsersYouHaveConnected(BluetoothAdapter bt) {
		Set<BluetoothDevice> set = bt.getBondedDevices();
		Iterator<BluetoothDevice> iterator = set.iterator();
		if (iterator == null || !iterator.hasNext()) {
			hasDevicesHistory = false;
			mSize = 0;

		} else {
			while (iterator.hasNext()) {
				BluetoothDevice device = iterator.next();
				KYDevice kydevice = new KYDevice(device);
				deviceInHistory.add(kydevice);
			}
		}

		if (deviceInHistory.size() > 0) {
			hasDevicesHistory = true;
			mSize = deviceInHistory.size();
		} else {
			hasDevicesHistory = false;
			mSize = 0;
		}
	}

	public void addNewDevice(BluetoothDevice newDevice, boolean recentSearched) {
		if (deviceInHistory != null) {
			KYDevice kyNewDevice = new KYDevice(newDevice);
			kyNewDevice.searchedRecently = recentSearched;
			deviceInHistory.add(kyNewDevice);
			mSize++;
		}
	}

	public void updateDeviceRecentSearched(BluetoothDevice device, boolean recentSearched) {
		if (deviceInHistory != null) {
			for (KYDevice kyDevice : deviceInHistory) {
				if (kyDevice.device.getAddress().equals(device.getAddress())) {
					kyDevice.searchedRecently = recentSearched;
					break;
				}
			}
		}
	}
	
	public void updateDeviceisConnected(BluetoothDevice device, boolean isConnected) {
		if (deviceInHistory != null && device != null) {
			for (KYDevice kyDevice : deviceInHistory) {
				if (kyDevice.device.getAddress().equals(device.getAddress())) {
					kyDevice.isConnected = isConnected;
					if(isConnected){
						String tmp = (String) android.text.format.DateFormat.format("yyyy/MM/dd", new Date());
						log(tmp);
						kyDevice.connectedDate = tmp;
						kyDevice.searchedRecently = true;
					}
					break;
				}
			}
		}
	}

}
