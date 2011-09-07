package net.cachapa.remotegallery;

import net.cachapa.remotegallery.ssh.LogCallback;
import net.cachapa.remotegallery.ssh.Ssh;
import net.cachapa.remotegallery.ssh.SshOutputReader;
import net.cachapa.remotegallery.util.Database;
import net.cachapa.remotegallery.util.ServerConf;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class ConnectionTest extends Activity implements OnClickListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connection_test);
		TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
		
		if (titleTextView != null) {
			titleTextView.setText(R.string.connection_test);
		}
		
		Intent intent = getIntent();
		long serverConfId = intent.getLongExtra("id", -1);
		if (serverConfId >= 0) {
			ServerConf serverConf = Database.getInstance(this).getServerConf(serverConfId);
			new SshTester().execute(serverConf);
		}
	}
	
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.homeButton:
			// Home icon in Action Bar clicked; go home without passing go
            Intent intent = new Intent(this, Main.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
			break;
		}
	}
	
	
	private class SshTester extends AsyncTask<ServerConf, String, Integer> implements LogCallback {
		@Override
		protected Integer doInBackground(ServerConf... serverConfs) {
			SshOutputReader ssh = new SshOutputReader(ConnectionTest.this, serverConfs[0]);
			ssh.execute("echo 'Connection OK'", this);
			return null;
		}
		
		@Override
		public void log(Ssh ssh, String log) {
			publishProgress(log);
		}
		
		@Override
		public void logError(Ssh ssh, String log) {
			publishProgress(null, log);
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			if (values[0] != null) {
				((TextView) findViewById(R.id.logTextView)).append(Html.fromHtml("<font color='#00ff00'>" + values[0] + "</font><br/><br/>"));
			}
			else {
				((TextView) findViewById(R.id.logTextView)).append(Html.fromHtml("<font color='yellow'>" + values[1] + "</font><br/><br/>"));
			}
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			findViewById(R.id.progressBar).setVisibility(View.GONE);
		}
	}
}
