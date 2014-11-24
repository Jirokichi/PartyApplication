package jp.kdy.bluetooth;

import jp.kdy.bluetooth.InterChangeTask.BlueToothResult;
import android.bluetooth.BluetoothSocket;

public interface BlueToothConnectionResultReceiver {
	/**
	 * クライアントとサーバー接続要求の結果を返すメソッド
	 * @param result サーバーとの通信に必要なBluetoothSocket(失敗時はnull)
	 * @param isClient (自分がクライアントかどうか)
	 */
	public void didBlueToothConnectionResultReceiver(BluetoothSocket result, boolean isClient, boolean isCancel);
}
