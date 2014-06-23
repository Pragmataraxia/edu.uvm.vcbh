package edu.uvm.vcbh;

import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
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
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class Phase2 extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 5773182870388558703L;
	
	public static final String EXIT_CHALLENGE = "EXIT";
	public static final int FIXED_SCHEDULE_SIZE = 10;
	public static final long SESSION_TIME_MILLIS = 3 * 60 * 60 * 1000; // hours * min/hour * sec/min * millis/sec
	public static final long REINFORCER_TIME_MILLIS = 3 * 60 * 1000; // minutes * sec/min * millis/sec
	
	protected Container mTrialPanel = null;
	protected ActionEventLog mEventLog = null;
	protected JLabel mSessionTimerLabel = null;
	protected JButton mButtonLeft = null;
	protected JButton mButtonRight = null;

	protected Container mReinforcerPanel = null;
	protected JLabel mReinforcerInstructionLabel = null;
	protected JLabel mReinforcerTimerLabel = null;
	protected long mReinforcerEndTime = 0;

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
	}

	protected boolean startSession()
	{
		while (!isValidSession(mSessionID))
		{
			mSessionID = (String)JOptionPane.showInputDialog(
	                this,
	                "Enter a valid session ID:",
	                "New Session",
	                JOptionPane.PLAIN_MESSAGE);
			
			if (null == mSessionID) // Indicates they pressed Cancel.
				return false;
		}
		
		while (!isValidResponse(mResponseLeft))
		{
			mResponseLeft = (String)JOptionPane.showInputDialog(
	                this,
	                "Enter the label for left response:",
	                "Left Response",
	                JOptionPane.PLAIN_MESSAGE);
			mResponseLeft = mResponseLeft.toUpperCase();
			
			if (null == mResponseLeft) // Indicates they pressed Cancel.
				return false;
		}
		
		while (!isValidResponse(mResponseRight) || 0 == mResponseLeft.compareTo(mResponseRight))
		{
			mResponseRight = (String)JOptionPane.showInputDialog(
	                this,
	                "Enter the label for right response:",
	                "Right Response",
	                JOptionPane.PLAIN_MESSAGE);
			mResponseRight = mResponseRight.toUpperCase();
			
			if (null == mResponseRight) // Indicates they pressed Cancel.
				return false;
		}
		
		try
		{
			mEventLog = new ActionEventLog(String.format("%s.%s.%s", mSessionID, mResponseLeft, mResponseRight));
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), "Initialization Error", JOptionPane.ERROR_MESSAGE);
			return false;
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
		
		initializeDialog();
		
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
		
		return true;
	}

	protected void initializeDialog()
	{
	    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	    
		mTrialPanel = new JPanel();
		setTitle(String.format("Phase 2: %s.%s.%s", mSessionID, mResponseLeft, mResponseRight));
		mTrialPanel.setLayout(new GridBagLayout());
		GridBagConstraints constraints;
		
		mSessionTimerLabel = new JLabel("03:00:00", SwingConstants.CENTER);
		mSessionTimerLabel.setFont(mSessionTimerLabel.getFont().deriveFont(64.0f));
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.weightx = 1.0;
		constraints.weighty = 0.01;
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.fill = GridBagConstraints.BOTH;
		mTrialPanel.add(mSessionTimerLabel, constraints);
		
		mButtonLeft = new JButton(mResponseLeft);
		mButtonLeft.setPreferredSize(new Dimension(160, 160));
		mButtonLeft.setFont(mButtonLeft.getFont().deriveFont(64.0f));
		mButtonLeft.setFocusable(false);
		mButtonLeft.setActionCommand(mResponseLeft);
		// ActionListeners are called back in the opposite order they were added.
		// So, to keep actions appearing in the correct order in the log, add the log last.
		mButtonLeft.addActionListener(this);
		mButtonLeft.addActionListener(mEventLog);
		mButtonLeft.setEnabled(false);
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.weightx = 0.4;
		constraints.weighty = 0.5;
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.fill = GridBagConstraints.NONE;
		mTrialPanel.add(mButtonLeft, constraints);
		
		JLabel emptySpace = new JLabel();
		constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 1;
		constraints.weightx = 0.2;
		constraints.weighty = 0.5;
		constraints.anchor = GridBagConstraints.SOUTH;
		constraints.fill = GridBagConstraints.BOTH;
		mTrialPanel.add(emptySpace, constraints);
		
		mButtonRight = new JButton(mResponseRight);
		mButtonRight.setPreferredSize(new Dimension(160, 160));
		mButtonRight.setFont(mButtonRight.getFont().deriveFont(64.0f));
		mButtonRight.setFocusable(false);
		mButtonRight.setActionCommand(mResponseRight);
		// ActionListeners are called back in the opposite order they were added.
		// So, to keep actions appearing in the correct order in the log, add the log last.
		mButtonRight.addActionListener(this);
		mButtonRight.addActionListener(mEventLog);
		mButtonRight.setEnabled(false);
		constraints = new GridBagConstraints();
		constraints.gridx = 2;
		constraints.gridy = 1;
		constraints.weightx = 0.4;
		constraints.weighty = 0.5;
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.fill = GridBagConstraints.NONE;
		mTrialPanel.add(mButtonRight, constraints);

		// Set up the reinforcer panel
		mReinforcerPanel = new JPanel();
		mReinforcerPanel.setLayout(new GridBagLayout());
		
		mReinforcerTimerLabel = new JLabel("03:00", SwingConstants.CENTER);
		mReinforcerTimerLabel.setFont(mSessionTimerLabel.getFont());
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.weightx = 1.0;
		constraints.weighty = 0.01;
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.fill = GridBagConstraints.NONE;
		mReinforcerPanel.add(mReinforcerTimerLabel, constraints);

		mReinforcerInstructionLabel = new JLabel("Instructions go here...", SwingConstants.CENTER);
		mReinforcerInstructionLabel.setFont(mReinforcerInstructionLabel.getFont().deriveFont(18.0f));
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.weightx = 1.0;
		constraints.weighty = 0.5;
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.fill = GridBagConstraints.BOTH;
		mReinforcerPanel.add(mReinforcerInstructionLabel, constraints);
		
		// Add the two panels to a card layout for application
		Container cp = this.getContentPane();
		cp.setLayout(new CardLayout());
		cp.add(mTrialPanel, "TRIAL");
		cp.add(mReinforcerPanel, "REINFORCE");
	}

	protected void tick()
	{
		// First tick the session clock
		long ms = mSessionEndTime - System.currentTimeMillis();
		if (0 >= ms) ms = 0;
		long s = (ms / 1000) % 60;
		long m = (ms / (1000 * 60)) % 60;
		long h = (ms / (1000 * 60 * 60));
		mSessionTimerLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
		
		// If the session is over...
		if (0 >= ms)
			endSession();
		
		// If we're not currently reinforcing, we're done.
		if (0 >= mReinforcerEndTime)
			return;
		
		// Tick the reinforcer timer.
		ms = mReinforcerEndTime - System.currentTimeMillis();
		if (0 >= ms) ms = 0;
		s = (ms / 1000) % 60;
		m = (ms / (1000 * 60)) % 60;
		mReinforcerTimerLabel.setText(String.format("%02d:%02d", m, s));
		
		// If reinforcement is over, start the next trial.
		if (0 >= ms && !isDone())
			startTrial();
	}

	protected void endSession()
	{
		mSessionTimer.cancel();
		mEventLog.actionPerformed(new ActionEvent(this, 0, "EXIT", System.currentTimeMillis(), 0));
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
		setFullscreen(false);
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
		session.setFullscreen(true);
		if (!session.startSession())
			System.exit(0);

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

	    session.setVisible(true);
	}

	protected boolean isDone()
	{
		return System.currentTimeMillis() > mSessionEndTime;
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
		mReinforcerEndTime = System.currentTimeMillis() + REINFORCER_TIME_MILLIS;
		mEventLog.actionPerformed(new ActionEvent(this, 0, String.format("TRIAL_%02d_END", mnTrials), System.currentTimeMillis(), 0));
		mButtonLeft.setEnabled(false);
		mButtonRight.setEnabled(false);
		
		mReinforcerInstructionLabel.setText(String.format("<html><center>Please follow the instructions for taking a puff from cigarette %s.<br>"
				+ "The next trial will begin when the timer ends.</center></html>", reinforcer));
		tick(); // Make sure the timer label is updated.

		Container cp = getContentPane();
		CardLayout cl = (CardLayout)cp.getLayout();
		cl.show(cp, "REINFORCE");
	}
	
	protected void startTrial()
	{
		mnTrials++;
		mEventLog.actionPerformed(new ActionEvent(this, 0, String.format("TRIAL_%02d_START", mnTrials), System.currentTimeMillis(), 0));
		mnResponsesLeft = 0;
		mnResponsesRight = 0;
		mReinforcerEndTime = 0;
		
		mButtonLeft.setEnabled(true);
		mButtonRight.setEnabled(true);

		Container cp = getContentPane();
		CardLayout cl = (CardLayout)cp.getLayout();
		cl.show(cp, "TRIAL");
		Toolkit.getDefaultToolkit().beep();
	}
	
	public void setFullscreen(boolean bOn)
	{
		try
		{
		    setUndecorated(bOn);	
		}
		catch (Exception e){}
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
		device.setFullScreenWindow(bOn ? this : null);
	}
}
