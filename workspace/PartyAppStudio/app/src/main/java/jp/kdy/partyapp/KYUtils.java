package jp.kdy.partyapp;

import android.util.Log;

public class KYUtils {

	public KYUtils() {
		// TODO Auto-generated constructor stub
	}
	
	final public static boolean DEBUG = false;

	
	public static final void log(String message) {  
	    StackTraceElement ste = Thread.currentThread().getStackTrace()[3];  
	    String className = ste.getClassName();  
	    className = className.substring(className.lastIndexOf(".") + 1);  
	    String methodName = ste.getMethodName();  
	    int lineNum = ste.getLineNumber(); 
	    String logText = String.format("%s(%s):%s", methodName, lineNum, message);
	    Log.d(className, logText);  
	}  
}
