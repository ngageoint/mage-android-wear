package mil.nga.giat.mage;

import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.location.Location;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentActivity;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import mil.nga.giat.chronostouch.pipe.DataManager;

public class LandingActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, DelayedConfirmationView.DelayedConfirmationListener {

	private static final String OBSERVATION_PATH = "/observation";
	private static final String GESTURE_KEY = "gesture";
	private static final String DESCRIPTION_KEY = "description";

	private static final int SPEECH_REQUEST_CODE = 0;

	private GoogleMap mMap;

	private DelayedConfirmationView delayedConfirmationView;
	private GestureOverlayView mGestureOverlay;
	private Gesture mGesture;
	private String mDescription;

	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);

		// Set the layout. It only contains a SupportMapFragment and a DismissOverlay.
		setContentView(R.layout.activity_landing);

		mGestureOverlay = (GestureOverlayView) findViewById(R.id.gesture_layout);
		mGestureOverlay.setVisibility(View.INVISIBLE);

		delayedConfirmationView = (DelayedConfirmationView) findViewById(R.id.delayed_confirm);
		delayedConfirmationView.setVisibility(View.INVISIBLE);

		// Retrieve the containers for the root of the layout and the map. Margins will need to be
		// set on them to account for the system window insets.
		final FrameLayout topFrameLayout = (FrameLayout) findViewById(R.id.root_container);
		final FrameLayout mapFrameLayout = (FrameLayout) findViewById(R.id.map_container);

		// Set the system view insets on the containers when they become available.
		topFrameLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
			@Override
			public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
				// Call through to super implementation and apply insets
				insets = topFrameLayout.onApplyWindowInsets(insets);

				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mapFrameLayout.getLayoutParams();

				// Add Wearable insets to FrameLayout container holding map as margins
				params.setMargins(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
				mapFrameLayout.setLayoutParams(params);

				return insets;
			}
		});

		// Obtain the MapFragment and set the async listener to be notified when the map is ready.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		// Map is ready to be used.
		mMap = googleMap;
		mMap.setMyLocationEnabled(true);
		mMap.getUiSettings().setAllGesturesEnabled(true);
		mMap.getUiSettings().setCompassEnabled(true);
		mMap.getUiSettings().setMyLocationButtonEnabled(true);

		// Set the long click listener for custom gesture drawing.
		mMap.setOnMapLongClickListener(this);

		Location location = mMap.getMyLocation();
		if (location != null) {
			LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

			// Move the camera to show the marker.
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
		}
	}

	@Override
	public void onMapLongClick(LatLng latLng) {
		mGestureOverlay.cancelClearAnimation();
		mGestureOverlay.clear(true);
		mGestureOverlay.setBackgroundColor(getResources().getColor(R.color.transparent_white));
		mGestureOverlay.addOnGestureListener(new GestureOverlayView.OnGestureListener() {
			@Override
			public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {

			}

			@Override
			public void onGesture(GestureOverlayView overlay, MotionEvent event) {

			}

			@Override
			public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
				mGesture = overlay.getGesture();
				displaySpeechRecognizer();
			}

			@Override
			public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {

			}
		});
		mGestureOverlay.setEventsInterceptionEnabled(true);
		mGestureOverlay.setVisibility(View.VISIBLE);
	}

	@Override
	public void onTimerSelected(View v) {
		// Timer was canceled, don't do anything.
		v.setPressed(true);
		Intent intent = new Intent(this, ConfirmationActivity.class);
		intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
		intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.cancel));
		startActivity(intent);
		((DelayedConfirmationView) v).setListener(null);
		delayedConfirmationView.setVisibility(View.INVISIBLE);
		mGestureOverlay.setVisibility(View.INVISIBLE);
	}

	public void onStartTimer() {
		delayedConfirmationView.setTotalTimeMs(1000);
		delayedConfirmationView.setListener(this);
		delayedConfirmationView.setVisibility(View.VISIBLE);
		delayedConfirmationView.start();
	}

	@Override
	public void onTimerFinished(View v) {
		DataManager dataManager = DataManager.getInstance(getApplicationContext());
		dataManager.createMap(OBSERVATION_PATH);
		dataManager.addDataItem(OBSERVATION_PATH, GESTURE_KEY, mGesture);
		dataManager.addDataItem(OBSERVATION_PATH, DESCRIPTION_KEY, mDescription);
		dataManager.sendData();

		Intent intent = new Intent(this, ConfirmationActivity.class);
		intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
		intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.confirm));
		startActivity(intent);
		delayedConfirmationView.setVisibility(View.INVISIBLE);
		mGestureOverlay.setVisibility(View.INVISIBLE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && requestCode == SPEECH_REQUEST_CODE) {
			List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			mDescription = results.get(0);
			onStartTimer();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void displaySpeechRecognizer() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Give a short description.");
		// Start the activity, the intent will be populated with the speech text
		startActivityForResult(intent, SPEECH_REQUEST_CODE);
	}
}