package jp.kdy.bluetooth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class ManagedDevices {

	public ArrayList<BluetoothDevice> deviceInHistory = null;
	public boolean hasDevicesHistory = false;
	public int mSize = 0;
	
	@Override
	public String toString(){
		String value = null;
		value = "hasDevicesHistory(" + mSize + "):"+hasDevicesHistory;
		value += "\n";
		if(deviceInHistory == null){
			return value + "deviceInHistory = null";
		}
		
		for(BluetoothDevice device:deviceInHistory){
			value += device.getName() + ":" + device.getAddress();
		}
		
		return value;
	}
	
	public ManagedDevices(){
		deviceInHistory = new ArrayList<BluetoothDevice>();
	}
	
	public void updateDeviceHistory(BluetoothAdapter bt){
		setListOfUsersYouHaveConnected(bt);
	}
	
	public void delete(){
		if(deviceInHistory != null){
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
				deviceInHistory.add(device);
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

	public void addNewDevice(BluetoothDevice newDevice) {
		if (deviceInHistory != null){
			deviceInHistory.add(newDevice);
			mSize++;
		}
	}
	
}
