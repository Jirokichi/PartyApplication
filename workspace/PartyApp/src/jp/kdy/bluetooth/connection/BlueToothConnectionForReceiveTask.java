package jp.kdy.bluetooth.connection;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import static jp.kdy.partyapp.KYUtils.*;


public class BlueToothConnectionForReceiveTask extends BlueToothConnectionTask {

    //サーバ側の処理
    private BluetoothServerSocket mServSock = null;
    
	public BlueToothConnectionForReceiveTask(Context context, BluetoothAdapter btAdapter, BlueToothConnectionResultReceiver receiver, String title, String message) {
		super(context, btAdapter, receiver, title, message);
	}

	@Override
	protected BluetoothSocket doInBackground(Object... params) {
		BluetoothSocket bSocket = null;
		try{
            //自分のデバイスのBluetoothサーバーソケットの取得
			log("start getting server socket:"+myBlueToothAdapter.getState());
			mServSock = myBlueToothAdapter.listenUsingRfcommWithServiceRecord("BlueToothSample03", TECHBOOSTER_BTSAMPLE_UUID);
			
			//クライアント側からの接続要求待ち。ソケットが返される。
			log(String.format("accepting socket from client(mServSock:%s)...", mServSock));
			bSocket = mServSock.accept();
            log("accepted");
		}catch(IOException e){
            e.printStackTrace();
        }finally{
            /*
             *  BluetoothSocketを閉じた際に、BluetoothServerSocketは閉じられないため、ここで閉じてあげなければならない
             *  なお、プログレスダイアログなどのキャンセルで閉じた場合はこの処理は実施されないため、onCancelで実装してあげる必要あり
             */
            try {
				if(mServSock != null)
					mServSock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
            mServSock = null;
        }

		log("doInBackgroundServer end - " + bSocket);
		return bSocket;
	}

	@Override
	protected boolean isClient() {
		return false;
	}

	/**
	 * プログレスダイアログのキャンセル時に実施しなければならない処理
	 */
	@Override
	public void onDialogCancel() {
		try {
			if(mServSock != null){
				mServSock.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			mServSock = null;
		}
	}
}
