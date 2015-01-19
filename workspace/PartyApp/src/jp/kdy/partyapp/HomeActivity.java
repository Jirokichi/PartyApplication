package jp.kdy.partyapp;

import java.io.IOException;

import jp.kdy.bluetooth.BlueToothMessageResultReceiver;
import jp.kdy.bluetooth.ManagedDevices;
import jp.kdy.bluetooth.ManagedDevices.KYDevice;
import jp.kdy.bluetooth.communication.CommunicationReceiverTask;
import jp.kdy.bluetooth.communication.CommunicationSenderTask;
import jp.kdy.bluetooth.communication.CommunicationTask.BlueToothResult;
import jp.kdy.partyapp.marubatsu.AppMaruBatsuActivity;
import jp.kdy.util.MyFragmentDialog;
import jp.kdy.util.MyFragmentDialog.MyDialogListener;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
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

public class HomeActivity extends BlueToothBaseActivity implements BlueToothMessageResultReceiver, MyDialogListener {

	private static final long serialVersionUID = 1L;

	BluetoothSocket mSocket;

	/**
	 * 接続を解除するためのメソッド
	 */
	private void closeSocket() {
		BluetoothDevice device = null;
		if (mSocket != null) {
			device = mSocket.getRemoteDevice();
		}

		if (mContext != null) {
			String message = null;
			if (KYUtils.DEBUG) {
				message = "**";
			} else {
				if (device != null) {
					message = device.toString();
				} else {
					message = "他の端末";
				}
			}
			Toast.makeText(mContext, String.format("%sとの接続を切断しました", message), Toast.LENGTH_LONG).show();
		}

		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				mSocket = null;
			}
		}

		// リスト上の(接続済み)メッセージ変更
		if (mDevices != null) {
			mDevices.updateDeviceisConnected(device, false);
		}
		if (mDeviceList != null) {
			mDeviceList.invalidateViews();
		}

		// Applicationの実行の禁止化
		disableAppsButton();
	}

	/**
	 * アプリ開始ボタンの無効化
	 */
	private void disableAppsButton() {
		Button b = (Button) this.findViewById(R.id.buttonMaruBatsuGame);
		if (b != null) {
			b.setEnabled(false);
		}
	}

	// Applicationクラス。mSocketと親情報を保存する。
	BlueToothBaseApplication mApp;

	private ListView mDeviceList;
	private DeviceListAdapter dAdapter;
	private ToggleButton mToggleButton;

	private MyFragmentDialog mWaitForSelectionOfClientDialog = null;
	//InterChangeTask mWaitForMessageTask = null;
	CommunicationReceiverTask receiverTask = null;

	/**
	 * フラグメントのタグ。onClickedメソッドなどで呼び出しもとを判定するために利用。
	 * 
	 * @author yuya DIALOG_TO_WAIT_APPS_SELECTION_OF_PAERENT : 親がアプリを選択するのを待つための待機ダイアログ用のタグ DIALOG_TO_CONFIRM_SELECTION_OF_APP_BY_PAERENT : 親がアプリを選択した際の確認ダイアログ用のタグ
	 */
	private static enum FragmentTag {
		DIALOG_TO_WAIT_APPS_SELECTION_OF_PAERENT, DIALOG_TO_CONFIRM_SELECTION_OF_APP_BY_PAERENT
	};

	/**
	 * 親が子に送信するアプリ起動前の確認メッセージ。子は受信するメッセージに応じたアプリを起動する。
	 * 
	 * @author yuya
	 * 
	 */
	private static enum AppName {
		MaruBatsuGame, Game2
	};

	/**
	 * 処理中ダイアログ(クライアントのアプリ選択待ち)を閉じるときの処理
	 */
	private void finishWaitForSlectionOfClientDialog() {
		if (mWaitForSelectionOfClientDialog != null) {
			mWaitForSelectionOfClientDialog.dismiss();
			mWaitForSelectionOfClientDialog = null;
		}
	}

	/**
	 * クライアントによるアプリ選択の待機状態を終了するときの処理
	 */
	private void finishWaitForMessageTask() {
		if (receiverTask != null) {
			// キャンセルが実施されるとdidBlueToothMessageResultReceiverが呼び出される
			if (!receiverTask.isCancelled()) {
				receiverTask.cancel(true);
			}
			receiverTask = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		log("onDestroy");
		// Socketクローズ
		closeSocket();
		// 待機ウィンドウクローズ
		finishWaitForSlectionOfClientDialog();
		// 待機タスククローズ
		finishWaitForMessageTask();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate");
		mApp = (BlueToothBaseApplication) this.getApplication();

		setContentView(R.layout.activity_home);

		// 端末リストについて
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
				newFragment.setListParameter(new String[] { "接続", "詳細" }, new DialogInterface.OnClickListener() {
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
		dAdapter = new DeviceListAdapter(this.getApplicationContext(), this.mDevices);
		mDeviceList.setAdapter(dAdapter);

		// たぐるボタンについて
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

		// すでにsocket接続がある場合
		if (mApp.mSocket != null) {
			this.mSocket = mApp.mSocket;
		}

		// デバッグモードの場合
		if (KYUtils.DEBUG) {
			Button b = (Button) this.findViewById(R.id.buttonMaruBatsuGame);
			if (b != null)
				b.setEnabled(true);
		}
	}

	/**
	 * 周囲のデバイス検索ボタン押下時
	 * 
	 * @param view
	 */
	public void onSearchDevice(View view) {
		log("onSearchDevice");
		Button b = (Button) view;
		b.setText(this.getString(R.string.buttonText_to_searching_NearByMe));
		searchNewDevices();
	}

	/**
	 * 検出可能に変更ボタン押下時
	 * 
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
	 * 
	 * @param view
	 */
	public void onMaruBatsuGameClick(View view) {
		log("AppMaruBatsuActivity");
		if (isSocketWorking(mSocket)) {
			// 確認のダイアログを表示する
			String message = null;
			if (KYUtils.DEBUG) {
				message = "**と対戦しますか？";
			} else {
				message = String.format("%sと対戦しますか？", mSocket.getRemoteDevice().getName());
			}
			MyFragmentDialog dialog = MyFragmentDialog.newInstanceForNormalDilog("アプリ起動", message, this);
			dialog.show(this.getSupportFragmentManager(), FragmentTag.DIALOG_TO_CONFIRM_SELECTION_OF_APP_BY_PAERENT.name());
		} else {
			if (mSocket != null) {
				Toast.makeText(this, String.format("すでに%sとの接続が切れています", mSocket.getRemoteDevice()), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "すでに他端末との接続が切れています", Toast.LENGTH_SHORT).show();

			}
		}

	}

	@Override
	protected void didGetHistoryOfDevices() {
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

	/**
	 * ListViewの中でDeviceと同じレコードの状態をisConectedに変更
	 * 
	 * @param device
	 * @param devices
	 * @param listView
	 * @param isConected
	 *            リスト上の接続あり、未接続のメッセージ
	 */
	private void changeStatus(BluetoothDevice device, ManagedDevices deices, ListView listView, boolean isConected) {
		log("mSocket.getRemoteDevice();:" + device);
		deices.updateDeviceisConnected(device, isConected);
		listView.invalidateViews();
	}

	@Override
	public void didBlueToothConnectionResultReceiver(BluetoothSocket result, boolean isClient, boolean isCancel) {
		//
		log("didBlueToothResultReceiver" + "(" + isClient + "):" + result);
		mSocket = result;

		if (isCancel) {
			Toast.makeText(this, "検索がキャンセルされました", Toast.LENGTH_LONG).show();
		} else if (mSocket != null) {
			Toast.makeText(this, String.format("%sと接続しました", mSocket.getRemoteDevice()), Toast.LENGTH_LONG).show();
			mApp.setBluetoothSocket(mSocket);
			log("isConnected:" + mSocket.isConnected());
			changeStatus(mSocket.getRemoteDevice(), mDevices, mDeviceList, true);
			this.mApp.parentPlayer = isClient;

			if (isClient) {
				// クライアントの場合はアプリを有効化
				Button b = (Button) this.findViewById(R.id.buttonMaruBatsuGame);
				b.setEnabled(true);
			} else {
				// クライアントではない場合、ダイアログで待機
				// 待機用ダイアログを生成
				mWaitForSelectionOfClientDialog = MyFragmentDialog.newInstanceForProgressDilog("待機中", "親がアプリを選択するまでしばらくおまちください...", this);
				mWaitForSelectionOfClientDialog.show(getSupportFragmentManager(), FragmentTag.DIALOG_TO_WAIT_APPS_SELECTION_OF_PAERENT.name());

				// クライアントからの送信待機
				receiverTask = new CommunicationReceiverTask(mSocket, this);
				receiverTask.execute(new Object[] { "Wait=" + mSocket + ")" });
			}

		} else {
			Toast.makeText(this, "指定した端末がみつかりませんでした", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void didBlueToothMessageResultReceiver(BlueToothResult result) {
		/**
		 * このクラスでは、このメソッドは以下の条件のときに呼び出される 1. 親がアプリを選択し、そのアプリ名を子に送信した後 2. 子がアプリ名を受信したとき 3. 子がアプリ名を送信したとき 4. 親がアプリ名を受信したとき
		 * 
		 * [備考：アプリ起動までの流れ] 親: アプリ名送信 > 子が本当に現在もアプリ起動中か確認するためメッセージが帰ってくるのを待機 > アプリ名受信 > アプリ起動 子: 親のアプリ選択を待機 > アプリ名受信 > アプリ名送信 > アプリ起動
		 */
		log(String.format("result(client[%s]):%s", isClientDevice, result.toString()));
		Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show();

		if (isClientDevice) {
			this.didBlueToothMessageResultReceiverForClient(result);
		} else {
			this.didBlueToothMessageResultReceiverForNotClient(result);
		}
	}

	private void didBlueToothMessageResultReceiverForNotClient(BlueToothResult result) {
		String message = null;
		switch (result.type) {
		case SendSuccess:
			finishWaitForSlectionOfClientDialog();
			finishWaitForMessageTask();
			startGame();
			break;
		case ReceiveSuccess:
			message = result.resultMessage;
			if (message.equals(AppName.MaruBatsuGame.name())) {
				finishWaitForMessageTask();
				CommunicationSenderTask ict = new CommunicationSenderTask(mSocket, this, AppName.MaruBatsuGame.name());
				ict.execute(new Object[] { "Send=" + mSocket + ")" });
			}
			break;
		case Exception:
		case Cancel:
			// 例外は相手がSocketを閉じた場合発生する場合があるため、その場合ダイアログを閉じてあげる必要がある
			finishWaitForSlectionOfClientDialog();
			finishWaitForMessageTask();
			closeSocket();
			break;
		default:
			break;
		}
	}

	private void didBlueToothMessageResultReceiverForClient(BlueToothResult result) {
		String message = null;
		switch (result.type) {
		case SendSuccess:
			// 　子から準備完了通知を待つ
			finishWaitForMessageTask();
			receiverTask = new CommunicationReceiverTask(mSocket, this);
			receiverTask.execute(new Object[] { "Wait=" + mSocket + ")" });
			break;
		case ReceiveSuccess:
			// 子から準備完了通知(アプリ名)を取得した場合のみゲーム開始
			message = result.resultMessage;
			if (message.equals(AppName.MaruBatsuGame.name())) {
				finishWaitForSlectionOfClientDialog();
				finishWaitForMessageTask();
				startGame();
			}
			break;
		case Exception:
		case Cancel:
			// 例外は相手がSocketを閉じた場合発生する場合があるため、その場合ダイアログを閉じてあげる必要がある
			finishWaitForSlectionOfClientDialog();
			finishWaitForMessageTask();
			closeSocket();
			break;
		default:
			break;
		}
	}

	/**
	 * ゲームを開始する
	 */
	private void startGame() {
		Intent intent = new Intent(this.getApplicationContext(), AppMaruBatsuActivity.class);
		startActivityForResult(intent, ActivityRequestCode.ACTIVITY_MARUBATSU.ordinal());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		log(String.format("(requestCode, resultCode, data)=(%s, %s, %s)", requestCode, resultCode, data));
		if (requestCode == ActivityRequestCode.ACTIVITY_MARUBATSU.ordinal()) {
			log("○×ゲームから戻ってきた場合");
			onMaruBatsuActivityResult(resultCode, data);
		}
	}

	private void onMaruBatsuActivityResult(int resultCode, Intent data) {
		log(String.format("resultCode(%s):%s", resultCode, data));
		log(String.format("mSocket:%s", mSocket));
		if (isSocketWorking(mSocket)) {
			changeStatus(mSocket.getRemoteDevice(), mDevices, this.mDeviceList, false);
			closeSocket();
		}

		this.disableAppsButton();
	}

	/**
	 * appNameのゲーム開始を他のデバイスに伝える
	 * 
	 * @param tag
	 */
	private void sendStartAppToOtherDevice(String tag) {
		if (tag.equals(FragmentTag.DIALOG_TO_CONFIRM_SELECTION_OF_APP_BY_PAERENT.name())) {

			// 待機用ダイアログを生成
			mWaitForSelectionOfClientDialog = MyFragmentDialog.newInstanceForProgressDilog("待機中", "アプリ起動中...", this);
			mWaitForSelectionOfClientDialog.show(getSupportFragmentManager(), FragmentTag.DIALOG_TO_WAIT_APPS_SELECTION_OF_PAERENT.name());

			CommunicationSenderTask ict = new CommunicationSenderTask(mSocket, this, AppName.MaruBatsuGame.name());
			ict.execute(new Object[] { "Send=" + mSocket + ")" });
		}
	}

	@Override
	public void onClicked(DialogInterface dialog, int which, String tag) {
		log(String.format("%s, %s, %s", dialog, which, tag));
		if (tag.equals(FragmentTag.DIALOG_TO_CONFIRM_SELECTION_OF_APP_BY_PAERENT.name())) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				sendStartAppToOtherDevice(tag);
			}
		} else if (tag.equals(FragmentTag.DIALOG_TO_WAIT_APPS_SELECTION_OF_PAERENT.name())) {
			// サーバーがクライアントがアプリを選択するのを待機中のときに利用
			if (which == DialogInterface.BUTTON_NEGATIVE) {
				// キャンセルされたとき
				log(String.format("キャンセル"));
				finishWaitForSlectionOfClientDialog();
				finishWaitForMessageTask();
				closeSocket();
			}
		}
	}
}
