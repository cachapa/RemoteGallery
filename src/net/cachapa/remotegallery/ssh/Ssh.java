package net.cachapa.remotegallery.ssh;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import net.cachapa.remotegallery.R;
import net.cachapa.remotegallery.util.ServerConf;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public abstract class Ssh {
	private static final String SSH_FILENAME = "ssh";
	
	protected Context context;
	protected ServerConf serverConf;
	private String sshPath;
	protected LogCallback logCallback;
	
	public Ssh(Context context, ServerConf serverConf) {
		this.context = context;
		this.serverConf = serverConf;
		sshPath = context.getFilesDir() + "/" + SSH_FILENAME;
		
		// Check if the binary is installed, and install if necessary
		File binary = new File(sshPath);
		if (!binary.exists()) {
			installSshBinary();
		}
	}
	
	public abstract void getDataStream(InputStream inputStream) throws IOException;
	
	public void execute(String remoteCommand, LogCallback logCallback) {
		this.logCallback = logCallback;
		
		String[] command = getCommand(serverConf, remoteCommand);
		String cmdStr = "ssh ";
		for (int i = 1; i < command.length; i++) {
			cmdStr += command[i] + " ";
		}
		logCallback.log(this, cmdStr);
		
		Process process;
		try {
			process = Runtime.getRuntime().exec(command);
			
			// Start a thread to read the error stream
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			new ErrorStreamReader().execute(errorReader);
			
			// Read the output stream on this thread
			getDataStream(process.getInputStream());
		} catch (IOException e) {
			logCallback.logError(this, e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
	private String[] getCommand(ServerConf serverConf, String command) {
		return new String[] {
				sshPath,
				serverConf.address,
				"-p", String.valueOf(serverConf.port),
				"-y", 	// accept the server's identity automatically
				"-l", serverConf.username,
				"-i", serverConf.keyPath,
				command
		};
	}
	
	private void installSshBinary() {
		try {
			// Copy the file to the app's private area
			InputStream sshInputStream = context.getResources().openRawResource(R.raw.ssh);
			OutputStream sshOutputStream = context.openFileOutput("ssh", Context.MODE_PRIVATE);
			byte[] buf = new byte[1024];
			int len;
			while ((len = sshInputStream.read(buf)) > 0){
				sshOutputStream.write(buf, 0, len);
			}
			sshInputStream.close();
			sshOutputStream.close();
			
			// Set the binary to executable
			Process process = Runtime.getRuntime().exec("chmod 744 " + sshPath);
			process.waitFor();
		} catch (Exception e) {
			Toast.makeText(context, "Couldn't install ssh binary: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}
	
	
	private class ErrorStreamReader extends AsyncTask<BufferedReader, String, Void> {
		@Override
		protected Void doInBackground(BufferedReader... readers) {
			int read;
			char[] buffer = new char[4096];
			String[] lines;
			try {
				while ((read = readers[0].read(buffer)) > 0) {
					lines = new String(buffer, 0, read).split("\n");
					for (String line : lines) {
						if (line.trim().length() > 0) {
							// Dropbear reminds us that he can't create .ssh because the
							// $HOME environment variable isn't set.
							// It's too hard to pass the $HOME, and .ssh isn't necessary
							// in any case, therefore, we simply ignore the warning.
							if (!line.toLowerCase().contains("warning: failed creating")) {
								// Dropbear shows us the fingerprint md5 on every operation
								// we simply ignore the warning.
								if (!line.toLowerCase().contains("fingerprint md5")) {
									publishProgress(line);
								}
							}
						}
					}
					buffer = new char[4096];
				}
				// Close the stream
				readers[0].close();
			} catch (Exception e) {
				publishProgress(e.getLocalizedMessage());
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			if (logCallback != null) {
				logCallback.logError(Ssh.this, values[0]);
			}
		}
	}
}
