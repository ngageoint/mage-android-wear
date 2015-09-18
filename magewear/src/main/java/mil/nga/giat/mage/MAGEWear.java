package mil.nga.giat.mage;

import android.app.Application;

import mil.nga.giat.chronostouch.pipe.DataManager;

public class MAGEWear extends Application {
	DataManager dataManager;

	@Override
	public void onCreate() {
		super.onCreate();
		startWearConnection();
	}

	private void startWearConnection() {
		dataManager = DataManager.getInstance(getApplicationContext());
	}
}
