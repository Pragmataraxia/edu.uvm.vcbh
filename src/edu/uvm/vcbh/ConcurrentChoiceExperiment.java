package edu.uvm.vcbh;

import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

//import java.net.URI;
//import java.net.URISyntaxException;
//import java.awt.GraphicsDevice;
//import java.awt.GraphicsEnvironment;
//import java.awt.Desktop;
//import javax.swing.JTextArea;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class ConcurrentChoiceExperiment extends JFrame implements ActionListener, KeyListener
{
	private static final long serialVersionUID = 4758559233406217497L;
	
	public static final int FIXED_SCHEDULE_SIZE = 10;
	protected static final int[] PROGRESSIVE_SCHEDULE_COSTS = { 10, 160, 320, 640, 1280, 2400 };
	protected static final int PROGRESSIVE_SCHEDULE_INCREMENT = 1200;

	public static final String EXIT_CHALLENGE = "EXIT";
	public static final long SESSION_TIME_MILLIS = (long)(3 * 60 * 60 * 1000); // hours * min/hour * sec/min * millis/sec
	public static final long REINFORCER_TIME_MILLIS = (long)(3 * 60 * 1000);   // minutes * sec/min * millis/sec
	
	public enum ProgressiveInput { NONE, LEFT, RIGHT };
	protected ProgressiveInput mProgressive = ProgressiveInput.NONE;

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
	protected int mnMaxRewardedResponses = 0;

	public ConcurrentChoiceExperiment()
	{
		this.setTitle("Concurrent Choice Experiment");
	}

	protected boolean startSession()
	{
		if (!gatherParams())
			return false;

		try
		{
			mEventLog = new ActionEventLog(getTitle());
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), "Initialization Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		JOptionPane.showMessageDialog(this, "Press OK after resetting the time on the CReSS", "CReSS Synchronization", JOptionPane.PLAIN_MESSAGE);
		mEventLog.restart();

//		try
//		{
//			Desktop.getDesktop().browse(new URI("http://vcbh.uvm.edu/Phase2Instructions.html"));
//		}
//		catch (IOException | URISyntaxException e1)
//		{
//			JOptionPane.showMessageDialog(this, "Failed to open a browser window to display the participant instructions!", "Failed to Open Browser", JOptionPane.ERROR_MESSAGE);
//		}

		initializeDialog();
		
		addKeyListener(this);

		// Wait for signal to start trials
		JOptionPane.showMessageDialog(this, "Press OK to begin session", "Begin Session", JOptionPane.PLAIN_MESSAGE);

		// Begin the clock
		mSessionEndTime = System.currentTimeMillis() + SESSION_TIME_MILLIS;
		mSessionTimer = new Timer("Main clock update timer");
		mSessionTimer.scheduleAtFixedRate(new TimerTask()
		{
			public void run()
			{
				tick();
			}
		}, 0, 500);

		startTrial();

		return true;
	}

	protected boolean gatherParams()
	{
		boolean bCorrect = false;
		while (!bCorrect)
		{
			mSessionID = null;
			while (!isValidSession(mSessionID))
			{
				mSessionID = (String) JOptionPane.showInputDialog(this, "Enter a valid session ID:", "New Session", JOptionPane.PLAIN_MESSAGE);

				if (null == mSessionID) // Indicates they pressed Cancel.
					return false;
			}

			mResponseLeft = null;
			while (!isValidResponse(mResponseLeft))
			{
				mResponseLeft = (String) JOptionPane.showInputDialog(this, "Enter the label for left response:", "Left Response", JOptionPane.PLAIN_MESSAGE);
				if (null == mResponseLeft) // Indicates they pressed Cancel.
					return false;
				
				mResponseLeft = mResponseLeft.toUpperCase();
			}

			mResponseRight = null;
			while (!isValidResponse(mResponseRight) || 0 == mResponseLeft.compareTo(mResponseRight))
			{
				mResponseRight = (String) JOptionPane.showInputDialog(this, "Enter the label for right response:", "Right Response", JOptionPane.PLAIN_MESSAGE);
				if (null == mResponseRight) // Indicates they pressed Cancel.
					return false;
				
				mResponseRight = mResponseRight.toUpperCase();
			}

			mProgressive = ProgressiveInput.NONE;
			{
				String progressiveOptions[] = { "None", mResponseLeft, mResponseRight };
				int response = JOptionPane.showOptionDialog(this, "Which input has the progressive schedule?", "Progressive Schedule", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, progressiveOptions, null);
				if (JOptionPane.CLOSED_OPTION == response)
					return false;

				if (1 == response)
					mProgressive = ProgressiveInput.LEFT;
				else if (2 == response)
					mProgressive = ProgressiveInput.RIGHT;
			}
			
			// Verify settings...
			String prog = "ERROR";
			switch(mProgressive)
			{
			case LEFT:
				prog = mResponseLeft;
				break;
			case RIGHT:
				prog = mResponseRight;
				break;
			case NONE:
			default:
				prog = "None";
				break;
			}
			
			// Using HTML to support presenting the data in a table...
			String confirm = String.format("<html>Are the following settings correct?<br>"
					+ "<table style=\"width:200px\">"
					+ "<tr><td>Session ID</td><td>%s</td></tr>"
					+ "<tr><td>Left Choice</td><td>%s</td></tr>"
					+ "<tr><td>Right Choice</td><td>%s</td></tr>"
					+ "<tr><td>Progressive</td><td>%s</td></tr>"
					+ "</html>", mSessionID, mResponseLeft, mResponseRight, prog);
			
			// Or, using a JTextArea so that it can have tabs.
//			JTextArea confirm = new JTextArea(String.format("Are the following settings correct?\n\n"
//					+ "Session ID:\t%s\n"
//					+ "Left Choice:\t%s\n"
//					+ "Right Choice:\t%s\n"
//					+ "Progressive:\t%s", mSessionID, mResponseLeft, mResponseRight, prog));
//			confirm.setOpaque(false);
			
			bCorrect = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, confirm, "Verification", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null);
		}

		String left = ProgressiveInput.LEFT == mProgressive ? mResponseLeft : mResponseLeft.toLowerCase();
		String right = ProgressiveInput.RIGHT == mProgressive ? mResponseRight : mResponseRight.toLowerCase();
		setTitle(String.format("%s.%s.%s", mSessionID, left, right));
		
		return true;
	}

	protected void initializeDialog()
	{
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		mTrialPanel = new JPanel();
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
		mButtonLeft.addActionListener(this);
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
		mButtonRight.addActionListener(this);
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
		if (0 >= ms)
			ms = 0;
		long s = (ms / 1000) % 60;
		long m = (ms / (1000 * 60)) % 60;
		long h = (ms / (1000 * 60 * 60));
		mSessionTimerLabel.setText(String.format("%02d:%02d:%02d", h, m, s));


		if (0 >= mReinforcerEndTime) // If we are not currently reinforcing...
		{
			if (0 >= ms) // If the session timer has expired...
				endSession();
		}
		else
		{
			// Tick the reinforcer timer.
			ms = mReinforcerEndTime - System.currentTimeMillis();
			if (0 >= ms)
				ms = 0;
			s = (ms / 1000) % 60;
			m = (ms / (1000 * 60)) % 60;
			mReinforcerTimerLabel.setText(String.format("%02d:%02d", m, s));

			// If reinforcement is over, start the next trial.
			if (0 >= ms)
				startTrial();
		}
	}

	protected void endSession()
	{
		mSessionTimer.cancel();
		mEventLog.actionPerformed(new ActionEvent(this, 0, "EXIT", System.currentTimeMillis(), 0));
		try
		{
			mEventLog.close();
		}
		catch (IOException e)
		{} // Honestly, what am I going to do with an exception now?
		mButtonLeft.setEnabled(false);
		mButtonRight.setEnabled(false);
		
		JOptionPane.showMessageDialog(this, "Your session is complete; please inform your session coordinator.\n" + "Thank you for your help in our research!", "Session Complete", JOptionPane.PLAIN_MESSAGE);

		String summary = String.format("<html><table style=\"width:300px\">"
				+ "<tr><td>%s Earned</td><td>%d</td>"
				+ "<tr><td>%s Earned</td><td>%d</td>", mResponseLeft, mnReinforcementsLeft, mResponseRight, mnReinforcementsRight);
		if (ProgressiveInput.NONE != mProgressive)
			summary += String.format("<tr><td>Highest PR completed</td><td>%d</td>", mnMaxRewardedResponses);
		summary += "</table></html>";
		JOptionPane.showMessageDialog(this, summary, "Session Summary", JOptionPane.PLAIN_MESSAGE);
		
		setFullscreen(false);
		setVisible(false);
		dispose();
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

	protected boolean isDone()
	{
		return System.currentTimeMillis() > mSessionEndTime;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		mEventLog.actionPerformed(e);
		if (e.getSource() == mButtonLeft)
		{
			if (requiredResponsesLeft() > ++mnResponsesLeft)
				return;

			mnReinforcementsLeft++;
			mnMaxRewardedResponses = Math.max(mnMaxRewardedResponses, mnResponsesLeft);
			reinforce(e);
		}
		else if (e.getSource() == mButtonRight)
		{
			if (requiredResponsesRight() > ++mnResponsesRight)
				return;

			mnReinforcementsRight++;
			mnMaxRewardedResponses = Math.max(mnMaxRewardedResponses, mnResponsesRight);
			reinforce(e);
		}
	}
	
	protected int requiredResponsesProgressive(int nReinforcers)
	{
		int n = nReinforcers;
		int r = 0;
		int nLinear = 1 + n - PROGRESSIVE_SCHEDULE_COSTS.length;
		
		if (0 < nLinear)
		{
			n = PROGRESSIVE_SCHEDULE_COSTS.length - 1;
			r = nLinear * PROGRESSIVE_SCHEDULE_INCREMENT;
		}
		
		r += PROGRESSIVE_SCHEDULE_COSTS[n];
		return r;
	}

	protected int requiredResponsesLeft()
	{
		return ProgressiveInput.LEFT == mProgressive ? requiredResponsesProgressive(mnReinforcementsLeft) : FIXED_SCHEDULE_SIZE;
	}

	protected int requiredResponsesRight()
	{
		return ProgressiveInput.RIGHT == mProgressive ? requiredResponsesProgressive(mnReinforcementsRight) : FIXED_SCHEDULE_SIZE;
	}

	protected void reinforce(ActionEvent e)
	{
		mReinforcerEndTime = e.getWhen() + REINFORCER_TIME_MILLIS;
		mEventLog.actionPerformed(new ActionEvent(this, 0, String.format("TRIAL_%02d_END", mnTrials), e.getWhen(), 0));
		mButtonLeft.setEnabled(false);
		mButtonRight.setEnabled(false);

		mReinforcerInstructionLabel.setText(String.format("<html><center>Please follow the instructions for taking a puff from cigarette %s.<br>" + "The next trial will begin when the timer ends.</center></html>", e.getActionCommand()));
		tick(); // Make sure the timer label is updated.

		Container cp = getContentPane();
		CardLayout cl = (CardLayout) cp.getLayout();
		cl.show(cp, "REINFORCE");
	}

	protected void startTrial()
	{
		if (isDone())
		{
			endSession();
			return;
		}
		
		mnTrials++;
		mEventLog.actionPerformed(new ActionEvent(this, 0, String.format("TRIAL_%02d_START", mnTrials), System.currentTimeMillis(), 0));
		mnResponsesLeft = 0;
		mnResponsesRight = 0;
		mReinforcerEndTime = 0;

		mButtonLeft.setEnabled(true);
		mButtonRight.setEnabled(true);

		Container cp = getContentPane();
		CardLayout cl = (CardLayout) cp.getLayout();
		cl.show(cp, "TRIAL");
		Toolkit.getDefaultToolkit().beep();
	}

	public void setFullscreen(boolean bOn)
	{		
		setExtendedState(bOn ? JFrame.MAXIMIZED_BOTH : JFrame.NORMAL);
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (KeyEvent.VK_CONTROL != e.getKeyCode())
			return;
		
		if (KeyEvent.KEY_LOCATION_LEFT == e.getKeyLocation())
			mButtonLeft.doClick();
		
		if (KeyEvent.KEY_LOCATION_RIGHT == e.getKeyLocation())
			mButtonRight.doClick();
	}

	@Override
	public void keyReleased(KeyEvent e){}

	@Override
	public void keyTyped(KeyEvent e){}

	public static void main(String[] args)
	{
		final ConcurrentChoiceExperiment session = new ConcurrentChoiceExperiment();
		session.setFullscreen(true);
		session.setVisible(true);
		if (!session.startSession())
			System.exit(0);

		session.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if (!session.isDone())
				{
					String response = (String) JOptionPane.showInputDialog(null, String.format("Type \"%s\" to terminate the session:", EXIT_CHALLENGE), "Terminate Session", JOptionPane.PLAIN_MESSAGE);

					if (null == response || 0 != response.compareToIgnoreCase(EXIT_CHALLENGE))
						return;
				}
				session.endSession(); 
			}
		});

		session.setVisible(true);
	}
}
