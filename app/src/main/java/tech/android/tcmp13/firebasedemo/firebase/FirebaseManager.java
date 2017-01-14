package tech.android.tcmp13.firebasedemo.firebase;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

import tech.android.tcmp13.firebasedemo.BuildConfig;
import tech.android.tcmp13.firebasedemo.MainActivity;
import tech.android.tcmp13.firebasedemo.UiCallbacks;
import tech.android.tcmp13.firebasedemo.message.model.Message;

/**
 * Created by noynngrisaru on 11/01/2017.
 */
public class FirebaseManager {

	private static final String LOG_TAG = MainActivity.class.getSimpleName();
	public static final String ANONYMOUS = "anonymous";
	public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

	private static FirebaseManager firebaseManager;

	public static FirebaseManager newInstance(UiCallbacks uiCallbacks) {

		if (firebaseManager == null)
			firebaseManager = new FirebaseManager();
		firebaseManager.uiCallbacks = uiCallbacks;
		return firebaseManager;
	}

	private UiCallbacks uiCallbacks;
	private String username;

	private FirebaseDatabase firebaseDatabase;
	private DatabaseReference messagesDatabaseReference;
	private ChildEventListener childEventListener;
	private FirebaseAuth firebaseAuth;
	private FirebaseAuth.AuthStateListener authStateListener;
	private FirebaseStorage firebaseStorage;
	private StorageReference chatPhotosStorageReference;
	private FirebaseRemoteConfig firebaseRemoteConfig;

	private FirebaseManager() {

		setupRealtimeDatabase();
		setupAuthentication();
		setupRemoteStorage();
		setupRemoteConfig();
	}

	private void setupRealtimeDatabase() {

		//Get a reference to the whole realtime database
		firebaseDatabase = FirebaseDatabase.getInstance();
		//Get a reference to the specific node that is relevant to this user.
		messagesDatabaseReference = firebaseDatabase.getReference().child(FirebaseDatabaseConstants.MESSAGES);
	}

	private void setupAuthentication() {

		username = ANONYMOUS;
		firebaseAuth = FirebaseAuth.getInstance();
		authStateListener = new FirebaseDemoAuthStateListener();
	}

	/**
	 * Remote config allows you to store functionality parameters at a remote location and update them on the fly, without the need to release an update of your app.
	 */
	private void setupRemoteConfig() {

		//Get a reference to the project's remote configs
		firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
		//Turn the remote config's developer mode on/off -> use developer mode to enable frequent refresh of the local cache.
		//BuildConfig.DEBUG changes whether the apk is signed as production APK or debug APK.
		FirebaseRemoteConfigSettings remoteConfigSettings = new FirebaseRemoteConfigSettings.Builder().setDeveloperModeEnabled(BuildConfig.DEBUG).build();
		firebaseRemoteConfig.setConfigSettings(remoteConfigSettings);
		setupDefaultCache();
		fetchConfig();
	}

	/**
	 * Setup the local cache with a default value in case of a failure to sync with the remote values.
	 */
	private void setupDefaultCache() {

		Map<String, Object> defaultConfigMap = new HashMap<>();
		defaultConfigMap.put(FirebaseDatabaseConstants.TEXT_MESSAGE_LENGTH_KEY, DEFAULT_MSG_LENGTH_LIMIT);
		firebaseRemoteConfig.setDefaults(defaultConfigMap);
	}

	/**
	 * Fetch the remote configs. First make a request to get the parameters and than use listeners to get the actual values.
	 */
	private void fetchConfig() {

		//Set the cache's timeout. In developer mode the cache should refresh frequently as values are prone to change,
		//but in production the values will change less frequently so save calls to the remote values and let the cache be saved for a longer period.
		long cacheExpiration = 3600;
		if (firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled())
			cacheExpiration = 0;
		firebaseRemoteConfig.fetch(cacheExpiration).addOnSuccessListener(new OnSuccessListener<Void>() {

			@Override
			public void onSuccess(Void aVoid) {

				//Flag the remote configs that the values were fetched successfully and the local cache is active and up to date.
				firebaseRemoteConfig.activateFetched();
				applyRetrievedLengthLimit();
			}
		}).addOnFailureListener(new OnFailureListener() {

			@Override
			public void onFailure(@NonNull Exception e) {

				Log.w(LOG_TAG, "Err fec con", e);
				applyRetrievedLengthLimit();
			}
		});
	}

	private void applyRetrievedLengthLimit() {

		//The value will be set to the current value on the local cache, whether it's the updated remote value or the default local value (depends on the success of the fetch call).
		Long maxLength = firebaseRemoteConfig.getLong(FirebaseDatabaseConstants.TEXT_MESSAGE_LENGTH_KEY);
		uiCallbacks.messageMaxLengthChanged(maxLength);
	}

	/**
	 * Used to store the user's files on a secure remote storage (same concept as the realtime database, but for files instead of
	 */
	private void setupRemoteStorage() {

		firebaseStorage = FirebaseStorage.getInstance();
		chatPhotosStorageReference = firebaseStorage.getReference().child(FirebaseDatabaseConstants.CHAT_PHOTOS);
	}

	private void onSignedOutCleanup() {

		username = ANONYMOUS;
		uiCallbacks.dataShouldClear();
	}

	private void onSignedInInitialized(String currentUser) {

		username = currentUser;
		attachDatabaseReadListener();
	}

	private void attachDatabaseReadListener() {

		if (childEventListener != null)
			return;
		childEventListener = new MessagesChildEventListener();
		messagesDatabaseReference.addChildEventListener(childEventListener);
	}

	private void detachDatabaseReadListener() {

		if (childEventListener == null)
			return;
		messagesDatabaseReference.removeEventListener(childEventListener);
		childEventListener = null;
	}

	public void sendTextMessage(String text) {

		Message message = new Message(text, username, null);
		messagesDatabaseReference.push().setValue(message);
	}

	public void sendPhotoMessage(Activity activity, Uri selectedImageUri) {

		//Get a reference to the specific photos storage
		StorageReference photoReference = chatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
		UploadTask uploadTask = photoReference.putFile(selectedImageUri);
		uploadTask.addOnSuccessListener(activity, new OnSuccessListener<UploadTask.TaskSnapshot>() {

			@Override
			public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

				//After the image is stored, get the url pointing at the remote photo
				Uri downloadUrl = taskSnapshot.getDownloadUrl();
				if (downloadUrl == null)
					return;
				Message message = new Message(null, username, downloadUrl.toString());
				messagesDatabaseReference.push().setValue(message);
			}
		});
	}

	public void clearUserInfo() {

		if (authStateListener != null)
			firebaseAuth.removeAuthStateListener(authStateListener);
		detachDatabaseReadListener();
		uiCallbacks.dataShouldClear();
	}

	public void setAuthorizationListener() {

		firebaseAuth.addAuthStateListener(authStateListener);
	}

	private class FirebaseDemoAuthStateListener implements FirebaseAuth.AuthStateListener {

		@Override
		public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

			//Whenever the user changes the signed in status check if he sign in/out
			FirebaseUser currentUser = firebaseAuth.getCurrentUser();
			if (currentUser == null) {
				//The user signed out, Clear the data and prevent the user from watching MainActivity that contains the chat's info by always starting FirebaseUi's login screen
				onSignedOutCleanup();
				//Generate an intent directed at the login screen of the Firebase UI
				uiCallbacks.presentSignInUi();
			} else {
				onSignedInInitialized(currentUser.getDisplayName());
			}
		}
	}

	private class MessagesChildEventListener implements ChildEventListener {


		@Override
		public void onChildAdded(DataSnapshot dataSnapshot, String s) {

			Message message = dataSnapshot.getValue(Message.class);
			uiCallbacks.messageFetchedFromRemoteDatabase(message);
		}

		@Override
		public void onChildChanged(DataSnapshot dataSnapshot, String s) {

		}

		@Override
		public void onChildRemoved(DataSnapshot dataSnapshot) {

		}

		@Override
		public void onChildMoved(DataSnapshot dataSnapshot, String s) {

		}

		@Override
		public void onCancelled(DatabaseError databaseError) {

		}
	}
}
