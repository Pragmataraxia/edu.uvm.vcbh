package edu.uvm.vcbh;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JOptionPane;



public class Phase2 extends ConcurrentChoiceExperiment
{
	private static final long serialVersionUID = 5773182870388558703L;

	public Phase2()
	{
		super();
		this.setTitle("Phase 2");
	}

	@Override
	protected int requiredResponsesLeft()
	{
		return FIXED_SCHEDULE_SIZE;
	}

	@Override
	protected int requiredResponsesRight()
	{
		return FIXED_SCHEDULE_SIZE;
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
