package jp.kdy.bluetooth;

import jp.kdy.bluetooth.communication.CommunicationTask.BlueToothResult;



public interface BlueToothMessageResultReceiver {
	/**
	 * データ通信の結果
	 * @param result
	 */
	public void didBlueToothMessageResultReceiver(BlueToothResult result);
}
