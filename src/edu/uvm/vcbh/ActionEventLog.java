package edu.uvm.vcbh;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;

public class ActionEventLog implements Closeable, ActionListener
{
	protected long startTime = System.currentTimeMillis();
	protected BufferedWriter log = null;
	
	public ActionEventLog(String sessionID) throws IOException
	{
		File logFile = new File(String.format("%s.csv", sessionID));
		if (logFile.exists())
		{
			int nButton = JOptionPane.showOptionDialog(
					null,
					"File already exists, overwrite it?",
					"Overwrite File?",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE, null, null, null);
			if (0 != nButton || !logFile.delete())
				throw new IOException(String.format("File %s could not be overwritten", logFile.getName()));
		}
		
		log = new BufferedWriter(new FileWriter(logFile));
	}

	@Override
	public void close() throws IOException
	{
		log.flush();
		log.close();
	}
	
	public static String clean(String input)
	{
		StringBuilder sb = new StringBuilder();
		for (char c: input.toCharArray())
		{
			if (Character.isUnicodeIdentifierPart(c))
				sb.append(c);
		}
		return sb.toString();
	}
	
	public void restart()
	{
		startTime = System.currentTimeMillis();
	}

	@Override
	public void actionPerformed(ActionEvent event)
	{
		long time = event.getWhen() - startTime;
		String line = String.format("%d,%s\r\n", time, clean(event.getActionCommand()));
		synchronized(log)
		{
			try
			{
				log.write(line);
				log.flush();
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(null,
						"Please inform the researcher that the program is unable to save the data to disk.",
					    "Disk Write Failure",
					    JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
