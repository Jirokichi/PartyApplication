package jp.kdy.util;

import java.util.EventListener;

public interface MyDialogListener extends EventListener {
	 /**
     * okボタンが押されたイベントを通知する
     */
    public void onPositiveClick(String message);

    /**
     * 画面に関わらず、
     * cancelボタンが押されたイベントを通知する
     */
    public void onNegativeClick();
}
