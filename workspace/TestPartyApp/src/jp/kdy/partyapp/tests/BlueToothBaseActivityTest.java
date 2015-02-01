package jp.kdy.partyapp.tests;

import jp.kdy.partyapp.BlueToothBaseActivity;
import jp.kdy.partyapp.R;
import android.test.ActivityInstrumentationTestCase2;

import static jp.kdy.partyapp.KYUtils.log;

public class BlueToothBaseActivityTest extends ActivityInstrumentationTestCase2<BlueToothBaseActivity> {

	BlueToothBaseActivity mBlueToothBaseActivity;
	public BlueToothBaseActivityTest() {
		super(BlueToothBaseActivity.class);
	}
	
	@Override
    protected void setUp() throws Exception {
        super.setUp();
        mBlueToothBaseActivity = getActivity();
    }
	
	public void testMyFirstTestTextView_labelText() {
		log("");
	    final boolean expected = false;
	    final boolean actual = false;
	    assertEquals(expected, actual);
	}

}
