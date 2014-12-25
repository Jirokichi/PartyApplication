package jp.kdy.util;

import java.io.Serializable;

import jp.kdy.partyapp.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.Toast;

// 参考サイト:http://d.hatena.ne.jp/sakura_bird1/20130207/1360193574
public class MyFragmentDialog extends DialogFragment {
	protected static final String TAG = "MyFragmentDialog";

	// ダイアログタイプ用パラメー保存キー
	private static String bundle_key_dialog_type = "DIALOG_TYPE";

	// ツイート用パラメー保存キー
	private static String bundle_key_title = "TITLE";
	private static String bundle_key_message = "MESSAGE";
	private static String bundle_key_listener = "LISTENER";

	// リスナー
	private MyDialogListener listener; // リスナー用の変数です
    public interface MyDialogListener extends Serializable{
        public void onClicked(DialogInterface dialog, int which, String tag);
    }
    public void removeListener(){
    	listener = null;
    }
    final MyFragmentDialog mDialog = this;
    DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener(){
        @Override
        public void onClick(DialogInterface dialog, int which){
        	log("");
        	listener.onClicked(dialog, which, mDialog.getTag());
        }
     };
    
	// ダイアログタイプ
	public enum TYPE {
		JUST_CONFIRMATION_DIALOG, NORMAL_DIALOG, LIST_DIALOG, PROGRESS_DIALOG
	};
	

	// リストダイアログのパラメータ
	private String[] mItems = null;
	DialogInterface.OnClickListener mListListener;

	/**
	 * リストの表示文字とリスナー登録
	 * 
	 * @param items
	 * @param listListener
	 */
	public void setListParameter(final String[] items, DialogInterface.OnClickListener listListener) {
		this.mItems = items;
		this.mListListener = listListener;
	}

	/**
	 * 確認用のダイアログを作成する。作成されるダイアログはボタンを一つした持たず、onNegativeClickのみ利用する。
	 * 
	 * @param title
	 *            　タイトル
	 * @param diaogMessage
	 *            　メッセージ
	 * @param myListener
	 *   			MyDialogListener
	 * @return
	 */
	public static MyFragmentDialog newInstanceForJustConfirmationDilog(String title, String diaogMessage, MyDialogListener myListener) {
		MyFragmentDialog frag = new MyFragmentDialog();
		Bundle bundle = new Bundle();
		bundle.putString(bundle_key_dialog_type, TYPE.JUST_CONFIRMATION_DIALOG.name());
		bundle.putString(bundle_key_title, title);
		bundle.putString(bundle_key_message, diaogMessage);
		bundle.putSerializable(bundle_key_listener, myListener);
		frag.setArguments(bundle);
		return frag;
	}

	/**
	 * ダイアログを作成する。作成されるダイアログはボタンを2つもつ
	 * 
	 * @param title
	 *            　タイトル
	 * @param diaogMessage
	 *            　メッセージ
	 * @return
	 */
	public static MyFragmentDialog newInstanceForNormalDilog(String title, String diaogMessage, MyDialogListener myListener) {
		MyFragmentDialog frag = new MyFragmentDialog();
		Bundle bundle = new Bundle();
		bundle.putString(bundle_key_dialog_type, TYPE.NORMAL_DIALOG.name());
		bundle.putString(bundle_key_title, title);
		bundle.putString(bundle_key_message, diaogMessage);
		bundle.putSerializable(bundle_key_listener, myListener);
		frag.setArguments(bundle);
		return frag;
	}

	/**
	 * リストがあるダイアログを作成する。
	 * 
	 * @param title
	 *            　タイトル
	 * @param diaogMessage
	 *            　メッセージ
	 * @return
	 */
	public static MyFragmentDialog newInstanceForListDilog(String title, String diaogMessage) {
		MyFragmentDialog frag = new MyFragmentDialog();
		Bundle bundle = new Bundle();
		bundle.putString(bundle_key_dialog_type, TYPE.LIST_DIALOG.name());
		bundle.putString(bundle_key_title, title);
		bundle.putString(bundle_key_message, diaogMessage);
		frag.setArguments(bundle);
		return frag;
	}
	
	/**
	 * プログレスダイアログを作成する。作成されるダイアログはキャンセルボタンだけをもつ
	 * 
	 * @param title
	 *            　タイトル
	 * @param diaogMessage
	 *            　メッセージ
	 * @return
	 */
	public static MyFragmentDialog newInstanceForProgressDilog(String title, String diaogMessage, MyDialogListener myListener) {
		MyFragmentDialog frag = new MyFragmentDialog();
		Bundle bundle = new Bundle();
		bundle.putString(bundle_key_dialog_type, TYPE.PROGRESS_DIALOG.name());
		bundle.putString(bundle_key_title, title);
		bundle.putString(bundle_key_message, diaogMessage);
		bundle.putSerializable(bundle_key_listener, myListener);
		frag.setArguments(bundle);
		return frag;
	}

	/**
	 * フラグメントのインスタンス作成後に、show(getSupportFragmentManager(), "list_dialog")を実施すると呼び出される
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		TYPE dialogType = TYPE.valueOf(getArguments().getString(bundle_key_dialog_type));
		switch (dialogType) {
		case NORMAL_DIALOG:
			return createNormalDialog(savedInstanceState);
		case JUST_CONFIRMATION_DIALOG:
			return createConfirmDialog(savedInstanceState);
		case LIST_DIALOG:
			return createListDialog(savedInstanceState);
		case PROGRESS_DIALOG:
			return createProgressDialog(savedInstanceState);
		default:
			return null;
		}
	}
	
	/**
	 * ボタン２つのダイアログ
	 * 
	 * @param savedInstanceState
	 * @return
	 */
	private Dialog createNormalDialog(Bundle savedInstanceState) {
		String title = getArguments().getString(bundle_key_title);
		String dialogMessage = getArguments().getString(bundle_key_message);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title);
		builder.setMessage(dialogMessage);
		// リスナーを設定
        listener = (MyDialogListener)getArguments().getSerializable(bundle_key_listener);
		builder.setPositiveButton(getString(R.string.Yes),dialogListener);
		builder.setNegativeButton(getString(R.string.No), dialogListener);
		
		return builder.create();
	}

	/**
	 * ボタンが一つだけの確認ダイアログ
	 * 
	 * @param savedInstanceState
	 * @return
	 */
	private Dialog createConfirmDialog(Bundle savedInstanceState) {
		String title = getArguments().getString(bundle_key_title);
		String dialogMessage = getArguments().getString(bundle_key_message);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title);
		builder.setMessage(dialogMessage);
		// リスナーを設定
        listener = (MyDialogListener)getArguments().getSerializable(bundle_key_listener);
		builder.setPositiveButton(getString(R.string.Yes), dialogListener);
		return builder.create();
	}
	
	/**
	 * キャンセルボタンが一つだけのプログレスダイアログ(処理中)
	 * 
	 * @param savedInstanceState
	 * @return
	 */
	private Dialog createProgressDialog(Bundle savedInstanceState) {
		String title = getArguments().getString(bundle_key_title);
		String dialogMessage = getArguments().getString(bundle_key_message);
		
		ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setTitle(title);
        progressDialog.setMessage(dialogMessage);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCanceledOnTouchOutside(false);
        setCancelable(false);
        listener = (MyDialogListener)getArguments().getSerializable(bundle_key_listener);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.Cancel), dialogListener);
        return progressDialog;
	}

	/**
	 * リストダイアログ
	 * 
	 * @param savedInstanceState
	 * @return
	 */
	private Dialog createListDialog(Bundle savedInstanceState) {
		String title = getArguments().getString(bundle_key_title);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title);
		if (this.mListListener != null) {
			builder.setItems(mItems, this.mListListener);
		}
		return builder.create();
	}

	private void log(String message) {
		Log.d(TAG, message);
	}
}