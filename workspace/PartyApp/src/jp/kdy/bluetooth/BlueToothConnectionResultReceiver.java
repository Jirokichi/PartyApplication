package jp.kdy.bluetooth;

import jp.kdy.bluetooth.InterChangeTask.BlueToothResult;
import android.bluetooth.BluetoothSocket;

public interface BlueToothConnectionResultReceiver {
	/**
	 * �N���C�A���g�ƃT�[�o�[�ڑ��v���̌��ʂ�Ԃ����\�b�h
	 * @param result �T�[�o�[�Ƃ̒ʐM�ɕK�v��BluetoothSocket(���s����null)
	 * @param isClient (�������N���C�A���g���ǂ���)
	 */
	public void didBlueToothConnectionResultReceiver(BluetoothSocket result, boolean isClient, boolean isCancel);
}
