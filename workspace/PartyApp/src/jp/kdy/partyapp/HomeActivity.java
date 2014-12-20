package jp.kdy.partyapp;

import java.io.IOException;

import jp.kdy.bluetooth.ManagedDevices;
import jp.kdy.bluetooth.ManagedDevices.KYDevice;
import jp.kdy.partyapp.marubatsu.AppMaruBatsuActivity;
import jp.kdy.util.MyFragmentDialog;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class HomeActivity extends BlueToothBaseActivity {

	private static final String TAG = "HomeActivity";

	BluetoothSocket mSocket;
	BlueToothBaseApplication mApp;

	private ListView mDeviceList;
	private DeviceListAdapter dAdapter;
	private ToggleButton mToggleButton;

	@Override
	protected void onDestroy() {
		super.onDestroy();
		log("onDestroy");
		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate");
		mApp = (BlueToothBaseApplication) this.getApplication();

		setContentView(R.layout.activity_home);
		mDeviceList = (ListView) this.findViewById(R.id.deviceList);
		mDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (mSocket == null) {
					KYDevice item = (KYDevice) ((ListView) parent).getItemAtPosition(position);
					startConnectingAsClient(item.device);
					Toast.makeText(mContext, "接続開始：" + item.device.getAddress(), Toast.LENGTH_SHORT).show();
				} else {
					closeSocket();
				}
			}
		});

		final Activity activity = this;
		mDeviceList.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View arg1, int position, long id) {

				ListView listView = (ListView) parent;
				final KYDevice item = (KYDevice) listView.getItemAtPosition(position);
				Toast.makeText(mContext, "対象：" + item.device.getAddress() + "(" + item.searchedRecently + ")", Toast.LENGTH_SHORT).show();
				// ダイアログを表示する
				MyFragmentDialog newFragment = MyFragmentDialog.newInstanceForListDilog(item.device.getName(), "message");
				newFragment.setListParameter(new String[] { "接続", "詳細"  }, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							if (mSocket == null) {
								startConnectingAsClient(item.device);
							}
							break;
						case 1:
							Toast.makeText(activity, "よくある質問が押された", Toast.LENGTH_LONG).show();
							break;
						default:
							break;
						}
					}
				});
				newFragment.show(getSupportFragmentManager(), "list_dialog");
				return true;
			}
		});

		mToggleButton = (ToggleButton) findViewById(R.id.toggleButton);
		mToggleButton.setChecked(false);
		mToggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				log("call OnCheckdChangeListener:" + isChecked);
				if (isChecked) {
					enableFeatureSearchedByNearDevice(60);
				} else {
					cancelEnableFeatureSearchedByNearDevice();
				}
			}
		});

		dAdapter = new DeviceListAdapter(this.getApplicationContext(), this.mDevices);
		mDeviceList.setAdapter(dAdapter);

		if (mApp.mSocket != null) {
			this.mSocket = mApp.mSocket;
		}

		if (KYUtils.DEBUG) {
			Button b = (Button) this.findViewById(R.id.buttonMaruBatsuGame);
			if (b != null)
				b.setEnabled(true);
		}
	}

	/**
	 * 周囲のデバイス検索ボタン押下時
	 * @param view
	 */
	public void onSearchDevice(View view) {
		log("onSearchDevice");
		Button b = (Button) view;
		b.setText(this.getString(R.string.buttonText_to_searching_NearByMe));
		this.searchNewDevices();
	}

	/**
	 * 検出可能に変更ボタン押下時
	 * @param view
	 */
	public void onWaitForBeingAccessedByClientButton(View view) {
		log("onWaitForBeingAccessedByClientButton");
		if (mSocket == null) {
			this.startConnectingAsServer();
		} else {
			closeSocket();
		}
	}

	/**
	 * ○×ゲームボタン押下時
	 * @param view
	 */
	public void onMaruBatsuGameClick(View view) {
		log("AppMaruBatsuActivity");
		if (isSocketWorking(mSocket)) {
			Intent intent = new Intent(this.getApplicationContext(), AppMaruBatsuActivity.class);
			this.startActivity(intent);
		} else {
			if (mSocket != null) {
				Toast.makeText(this, String.format("すでに%sとの接続が切れています", mSocket.getRemoteDevice()), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "すでに他端末との接続が切れています", Toast.LENGTH_SHORT).show();

			}
		}

	}

	private boolean isSocketWorking(BluetoothSocket socket) {
		boolean result = true;

		if (KYUtils.DEBUG)
			return result;

		log(socket.toString());
		if (socket == null) {
			result = false;
		} else {
			log("socket.isConnected():" + socket.isConnected());
			if (!socket.isConnected()) {
				result = false;
			} else {
			}

		}
		return result;
	}

	private void closeSocket() {
		Toast.makeText(mContext, String.format("%sとの接続を切断しました", mSocket.getRemoteDevice()), Toast.LENGTH_LONG).show();
		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				mSocket = null;
			}
		}

		// Applicationの実行の禁止化
		Button b = (Button) this.findViewById(R.id.buttonMaruBatsuGame);
		b.setEnabled(false);

	}

	@Override
	void didGetHistoryOfDevices() {
		log("didGetHistoryOfDevices");
		if (dAdapter != null)
			dAdapter.notifyDataSetChanged();
		Toast.makeText(this, this.mDevices.toString(), Toast.LENGTH_LONG).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	@Override
	void didDetectedDevice(String result, BluetoothDevice foundDevice) {
		log("didDetectedDevice:" + result + ":" + foundDevice);
		if (BluetoothDevice.ACTION_FOUND.equals(result)) {
			log("ACTION_FOUND:");
			mDeviceList.invalidateViews();
		} else if (BluetoothDevice.ACTION_NAME_CHANGED.equals(result)) {
			log("ACTION_NAME_CHANGED:");
			mDeviceList.invalidateViews();
		} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(result)) {
			log("ACTION_DISCOVERY_FINISHED:");
			Button b = (Button) this.findViewById(R.id.buttonServer);
			if (b != null)
				b.setText(getString(R.string.buttonText_to_search_NearByMe));
			if (b != null)
				Toast.makeText(this, "端末の検索が終了しました", Toast.LENGTH_SHORT).show();
		}
		log(mDeviceList.toString());
	}

	@Override
	void didDisableToBeSearched() {
		log("didDisableToBeSearched");
		if (mToggleButton != null)
			mToggleButton.setChecked(false);
		Button b = (Button) this.findViewById(R.id.buttonServer);
		if (b != null)
			b.setText(getString(R.string.buttonText_to_search_NearByMe));
	}

	@Override
	public void didBlueToothConnectionResultReceiver(BluetoothSocket result, boolean isClient, boolean isCancel) {
		log("didBlueToothResultReceiver" + "(" + isClient + "):" + result);
		mSocket = result;

		if (isCancel) {
			Toast.makeText(this, "検索がキャンセルされました", Toast.LENGTH_LONG).show();
		} else if (mSocket != null) {
			BluetoothDevice device = mSocket.getRemoteDevice();
			Toast.makeText(this, String.format("%sと接続しました", mSocket.getRemoteDevice()), Toast.LENGTH_LONG).show();
			log("mSocket.getRemoteDevice();:" + device);
			log("isConnected:" + mSocket.isConnected());
			mApp.setBluetoothSocket(mSocket);
			Button b = (Button) this.findViewById(R.id.buttonMaruBatsuGame);
			b.setEnabled(true);
			this.mApp.parentPlayer = isClient;
			this.mDevices.updateDeviceisConnected(device, true);
			mDeviceList.invalidateViews();

		} else {
			Toast.makeText(this, "指定した端末がみつかりませんでした", Toast.LENGTH_LONG).show();
		}
	}

	private void log(String message) {
		Log.d(TAG, message);
	}

}
