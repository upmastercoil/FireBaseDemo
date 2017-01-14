package tech.android.tcmp13.firebasedemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;

import java.util.ArrayList;
import java.util.List;

import tech.android.tcmp13.firebasedemo.firebase.FirebaseManager;
import tech.android.tcmp13.firebasedemo.message.control.MessagesAdapter;
import tech.android.tcmp13.firebasedemo.message.model.Message;

public class MainActivity extends AppCompatActivity implements UiCallbacks {

	private static final String TAG = MainActivity.class.getSimpleName();

	public static final int SIGN_IN_REQUEST_CODE = 1;
	public static final int PHOTO_PICKER_REQUEST_CODE = 2;

	private FirebaseManager firebaseManager;

	private RecyclerView messageRecyclerView;
	private MessagesAdapter messagesAdapter;
	private ProgressBar progressBar;
	private ImageButton photoPickerButton;
	private EditText messageEditText;
	private Button sendButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		firebaseManager = FirebaseManager.newInstance(this);
		setupViews();
	}

	private void onSignedOutCleanup() {

		messagesAdapter.clear();
	}

	private void setupViews() {

		initViews();
		setupRecyclerView();
		setupProgressBar();
		setupPhotoPickerButton();
		setupMessageEditText();
		setupSendButton();
	}

	private void initViews() {

		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		messageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
		photoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
		messageEditText = (EditText) findViewById(R.id.messageEditText);
		sendButton = (Button) findViewById(R.id.sendButton);
	}

	private void setupRecyclerView() {

		messageRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));
		messagesAdapter = new MessagesAdapter();
		messageRecyclerView.setAdapter(messagesAdapter);
	}

	private void setupPhotoPickerButton() {

		photoPickerButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {

				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("image/jpeg");
				intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
				startActivityForResult(intent, PHOTO_PICKER_REQUEST_CODE);
			}
		});
	}

	private void setupMessageEditText() {

		messageEditText.addTextChangedListener(new NewMessageTextWatcher());
	}

	private void setupProgressBar() {

		progressBar.setVisibility(ProgressBar.INVISIBLE);
	}

	private void setupSendButton() {

		sendButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {

				firebaseManager.sendTextMessage(messageEditText.getText().toString());
				// Clear input box
				messageEditText.setText("");
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
			case SIGN_IN_REQUEST_CODE:
				if (resultCode == RESULT_OK) {
					Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
				} else if (resultCode == RESULT_CANCELED) {
					Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show();
					//If the user cancelled signed in quite the app and avoid entering a loop of exiting the login screen, reentering the main activity,
					// and than due to unauthorised user pop up the login screen again.
					finish();
				}
				break;
			case PHOTO_PICKER_REQUEST_CODE:
				firebaseManager.sendPhotoMessage(this, data.getData());
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	protected void onPause() {

		super.onPause();
		firebaseManager.clearUserInfo();
	}

	@Override
	protected void onResume() {

		super.onResume();
		firebaseManager.setAuthorizationListener();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.sign_out_menu) {
			AuthUI.getInstance().signOut(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void messageMaxLengthChanged(Long maxLength) {

		messageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength.intValue())});
	}

	@Override
	public void dataShouldClear() {

		messagesAdapter.clear();
	}

	@Override
	public void presentSignInUi() {

		Intent intent = AuthUI.getInstance(). //Firebase UI Auth singleton
				createSignInIntentBuilder(). //Generate an intent for the sign in/login screen
				setIsSmartLockEnabled(false). //Set whether the user should stay logged in also in other devices
				setProviders(getIdpConfigs()). //Set which login providers will be presented to the user
				setTheme(R.style.MyFirebase). //Make the login screen use your app's theme instead of the default theme
				build();
		startActivityForResult(intent, SIGN_IN_REQUEST_CODE);
	}

	@NonNull
	private List<AuthUI.IdpConfig> getIdpConfigs() {

		List<AuthUI.IdpConfig> idpConfigs = new ArrayList<>(3);
		idpConfigs.add(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build());
		idpConfigs.add(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build());
		idpConfigs.add(new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build());
		return idpConfigs;
	}

	@Override
	public void messageFetchedFromRemoteDatabase(Message message) {

		messagesAdapter.insert(message);
		messageRecyclerView.smoothScrollToPosition(0);
	}

	private class NewMessageTextWatcher implements TextWatcher {

		@Override
		public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

		}

		@Override
		public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

			if (charSequence.toString().trim().length() > 0)
				sendButton.setEnabled(true);
			else
				sendButton.setEnabled(false);
		}

		@Override
		public void afterTextChanged(Editable editable) {

		}
	}
}
