package jp.kdy.bluetooth;

import jp.kdy.bluetooth.InterChangeTask.BlueToothResult;


public interface BlueToothMessageResultReceiver {
	/**
	 * �f�[�^�ʐM�̌���
	 * @param result
	 */
	public void didBlueToothMessageResultReceiver(BlueToothResult result);
}
