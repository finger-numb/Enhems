package enhems;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import enhems.components.ControlPanel;
import enhems.components.GraphPanel;
import enhems.components.MeasuredUnitPanel;


public class Enhems extends JFrame implements DataListener{

	private static final long serialVersionUID = 1L;
	private static final String pathToLoadingIcon = "res/icons/loading.gif";
	private EnhemsDataModel dataModel;
	private Timer refreshDataModelTimer = new Timer();
	
	private JLabel roomSelected;
	
	public Enhems() {
		setTitle("EnhemsApp");
		setImageIcon();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				refreshDataModelTimer.cancel();
				refreshDataModelTimer.purge();
			}
		});
		loginGUI();
	}
	
	private void setImageIcon() {
		List<Image> icons = new ArrayList<>();
		icons.add(new ImageIcon(Utilities.class.getResource("res/icons/enhems32.png")).getImage());
		icons.add(new ImageIcon(Utilities.class.getResource("res/icons/enhems16.png")).getImage());
		setIconImages(icons);
	}
	
	
	private void loginGUI() {
		LoginProcess.tokenLogin(this, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				mainGUI();
			}
		});
	}
	
	public void panelLogin() {
		LoginProcess.createLoginGUI(this);
	}

	private void mainGUI() {
		
		dataModel = new EnhemsDataModel();
		
		initTimerRefresh(dataModel);
		
		dataModel.addListener(this);
		JPanel centerPanel = new JPanel(new GridLayout(1,0));

		GraphPanel graphPanel = new GraphPanel("Graf", dataModel);
		centerPanel.add(graphPanel);
		
		setVisible(true);
		getContentPane().removeAll();
		getContentPane().repaint();
		setLayout(new BorderLayout());

		JPanel upperPanel = new JPanel(new GridBagLayout());
		JPanel leftPanel = new JPanel(new GridBagLayout());
		
		ControlPanel controlPanel = new ControlPanel(
				"Kontrole", 10, BoxLayout.Y_AXIS, TitledBorder.CENTER, dataModel);
		MeasuredUnitPanel humidityPanel = new MeasuredUnitPanel(
				"Vlažnost", "res/icons/humidity.png", dataModel, GraphCodes.humidity);
		MeasuredUnitPanel co2Panel = new MeasuredUnitPanel(
				"CO2", "res/icons/co2.png", dataModel, GraphCodes.co2);
		MeasuredUnitPanel temperaturePanel = new MeasuredUnitPanel(
				"Temperatura", "res/icons/temperature.png", dataModel, GraphCodes.temperature);
		
		leftPanel.setBorder(BorderFactory.createEmptyBorder(0,10,10,10));
		GridBagConstraints c = new GridBagConstraints();
		c.weighty = 1.0;
		c.fill=GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.PAGE_START;
		leftPanel.add(controlPanel,c);
		c.fill=GridBagConstraints.NONE;
		c.weighty = 0.3;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridy=1;
		leftPanel.add(temperaturePanel,c);
		c.gridy=2;
		leftPanel.add(co2Panel,c);
		c.gridy=3;
		leftPanel.add(humidityPanel, c);

		roomSelected = new JLabel("---");
		roomSelected.setEnabled(false);
//		DefaultListCellRenderer dlcr = new DefaultListCellRenderer(); 
//		dlcr.setHorizontalAlignment(DefaultListCellRenderer.CENTER); 
//		roomSelected.setRenderer(dlcr);
//		roomSelected.setPreferredSize(new Dimension(150, roomSelected.getPreferredSize().height));
		JLabel usernameLabel = new JLabel("Prijavljen: " + Token.getUsername());
		usernameLabel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		JButton logoutButton = new JButton("Odjava");
		logoutButton.addActionListener((l)-> {
			panelLogin();
			Token.removeToken();
		});
		JButton refreshButton = new JButton("Osvježi");
		refreshButton.addActionListener((l)-> {
			ServerService.executeRequest(new ServerRequest() {
				public void execute() {
					ImageIcon loadingIcon = new ImageIcon(
							Utilities.class.getResource(pathToLoadingIcon));
					refreshButton.setHorizontalTextPosition(JButton.LEFT);
					refreshButton.setIcon(loadingIcon);
					refreshButton.setEnabled(false);
					dataModel.refreshData();
					
					/**
					 * using this sleep to show user that button was pressed
					 * in case of fast refreshing, also removes possibility
					 * for user to send to many requests at time by spaming 
					 * button
					 */
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						MyLogger.log("Trouble with thread sleep", e);
					}
				}
				public void afterExecution() {
					refreshButton.setIcon(null);
					refreshButton.setEnabled(true);
				}
			});
		});


		c = new GridBagConstraints();
		c.insets = new Insets(10, 10, 15, 0);
		c.weightx = 1;
		c.gridx = 0;
		c.anchor = GridBagConstraints.LINE_START;
		upperPanel.add(usernameLabel, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.LINE_END;
		upperPanel.add(new JLabel("Prostorija:"), c);
		c.gridx = 2;
		c.anchor = GridBagConstraints.LINE_START;
		upperPanel.add(roomSelected, c);
		c.anchor = GridBagConstraints.LINE_END;
		c.gridx = 3;
		upperPanel.add(refreshButton, c);
		c.gridx = 4;
		c.anchor = GridBagConstraints.LINE_START;
		upperPanel.add(logoutButton, c);

		getContentPane().add(leftPanel, BorderLayout.LINE_START);
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		getContentPane().add(upperPanel, BorderLayout.PAGE_START);	
		getContentPane().revalidate();
		
		//or 1024x620
		setSize(930, 580);
		Utilities.putFrameInScreenCenter(this);
		
		
		dataModel.refreshData();
	}
	

	private void initTimerRefresh(EnhemsDataModel dataModel) {
		
        TimerTask userTask = new TimerTask() {
            @Override
            public void run() {
            	dataModel.refreshData();
            }
        };
        
		int refreshPeriod = 1200000;
        refreshDataModelTimer.schedule(userTask, refreshPeriod, refreshPeriod);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(()-> {
			try {
				UIManager.setLookAndFeel(
						"com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			} catch (Exception ignorable) { }
			new Enhems();
			
		});
	}

	public EnhemsDataModel getDataModel() {
		return dataModel;
	}

	@Override
	public void dataChanged() {
		if(roomSelected != null) {
			roomSelected.setText(dataModel.getSelectedRoom());
		}
		
	}

}