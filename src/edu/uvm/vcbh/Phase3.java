package edu.uvm.vcbh;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JOptionPane;

public class Phase3 extends ConcurrentChoiceExperiment
{
	private static final long serialVersionUID = 2617381599188095940L;

	protected static final int[] PROGRESSIVE_SCHEDULE_COSTS = { 10, 50, 200, 600, 1200 };
	protected static final int PROGRESSIVE_SCHEDULE_INCREMENT = 1200;
	
	boolean mbProgressiveOnLeft = true;

	public Phase3()
	{
		super();
		this.setTitle("Phase 3");
	}

	@Override
	protected boolean gatherParams()
	{
		if (!super.gatherParams())
			return false;
		
		String[] options = { "Left", "Right" };
		int response = JOptionPane.showOptionDialog(this, "Which input has the progressive schedule?", "Progressive Schedule", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);

		if (JOptionPane.CLOSED_OPTION == response)
			return false;
		
		mbProgressiveOnLeft = (JOptionPane.YES_OPTION == response);
		
		return true;
	}

	@Override
	protected int requiredResponsesLeft()
	{
		return mbProgressiveOnLeft ? requiredResponsesProgressive(mnReinforcementsLeft) : FIXED_SCHEDULE_SIZE;
	}

	@Override
	protected int requiredResponsesRight()
	{
		return mbProgressiveOnLeft ? FIXED_SCHEDULE_SIZE : requiredResponsesProgressive(mnReinforcementsRight);
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

	public static void main(String[] args)
	{
		final Phase3 session = new Phase3();
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
					String response = (String) JOptionPane.showInputDialog(null, String.format("Type \"%s\" to terminate the session:", EXIT_CHALLENGE), "Terminate Session", JOptionPane.PLAIN_MESSAGE);

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
}
