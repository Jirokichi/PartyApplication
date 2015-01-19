package jp.kdy.bluetooth.connection;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;

import static jp.kdy.partyapp.KYUtils.*;
public class BlueToothConnectionForSendTask extends BlueToothConnectionTask {

	private BluetoothDevice mDevice;
	
	public BlueToothConnectionForSendTask(Context context, BluetoothAdapter btAdapter, BlueToothConnectionResultReceiver receiver, String title, String message, BluetoothDevice device) {
		super(context, btAdapter, receiver, title, message);
		mDevice = device;
	}

	@Override
	protected BluetoothSocket doInBackground(Object... params) {
		BluetoothSocket bSocket = null;
		try{
            //自デバイスのBluetoothクライアントソケットの取得
			log("start getting client socket");
			bSocket = mDevice.createRfcommSocketToServiceRecord(TECHBOOSTER_BTSAMPLE_UUID);
            //サーバー側に接続要求
			log("start connecting server");
			bSocket.connect();
			log("end connecting server - success");
		}catch(IOException e){
            e.printStackTrace();
            if(bSocket !=null){
				try {
					bSocket.close();
					bSocket = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
            }
        }
		log("doInBackgroundClient end - " + bSocket);
		return bSocket;
	}

	@Override
	protected boolean isClient() {
		return true;
	}

	/**
	 * プログレスダイアログのキャンセル時に実施しなければならない処理
	 */
	@Override
	public void onDialogCancel() {
		// 親は待ち状態にはならず、閉じる必要のあるインスタンスはないため、特になにもしない
	}
}
