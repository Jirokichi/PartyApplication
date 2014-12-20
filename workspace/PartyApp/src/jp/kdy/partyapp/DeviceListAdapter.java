package jp.kdy.partyapp;

import jp.kdy.bluetooth.ManagedDevices;
import jp.kdy.bluetooth.ManagedDevices.KYDevice;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * デバイスリストのアダプター
 * 
 * @author yuya
 * 
 */
public class DeviceListAdapter extends BaseAdapter {
	ManagedDevices mDevices;
	LayoutInflater mInflater;

	public DeviceListAdapter(Context context, ManagedDevices devices) {
		super();
		this.mDevices = devices;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return mDevices.mSize;
	}

	@Override
	public Object getItem(int position) {
		return mDevices.deviceInHistory.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	private class HolderView {
		TextView name;
		TextView address;
		TextView status;
		TextView date;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		HolderView holder = new HolderView();
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.list_device, null);
			holder.name = (TextView) convertView.findViewById(R.id.deviceName);
			holder.address = (TextView) convertView.findViewById(R.id.deviceAddress);
			holder.status = (TextView) convertView.findViewById(R.id.deviceStatus);
			holder.date = (TextView) convertView.findViewById(R.id.deviceDate);
			convertView.setTag(holder);
		} else {
			holder = (HolderView) convertView.getTag();
		}

		KYDevice kyDevice = (KYDevice) getItem(position);
		holder.name.setText(kyDevice.device.getName());
		holder.address.setText(kyDevice.device.getAddress());
		holder.date.setText(kyDevice.connectedDate);

		if (kyDevice.isConnected) {
			holder.status.setText("接続済み");
			holder.status.setTextColor(Color.RED);
		} else {
			if (kyDevice.searchedRecently) {
				holder.status.setText("接続可能");
				holder.status.setTextColor(Color.BLUE);
			}
		}

		return convertView;
	}

}
