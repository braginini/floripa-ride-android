/*
 * Copyright 2013 Mikhail Bragin
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.floriparide.android.tasks;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import org.opentripplanner.api.ws.GraphMetadata;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.floriparide.android.OTPApp;
import com.floriparide.android.R;
import com.floriparide.android.listeners.MetadataRequestCompleteListener;

/**
 * @author Khoa Tran
 *
 */

public class MetadataRequest extends AsyncTask<String, Integer, GraphMetadata> {
	private GraphMetadata metadata;
	private static final String TAG = "OTP";
	private ProgressDialog progressDialog;
	private WeakReference<Activity> activity;
	private Context context;
	
	private MetadataRequestCompleteListener callback;
	
	private static ObjectMapper mapper = null;

	public MetadataRequest(WeakReference<Activity> activity, Context context, MetadataRequestCompleteListener callback) {
		this.activity = activity;
		this.context = context;
		this.callback = callback;
		if (activity.get() != null){
			progressDialog = new ProgressDialog(activity.get());
		}	
	}

	protected void onPreExecute() {
		if (activity.get() != null){
			progressDialog.setIndeterminate(true);
	        progressDialog.setCancelable(true);
			progressDialog = ProgressDialog.show(activity.get(),"",
					context.getResources().getString(R.string.metadata_request_progress), true);
		}
	}

	protected GraphMetadata doInBackground(String... reqs) {
		int count = reqs.length;
		for (int i = 0; i < count; i++) {
			String serverURL = reqs[0];
			metadata = requestMetadata(serverURL);
			// publishProgress((int) ((i / (float) count) * 100));
		}
		return metadata;
	}

	protected void onPostExecute(GraphMetadata metadata) {
		if (activity.get() != null){
			try{
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
			}catch(Exception e){
				Log.e(TAG, "Error in Metadata Request PostExecute dismissing dialog: " + e);
			}
		}		

		if (metadata != null) {
			Toast.makeText(context, context.getResources().getString(R.string.metadata_request_successful), Toast.LENGTH_SHORT).show();
			callback.onMetadataRequestComplete(metadata);
		} else {
			Toast.makeText(context, context.getResources().getString(R.string.info_server_error), Toast.LENGTH_SHORT).show();

			Log.e(TAG, "No metadata!");
		}
	}
	
	private GraphMetadata requestMetadata(String serverURL) {
		String res = OTPApp.METADATA_LOCATION;

		String u = serverURL + res;

		Log.d(TAG, "URL: " + u);
			
		HttpURLConnection urlConnection = null;
		URL url = null;
		GraphMetadata plan = null;

		try {

			url = new URL(u);

			disableConnectionReuseIfNecessary(); // For bugs in HttpURLConnection pre-Froyo

			// Serializer serializer = new Persister();
			
			if(mapper == null){
				mapper = new ObjectMapper();
			}

			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestProperty("Accept", "application/json");
			urlConnection.setConnectTimeout(context.getResources().getInteger(R.integer.connection_timeout));
			urlConnection.setReadTimeout(context.getResources().getInteger(R.integer.socket_timeout));

			// plan = serializer.read(Response.class, result);
			plan = mapper.readValue(urlConnection.getInputStream(),
					GraphMetadata.class);
			
		} catch (IOException e) {
			Log.e(TAG, "Error fetching JSON or XML: " + e);
			e.printStackTrace();
			// Reset timestamps to show there was an error
			// requestStartTime = 0;
			// requestEndTime = 0;
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}

		return plan;
	}
	
	/**
	 * Disable HTTP connection reuse which was buggy pre-froyo
	 */
	private void disableConnectionReuseIfNecessary() {
		//if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {  //Should change to this once we update to Android 4.1 SDK
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1) {
			System.setProperty("http.keepAlive", "false");
		}
	}
}