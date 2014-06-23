package edu.uvm.vcbh;

import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class Phase2 extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 5773182870388558703L;
	
	public static final String EXIT_CHALLENGE = "EXIT";
	public static final int FIXED_SCHEDULE_SIZE = 10;
	public static final long SESSION_TIME_MILLIS = 3 * 60 * 60 * 1000; // hours * min/hour * sec/min * millis/sec
	
	protected ActionEventLog mEventLog = null;
	protected JLabel mTimerLabel = null;
	protected JButton mButtonLeft = null;
	protected JButton mButtonRight = null;

	protected Timer mSessionTimer = null;
	protected String mSessionID = null;
	protected String mResponseLeft = null;
	protected String mResponseRight = null;
	protected long mSessionEndTime = 0;
	
	protected int mnResponsesLeft = 0;
	protected int mnResponsesRight = 0;
	protected int mnReinforcementsLeft = 0;
	protected int mnReinforcementsRight = 0;
	protected int mnTrials = 0;

	public Phase2()
	{
		while (!isValidSession(mSessionID))
		{
			mSessionID = (String)JOptionPane.showInputDialog(
	                this,
	                "Enter a valid session ID:",
	                "New Session",
	                JOptionPane.PLAIN_MESSAGE);
			
			if (null == mSessionID) // Indicates they pressed Cancel.
				System.exit(0);
		}
		
		while (!isValidResponse(mResponseLeft))
		{
			mResponseLeft = (String)JOptionPane.showInputDialog(
	                this,
	                "Enter the label for left response:",
	                "Left Response",
	                JOptionPane.PLAIN_MESSAGE);
			
			if (null == mResponseLeft) // Indicates they pressed Cancel.
				System.exit(0);
		}
		
		while (!isValidResponse(mResponseRight) || 0 == mResponseLeft.compareTo(mResponseRight))
		{
			mResponseRight = (String)JOptionPane.showInputDialog(
	                this,
	                "Enter the label for right response:",
	                "Right Response",
	                JOptionPane.PLAIN_MESSAGE);
			
			if (null == mResponseRight) // Indicates they pressed Cancel.
				System.exit(0);
		}
		
		try
		{
			mEventLog = new ActionEventLog(String.format("%s.%s.%s", mSessionID, mResponseLeft, mResponseRight));
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), "Initialization Error", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		
		JOptionPane.showMessageDialog(this,
			    "Press OK after resetting the time on the CReSS",
			    "CReSS Synchronization",
			    JOptionPane.PLAIN_MESSAGE);
		mEventLog.restart();
		
		try
		{
			Desktop.getDesktop().browse(new URI("http://vcbh.uvm.edu/Phase2Instructions.html"));
		}
		catch (IOException | URISyntaxException e1)
		{
			JOptionPane.showMessageDialog(this,
				    "Failed to open a browser window to display the participant instructions!",
				    "Failed to Open Browser",
				    JOptionPane.ERROR_MESSAGE);
		}
		
		// Initialize Dialog
		setLayout(new GridBagLayout());
		GridBagConstraints constraints;
		
		mTimerLabel = new JLabel("03:00:00");
		mTimerLabel.setFont(mTimerLabel.getFont().deriveFont(64.0f));
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 3;
		constraints.weightx = 1.0;
		constraints.weighty = 0.5;
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.fill = GridBagConstraints.BOTH;
		add(mTimerLabel, constraints);
		
		mButtonLeft = new JButton(mResponseLeft);
		mButtonLeft.setFont(mButtonLeft.getFont().deriveFont(32.0f));
		mButtonLeft.setActionCommand(mResponseLeft);
		mButtonLeft.addActionListener(mEventLog);
		mButtonLeft.addActionListener(this);
		mButtonLeft.setEnabled(false);
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.weightx = 0.4;
		constraints.weighty = 0.5;
		constraints.anchor = GridBagConstraints.SOUTHWEST;
		constraints.fill = GridBagConstraints.BOTH;
		add(mButtonLeft, constraints);
		
		JButton secretButton = new JButton("Ponies!");
		secretButton.setVisible(false);
		secretButton.setEnabled(false);
		constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 1;
		constraints.weightx = 0.2;
		constraints.weighty = 0.5;
		constraints.anchor = GridBagConstraints.SOUTH;
		constraints.fill = GridBagConstraints.BOTH;
		add(secretButton, constraints);
		
		mButtonRight = new JButton(mResponseRight);
		mButtonRight.setFont(mButtonRight.getFont().deriveFont(32.0f));
		mButtonRight.setActionCommand(mResponseRight);
		mButtonRight.addActionListener(mEventLog);
		mButtonRight.addActionListener(this);
		mButtonRight.setEnabled(false);
		constraints = new GridBagConstraints();
		constraints.gridx = 2;
		constraints.gridy = 1;
		constraints.weightx = 0.4;
		constraints.weighty = 0.5;
		constraints.anchor = GridBagConstraints.SOUTHEAST;
		constraints.fill = GridBagConstraints.BOTH;
		add(mButtonRight, constraints);
		
		pack();
		
		// Wait for signal to start trials
		JOptionPane.showMessageDialog(this,
			    "Press OK to begin session",
			    "Begin Session",
			    JOptionPane.PLAIN_MESSAGE);
		
		// Begin the clock
		mSessionEndTime = System.currentTimeMillis() + SESSION_TIME_MILLIS;
		mSessionTimer = new Timer("Main clock update timer");
		mSessionTimer.scheduleAtFixedRate(new TimerTask(){
			public void run()
			{
				tick();
			}
		}, 0, 500);
		
		startTrial();
	}

	protected void tick()
	{
		long ms = mSessionEndTime - System.currentTimeMillis();
		if (0 >= ms) ms = 0;
		long s = (ms / 1000) % 60;
		long m = (ms / (1000 * 60)) % 60;
		long h = (ms / (1000 * 60 * 60));
		mTimerLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
		
		if (0 >= ms)
			endSession();		
	}

	protected void endSession()
	{
		mSessionTimer.cancel();
		mEventLog.actionPerformed(new ActionEvent(this, 0, "END"));
		try
		{
			mEventLog.close();
		}
		catch(IOException e){} // Honestly, what am I going to do with an exception now?
		mButtonLeft.setEnabled(false);
		mButtonRight.setEnabled(false);
		JOptionPane.showMessageDialog(this,
			    "Your session is complete; please inform your session coordinator.\n" +
		        "Thank you for your help in our research!\n\n" + 
			    String.format("%s: %d, %s: %d", mResponseLeft, mnReinforcementsLeft, mResponseRight, mnReinforcementsRight),
			    "Session Complete",
			    JOptionPane.PLAIN_MESSAGE);
		mEventLog.restart();
	}

	protected static boolean isValidSession(String sessionID)
	{
		if (null == sessionID)
			return false;

		if (sessionID.isEmpty())
			return false;
		
		boolean bValid = true;
		// TODO Validate however you like...
		return bValid;
	}

	protected static boolean isValidResponse(String response)
	{
		if (null == response)
			return false;

		if (response.isEmpty())
			return false;
		
		boolean bValid = true;
		// TODO Validate however you like...
		return bValid;
	}

	public static void main(String[] args)
	{
		final Phase2 session = new Phase2();
	    session.setTitle("Phase2 Session");
	    session.setSize(300, 100);
	    session.setLocation(300, 100);

	    session.addWindowListener(new WindowAdapter()
	    {
	        @Override
	        public void windowClosing(WindowEvent e)
	        {
	        	if (!session.isDone())
	        	{
	    			String response = (String)JOptionPane.showInputDialog(
	    	                null,
	    	                String.format("Type \"%s\" to terminate the session:", EXIT_CHALLENGE),
	    	                "Terminate Session",
	    	                JOptionPane.PLAIN_MESSAGE);
	    			
	    			if (null == response || 0 != response.compareToIgnoreCase(EXIT_CHALLENGE))
	    				return;
	        	}
	        	session.endSession();
	            session.setVisible(false);
	            session.dispose();
	        }
	    });

	    session.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	    session.setVisible(true);
	}

	protected boolean isDone()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == mButtonLeft)
		{
			responseLeft();
		}
		else if (e.getSource() == mButtonRight)
		{
			responseRight();
		}
	}

	protected void responseLeft()
	{
		if (FIXED_SCHEDULE_SIZE > ++mnResponsesLeft)
			return;
		
		mnReinforcementsLeft++;
		reinforce(mResponseLeft);
	}

	protected void responseRight()
	{
		if (FIXED_SCHEDULE_SIZE > ++mnResponsesRight)
			return;
		
		mnReinforcementsRight++;
		reinforce(mResponseRight);
	}

	protected void reinforce(String reinforcer)
	{
		// TODO Auto-generated method stub
		
	}
	
	protected void startTrial()
	{
		
	}
}
