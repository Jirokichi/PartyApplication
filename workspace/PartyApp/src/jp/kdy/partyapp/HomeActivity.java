package jp.kdy.partyapp;

import java.io.IOException;

import jp.kdy.bluetooth.BlueToothMessageResultReceiver;
import jp.kdy.bluetooth.InterChangeTask;
import jp.kdy.bluetooth.InterChangeTask.BlueToothResult;
import jp.kdy.bluetooth.ManagedDevices.KYDevice;
import jp.kdy.partyapp.marubatsu.AppMaruBatsuActivity;
import jp.kdy.util.MyFragmentDialog;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import static jp.kdy.partyapp.KYUtils.*;

public class HomeActivity extends BlueToothBaseActivity implements OnClickListener, BlueToothMessageResultReceiver {

	BluetoothSocket mSocket;
	/**
	 * 接続を解除するためのメソッド
	 */
	private void closeSocket() {
		if(mContext!=null){
			Toast.makeText(mContext, String.format("%sとの接続を切断しました", mSocket.getRemoteDevice()), Toast.LENGTH_LONG).show();
		}
		BluetoothDevice device = null;
		if (mSocket != null) {
			device = mSocket.getRemoteDevice();
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				mSocket = null;
			}
		}
		
		// リスト上の(接続済み)メッセージ変更
		if(mDevices != null){
			mDevices.updateDeviceisConnected(device, false);
		}
		if(mDeviceList != null){
			mDeviceList.invalidateViews();
		}
		
		// Applicationの実行の禁止化
		Button b = (Button) this.findViewById(R.id.buttonMaruBatsuGame);
		if(b != null){
			b.setEnabled(false);
		}
	}
	
	BlueToothBaseApplication mApp;

	private ListView mDeviceList;
	private DeviceListAdapter dAdapter;
	private ToggleButton mToggleButton;
	
	private MyFragmentDialog mWaitForSelectionOfClientDialog = null;
	InterChangeTask mWaitForSelectionOfClientTask = null;
	/**
	 * 処理中ダイアログ(クライアントのアプリ選択待ち)を閉じるときの処理
	 */
	private void finishWaitForSlectionOfClientDialog(){
		if(mWaitForSelectionOfClientDialog != null){
			mWaitForSelectionOfClientDialog.dismiss();
			mWaitForSelectionOfClientDialog = null;	
		}
	}
	
	/**
	 * クライアントのアプリ選択待ち状態を終了するときの処理
	 */
	private void finishWaitForSelectionOfClientTask(){
		if(mWaitForSelectionOfClientTask != null){
			// キャンセルが実施されるとdidBlueToothMessageResultReceiverが呼び出される
			if(!mWaitForSelectionOfClientTask.isCancelled()){
				mWaitForSelectionOfClientTask.cancel(true);
			}
			mWaitForSelectionOfClientTask = null;
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		log("onDestroy");
		closeSocket();
		finishWaitForSlectionOfClientDialog();
		finishWaitForSelectionOfClientTask();
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
							// 他の端末との接続がない場合のみ
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
	 * ○×ゲームボタン押下時に呼び出されるメソッド
	 * @param view
	 */
	public void onMaruBatsuGameClick(View view) {
		log("AppMaruBatsuActivity");
		if (isSocketWorking(mSocket)) {
			// 確認のダイアログを表示する
			MyFragmentDialog dialog = MyFragmentDialog.newInstanceForNormalDilog("アプリ起動", String.format("%sと対戦しますか？", mSocket.getRemoteDevice().getName()));
			dialog.setDialogListener(this);
			dialog.show(this.getSupportFragmentManager(),"confirm_dialog");
			
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

	/**
	 * Socketが有効かどうかを判定するメソッド
	 * @param socket
	 * @return
	 */
	private boolean isSocketWorking(BluetoothSocket socket) {
		boolean result = true;

		if (KYUtils.DEBUG)
			return result;

		if (socket == null){
			result = false;
		} 
		
		log(socket.toString());
//		else {
			// isConnected()は接続確認をしたデバイスがある場合trueなだけで、現在も通信可能かどうかを保障しないため
//			log("socket.isConnected():" + socket.isConnected());
//			if (!socket.isConnected()) {
//				result = false;
//			} else {
//			}
//		}
		return result;
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

			// クライアントではない場合、ダイアログで待機
			if(!isClient){
				// 待機用ダイアログを生成
				mWaitForSelectionOfClientDialog = MyFragmentDialog.newInstanceForProgressDilog("待機中", "クライアントがアプリを選択するまでしばらくおまちください...");
				mWaitForSelectionOfClientDialog.setDialogListener(this);
				mWaitForSelectionOfClientDialog.show(getSupportFragmentManager(), "progress_dialog");
				
				// クライアントからの送信待機
				mWaitForSelectionOfClientTask = new InterChangeTask(mSocket, false, null);
				mWaitForSelectionOfClientTask.setBlueToothReceiver(this);
				mWaitForSelectionOfClientTask.execute(new Object[] { "Wait=" + mSocket + ")" });
				
			}
			
		} else {
			Toast.makeText(this, "指定した端末がみつかりませんでした", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		// サーバーがクライアントがアプリを選択するのを待機中のときに利用
		finishWaitForSlectionOfClientDialog();
		finishWaitForSelectionOfClientTask();
		closeSocket();
	}

	@Override
	public void didBlueToothMessageResultReceiver(BlueToothResult result) {
		switch (result.type) {
		case SendSuccess:
			Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show();
			
			break;
		case ReceiveSuccess:
			Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show();

			break;
		case Exception:
			Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show();
			// 例外は相手がSocketを閉じた場合発生する場合があるため、その場合ダイアログを閉じてあげる必要がある
			finishWaitForSlectionOfClientDialog();
			finishWaitForSelectionOfClientTask();
			closeSocket();
			break;
		case Cancel:
			Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show();
			break;
		default:
			break;
		}
		
	}
}
