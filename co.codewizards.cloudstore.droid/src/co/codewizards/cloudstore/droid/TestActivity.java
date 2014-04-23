package co.codewizards.cloudstore.droid;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.bouncycastle.crypto.RuntimeCryptoException;

import co.codewizards.cloudstore.core.util.ZipUtil;
import co.codewizards.cloudstore.droid.util.SystemUiHider;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import dalvik.system.DexClassLoader;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class TestActivity extends Activity {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_test);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.dummy_button).setOnTouchListener(
				mDelayHideTouchListener);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			doTest();
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	private void doTest() {
		// Before the secondary dex file can be processed by the DexClassLoader,
		// it has to be first copied from asset resource to a storage location.

		final ClassLoader defaultClassLoader = TestActivity.class.getClassLoader();

		final ClassLoader dataNucleusClassLoader = new AssetClassLoader(getApplicationContext(), new String[] {
			"co.codewizards.android.datanucleus.core.dex-4.0.0-m2.jar",
			"co.codewizards.android.datanucleus.rdbms.dex-4.0.0-m2.jar",
			"co.codewizards.android.datanucleus.jdo.dex-4.0.0-m2.jar"
		}, defaultClassLoader);

		Thread.currentThread().setContextClassLoader(dataNucleusClassLoader);

		final Properties persistenceProperties = new Properties();
		try {
			InputStream in = getAssets().open("test-persistence.properties");
			persistenceProperties.load(in);
			in.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		SQLiteDatabase db = getApplicationContext().openOrCreateDatabase("testDB", Context.MODE_PRIVATE, null);
		String databasePath = db.getPath();
		db.close();
		persistenceProperties.setProperty("javax.jdo.option.ConnectionURL", "jdbc:sqldroid:" + databasePath);
//		persistenceProperties.setProperty("javax.jdo.option.ConnectionURL", "jdbc:sqlite:" + databasePath);

//		try {
//			Class<?> implClass = Class.forName("org.datanucleus.api.jdo.JDOPersistenceManagerFactory", true, Thread.currentThread().getContextClassLoader());
//            Method m = implClass.getMethod("getPersistenceManagerFactory", Map.class, Map.class);
//            PersistenceManagerFactory pmf = 
//                (PersistenceManagerFactory) m.invoke(
//                    null, new Object[]{new HashMap<>(), persistenceProperties});
//
//			System.out.println("*********************************************************************");
//			System.out.println(pmf);
//			System.out.println("*********************************************************************");
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException(e);
//		}

		final PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(persistenceProperties, Thread.currentThread().getContextClassLoader());
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");

//		final String traceFilePath = new File(getDir("trace", MODE_WORLD_READABLE), "trace").getAbsolutePath();
//		System.out.println("TRACE: " + traceFilePath);
//		Debug.startMethodTracing(traceFilePath);

		System.out.println("*************************************************************************************");
		
		final PersistenceManager pm = pmf.getPersistenceManager();
		System.out.println("*************************************************************************************");
		System.out.println("PersistenceManager created!!!");
		System.out.println("*************************************************************************************");
		
		pm.getExtent(LocalRepository.class);
		System.out.println("*************************************************************************************");
		System.out.println("Entity LocalRepository initialised!!!");
		System.out.println("*************************************************************************************");

		LocalRepository localRepository = new LocalRepository();
		localRepository.setPrivateKey(new byte[10]);
		localRepository.setPublicKey(new byte[10]);
		localRepository.setRevision(9);
		Directory root = new Directory();
		root.setChanged(new Date());
		root.setLastModified(new Date());
		root.setLocalRevision(9);
		root.setName("/");
		root.setParent(null);
		localRepository.setRoot(root);
		localRepository.setChanged(new Date());
		pm.makePersistent(localRepository);

		System.out.println("*************************************************************************************");
		System.out.println("Entity LocalRepository persisted!!!");
		System.out.println("*************************************************************************************");

		@SuppressWarnings("unchecked")
		Collection<LocalRepository> localRepositories = (Collection<LocalRepository>) pm.newQuery(LocalRepository.class).execute();
		System.out.println("*************************************************************************************");
		System.out.println("Entity LocalRepository queried:");
		for (LocalRepository localRepository2 : localRepositories) {
			System.out.println("  * " + localRepository2);
		}

		pm.close();
		pmf.close();
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
		System.out.println("*************************************************************************************");
	}

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
}
