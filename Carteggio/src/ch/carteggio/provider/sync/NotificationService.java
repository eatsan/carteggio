/*******************************************************************************
 * Copyright (c) 2014, Lorenzo Keller
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ch.carteggio.provider.sync;

import ch.carteggio.provider.CarteggioContract.Messages;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;

public class NotificationService extends IntentService {

	public NotificationService() {
		super("NotificationService");
	}

	private static final int ERROR_NOTIFICATION_ID = 1;	
	
	public static final String UPDATE_SENDING_STATE_ACTION = "ch.carteggio.provider.sync.NotificationService.UPDATE_SENDING_STATE_ACTION";
	public static final String UPDATE_RECEIVING_STATE_ACTION = "ch.carteggio.provider.sync.NotificationService.UPDATE_RECEIVING_STATE_ACTION";
	public static final String UPDATE_UNREAD_STATE_ACTION = "ch.carteggio.provider.sync.NotificationService.UPDATE_UNREAD_STATE_ACTION";

	public static final String FAILURE_EXTRA = "ch.carteggio.provider.sync.NotificationService.SUCCESS_EXTRA";
	
	private Observer mObserver;
	
	private boolean mSendFailure;
	private boolean mReceiveFailure;
	
	@Override
	public IBinder onBind(Intent intent) { 
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();	
		
		mObserver = new Observer();
		
		getContentResolver().registerContentObserver(Messages.CONTENT_URI, true, mObserver);
		
	}
	
	@Override
	public void onDestroy() {

		getContentResolver().unregisterContentObserver(mObserver);
		
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		
		if ( intent != null) {
			
			if ( UPDATE_RECEIVING_STATE_ACTION.equals(intent.getAction())) {
				
				mReceiveFailure = intent.getBooleanExtra(FAILURE_EXTRA, true);
				
			} else if ( UPDATE_SENDING_STATE_ACTION.equals(intent.getAction())) {				
				
				mSendFailure = intent.getBooleanExtra(FAILURE_EXTRA, true);
				
			}			
			
		}
		
		if ( mSendFailure || mReceiveFailure) {
			showFailureNotification();
		} else {
			hideFailureNotification();
		}
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {		
		super.onStartCommand(intent, flags, startId);
		
		// we want the service to be sticky to make sure we remember the send/receive state
		return START_STICKY;
	}

	private void hideFailureNotification() {
		
		NotificationManager mNotificationManager =
		        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		mNotificationManager.cancel(ERROR_NOTIFICATION_ID);
	}


	private void showFailureNotification() {

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		
		Notification.Builder mNotifyBuilder = new Notification.Builder(this)
		    .setContentTitle("Carteggio network error")		    
		    .setSmallIcon(android.R.drawable.stat_notify_error);
				
		if ( mSendFailure && mReceiveFailure) {
			mNotifyBuilder.setContentText("There was a problem while delivering and receiving messages");
		} else if ( mSendFailure ) {
			mNotifyBuilder.setContentText("There was a problem while delivering messages");
		} else if (mReceiveFailure) {
			mNotifyBuilder.setContentText("There was a problem while receiving messages");
		}
		
		mNotificationManager.notify(ERROR_NOTIFICATION_ID, mNotifyBuilder.getNotification());
		
	}

	private class Observer extends ContentObserver {

		public Observer() {
			super(new Handler());
		}

		@Override
		public void onChange(boolean selfChange) {
			
			// we send an intent here because we don't want to do database operations in the main thread
			Intent service = new Intent(NotificationService.this, NotificationService.class);			
			service.setAction(UPDATE_UNREAD_STATE_ACTION);
			
			startService(service);
		}
		
		
		
	}

	public static void setSendingError(Context c, boolean error) {
		
		Intent service = new Intent(c, NotificationService.class);			
		service.setAction(UPDATE_SENDING_STATE_ACTION);
		service.putExtra(FAILURE_EXTRA, error);
		
		c.startService(service);
		
	}
	
	public static void setReceivingError(Context c, boolean error) {
		
		Intent service = new Intent(c, NotificationService.class);			
		service.setAction(UPDATE_RECEIVING_STATE_ACTION);
		service.putExtra(FAILURE_EXTRA, error);
		
		c.startService(service);
		
	}
	
	
}