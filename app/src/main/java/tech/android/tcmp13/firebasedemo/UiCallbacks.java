package tech.android.tcmp13.firebasedemo;

import tech.android.tcmp13.firebasedemo.message.model.Message;

/**
 * Created by noynngrisaru on 13/01/2017.
 */

public interface UiCallbacks {

	void messageMaxLengthChanged(Long maxLength);

	void dataShouldClear();

	void presentSignInUi();

	void messageFetchedFromRemoteDatabase(Message message);
}
