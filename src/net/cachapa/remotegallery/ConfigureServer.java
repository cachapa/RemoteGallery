package net.cachapa.remotegallery;

import net.cachapa.remotegallery.util.Database;
import net.cachapa.remotegallery.util.ServerConf;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

public class ConfigureServer extends Activity implements OnClickListener, TextWatcher {
	private EditText nameEditText, addressEditText, portEditText, usernameEditText, keyPathEditText, remotePathEditText;
	private long serverConfId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configure_server);
		
		TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
		if (titleTextView != null) {
			titleTextView.setText(R.string.server_configuration);
		}
		
		nameEditText = (EditText)findViewById(R.id.name);
		addressEditText = (EditText)findViewById(R.id.address);
		portEditText = (EditText)findViewById(R.id.port);
		usernameEditText = (EditText)findViewById(R.id.username);
		keyPathEditText = (EditText)findViewById(R.id.keyPath);
		remotePathEditText = (EditText)findViewById(R.id.remotePath);
		
		nameEditText.addTextChangedListener(this);
		addressEditText.addTextChangedListener(this);
		portEditText.addTextChangedListener(this);
		usernameEditText.addTextChangedListener(this);
		keyPathEditText.addTextChangedListener(this);
		remotePathEditText.addTextChangedListener(this);
		
		Intent intent = getIntent();
		serverConfId = intent.getLongExtra("id", -1);
		if (serverConfId >= 0) {
			ServerConf serverConf = Database.getInstance(this).getServerConf(serverConfId);
			
			nameEditText.setText(serverConf.name);
			addressEditText.setText(serverConf.address);
			portEditText.setText(String.valueOf(serverConf.port));
			usernameEditText.setText(serverConf.username);
			keyPathEditText.setText(serverConf.keyPath);
			remotePathEditText.setText(serverConf.remotePath);
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
			
		case R.id.browse:
			Intent localBrowserIntent = new Intent(this, net.cachapa.remotegallery.LocalBrowser.class);
			localBrowserIntent.putExtra("path", "");
			startActivityForResult(localBrowserIntent, 1);
			break;
			
		case R.id.SaveButton:
			ServerConf serverConf = new ServerConf(
					nameEditText.getText().toString().trim(),
					addressEditText.getText().toString(),
					Integer.valueOf(portEditText.getText().toString()).intValue(),
					usernameEditText.getText().toString(),
					keyPathEditText.getText().toString(),
					remotePathEditText.getText().toString()
			);
			
			Database database = Database.getInstance(this);
			if (serverConfId == -1) {
				// Create a new configuration
				database.insertServerConf(serverConf);
			}
			else {
				// Update an existing configuration
				serverConf.id = serverConfId;
				database.updateServerConf(serverConf);
			}
			
			finish();
			break;
			
		case R.id.CancelButton:
			finish();
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && requestCode == 1) {
			((TextView)findViewById(R.id.keyPath)).setText(data.getStringExtra("path"));
		}
	}
	
	@Override
	public void afterTextChanged(Editable s) {
		findViewById(R.id.SaveButton).setEnabled(
				addressEditText.getText().length() > 0 &&
				portEditText.getText().length() > 0 &&
				usernameEditText.getText().length() > 0 &&
				keyPathEditText.getText().length() > 0 &&
				remotePathEditText.getText().length() > 0
		);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// Not needed
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// Not needed
	}
}
