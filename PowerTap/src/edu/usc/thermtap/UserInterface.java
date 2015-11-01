/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Majid Ghasemi-Gol, Mahdi Nazemi, 
 *    and Massoud Pedram, SPORT lab, University of Southern California. All 
 *    rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 *
 */
package edu.usc.thermtap;

import java.awt.event.MouseAdapter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.jzy3d.chart.Chart;

import edu.usc.powertap.entities.PidNode;
import edu.usc.powertap.entities.UidNode;
import edu.usc.powertap.powermodel.CPUPower;
import edu.usc.powertap.powermodel.DisplayPower;
import edu.usc.powertap.utils.UserProperty;
import edu.usc.thermtap.ThermTap.Actions;

@SuppressWarnings("serial")
public class UserInterface extends JFrame{
	static UserInterface ui = null;
	JLabel jlbempty;
	TextArea logo;
	TextArea result;
	JButton calculateBtn;
	
	JFileChooser fileChooser = new JFileChooser();
	
	
	public static CPUPower cpuPower;
	public static DisplayPower displayPower;
	
	Component pieChart ;
	Component xyChart ;//= ChartManager.getXYChart(data, data2);
	
	
	JButton openBtn = new JButton("Open");
	JButton connectBtn = new JButton("Connect");
	JButton disconnectBtn = new JButton("Disconnect");

	JButton refreshBtn = new JButton("Refresh");	
	JButton startBtn = new JButton("Start");
	JButton stopBtn = new JButton("Stop");
	
	JButton steadyStateBtn = new JButton("Steady");	
	JButton transientBtn = new JButton("Transient");

	JButton changeBtn = new JButton("Change PID/UID");



//	JButton print = new JButton("print");
	
	JPanel piduidPanel = new JPanel();
	JPanel uidPanel = new JPanel();
	JPanel pidPanel = new JPanel();
	
//	JPanel thermalMaps = new JPanel();
	JPanel thermalChart = new JPanel();
	JPanel thermalComponents = new JPanel();
	JPanel thermalComponentsList = new JPanel();
	JPanel thermalComponentsZList = new JPanel();

	JPanel pieChartPanel = new JPanel();
	JPanel xyChartPanel = new JPanel();
//	JPanel powerCharts = new JPanel();
//	JPanel thermTapPanel = new JPanel();
	JPanel buttonPanel = new JPanel();
	
	Chart thermalChartInstance;
	
	JTable uidTable;
	JTable pidTable;
	JTable thcTable;
	JTable thczTable;
	
	MyTableModel pidTableModel = new MyTableModel();
	MyTableModel uidTableModel = new MyTableModel();
	MyTableModel thcTableModel = new MyTableModel();
	MyTableModel thczTableModel = new MyTableModel();
	
	String []uidColumnNames  = {"UID","Name"};
	String []pidColumnNames  = {"PID","Name"};
	String []thcColumnNames  = {"Component Name"};
	String []thczColumnNames  = {"Component Layer"};
	
	Vector<String> testList = new Vector<>();
	
	boolean thczLock = false;
	boolean pidtLock = false;
	
	
	private PowerTap powertap;
	
	
	private int uid = -1,pid = -1;
	
	public void refreshPowerCharts(){
		double [] powers ;
		
		
		UidNode uidNode = ThermTap.getPhone().getUidMap().get(uid);
		PidNode pidNode = null;
		if(uidNode != null){
			for (PidNode node : uidNode.getPidList()) {
				if(node.getPid() == pid)
					pidNode = node;
			}
		}
		
//		System.out.println("refreshing charts, uid: "+uid+" pid: "+pid+ " "+pidNode);
		
		if(pid != -1 && pidNode != null)
			powers = pidNode.getAvgPower();
		else if(uid != -1 && uidNode != null)
			powers = uidNode.getAvgPower();
		else
			powers = ThermTap.getPhone().getAvgPower();
		
		double [] powers_ = new double [powers.length-3];
		int j = 0;
		for (int i = 0; i < powers_.length; i++) {
			if(i == UserProperty.componentNames.LTE.ordinal() || 
					i == UserProperty.componentNames.DRAM.ordinal() || 
					i == UserProperty.componentNames.GPS.ordinal())
				continue;
			powers_[j] = powers[i];
//			System.out.print(powers_[j]+" ");
			j++;
		}
		
		
		
//		System.out.println();
		Rectangle pieChartBounds = pieChart.getBounds();
		Rectangle xyChartBounds = xyChart.getBounds();
		
		pieChart = ChartManager.getPieChart(powers_, ThermTap.getComponentNames());
		xyChart = ChartManager.getXYChart(ThermTap.getPowerTrace(uid,pid)[0] , ThermTap.getPowerTrace(uid,pid)[1]);
		pieChart.repaint();
		xyChart.repaint();
		
		pieChart.setBounds(pieChartBounds);
		xyChart.setBounds(xyChartBounds);
		
		pieChartPanel.removeAll();
		xyChartPanel.removeAll();
		pieChartPanel.add(pieChart);
		xyChartPanel.add(xyChart);
		
		pieChartPanel.repaint();
		xyChartPanel.repaint();
		
		getContentPane().repaint();
		
		PrintStream ps = null;
		try {
			ps = new PrintStream(new File("PowerTrace.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < ThermTap.getPowerTrace(uid,pid)[0].length; i++) {
			ps.println(ThermTap.getPowerTrace(uid,pid)[0][i]+"\t"+ThermTap.getPowerTrace(uid,pid)[1][i]);
		}
		ps.close();
	}

	
	public UserInterface(PowerTap pInstance) {
		super();

		//Makes sure that all the system resources are freed
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				try {
					Runtime.getRuntime().exec("killall therminator").waitFor();
					powertap.performAction(Actions.STOP);
					powertap.performAction(Actions.DISCONNECT);
				} catch (InterruptedException | IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		
		int btnWidth = 200;
		int btnHeight = 45;
		int btnGap = 60;
		
		setTitle("ThermTap");
		powertap = pInstance;
		
		buttonPanel.setBounds(30, 10, 1300, 50);
		xyChartPanel.setBounds(10, 60, 860, 400);
		
		
		int thirdRowY = 470;
		
		piduidPanel.setBounds(10, thirdRowY, 375, 400);
		uidPanel.setBounds(10, 30, 175, 300);
		pidPanel.setBounds(190, 30, 175, 300);
		
		pieChartPanel.setBounds(870, 60, 420, 400);
		
		thermalComponents.setBounds(388, thirdRowY,402,400);
		thermalComponentsList.setBounds(10,20,100,300);
		thermalComponentsZList.setBounds(10,20,100,300);
		
		refreshBtn.setBounds(10, 340, 170, btnHeight);
		changeBtn.setBounds(refreshBtn.getBounds().x + refreshBtn.getBounds().width + 10, 
				refreshBtn.getBounds().y, refreshBtn.getBounds().width+5, btnHeight);
		
		
		thermalChart.setBounds(790, thirdRowY, 500, 400);
		
		openBtn.setToolTipText("Open Device Thermal Characteristic");
		connectBtn.setToolTipText(connectBtn.getText());
		refreshBtn.setToolTipText(refreshBtn.getText());
		disconnectBtn.setToolTipText(disconnectBtn.getText());
		startBtn.setToolTipText("Start PowerTap");
		stopBtn.setToolTipText("Stop PowerTap");
		steadyStateBtn.setToolTipText("Stead-State Thermal Simulation");
		transientBtn.setToolTipText("Transient Thermal Simulation");
		changeBtn.setToolTipText(changeBtn.getText());
		
		
		openBtn.setBounds(0, 0, btnWidth, btnHeight);
		connectBtn.setBounds(openBtn.getBounds().x + btnWidth + btnGap, 0, btnWidth, btnHeight);
		disconnectBtn.setBounds(openBtn.getBounds().x + btnWidth + btnGap, 0, btnWidth, btnHeight);
		startBtn.setBounds(connectBtn.getBounds().x + btnWidth + btnGap, 0, btnWidth, btnHeight);
		stopBtn.setBounds(connectBtn.getBounds().x + btnWidth + btnGap, 0, btnWidth, btnHeight);
		steadyStateBtn.setBounds(stopBtn.getBounds().x + btnWidth + btnGap, 0, btnWidth, btnHeight);
		transientBtn.setBounds(steadyStateBtn.getBounds().x + btnWidth + btnGap, 0, btnWidth, btnHeight);
		
		openBtn.setMargin(new Insets(0,0,0,0));
		refreshBtn.setMargin(new Insets(0,0,0,0));
		connectBtn.setMargin(new Insets(0,0,0,0));
		disconnectBtn.setMargin(new Insets(0,0,0,0));
		startBtn.setMargin(new Insets(0,0,0,0));
		stopBtn.setMargin(new Insets(0,0,0,0));
		steadyStateBtn.setMargin(new Insets(0,0,0,0));
		transientBtn.setMargin(new Insets(0,0,0,0));
		changeBtn.setMargin(new Insets(0,0,0,0));
		
		
		
		

		
		
//		changeComponentBtn.setBounds(360,100,80,23);
		
		//initial button visibility
		changeBtn.setEnabled(false);
		startBtn.setEnabled(false);
		connectBtn.setEnabled(false);
		steadyStateBtn.setEnabled(false);
		transientBtn.setEnabled(false);
		disconnectBtn.setVisible(false);
		stopBtn.setVisible(false);
		
//		pidTableModel.addRows(powertap.getPhone().getUidMap());
		pidTableModel.setHeader(pidColumnNames);
//		uidTableModel.addRows(powertap.getPhone().getUidMap());
		uidTableModel.setHeader(uidColumnNames);
		thcTableModel.setHeader(thcColumnNames);
		thczTableModel.setHeader(thczColumnNames);
		
		
		uidTable = new JTable(uidTableModel);
		uidTable.getColumnModel().getColumn(0).setPreferredWidth(10);
		
		pidTable = new JTable(pidTableModel);
		pidTable.getColumnModel().getColumn(0).setPreferredWidth(10);
		
		thcTable = new JTable(thcTableModel);
		thcTable.getColumnModel().getColumn(0).setPreferredWidth(5);
		
		
		thczTable = new JTable(thczTableModel);
		
		uidPanel.setLayout( new BorderLayout() );
		uidPanel.add(uidTable, BorderLayout.CENTER);
		uidPanel.add(uidTable.getTableHeader(), BorderLayout.PAGE_START);
		uidPanel.add(new JScrollPane(uidTable));
		
		
		pidPanel.setLayout( new BorderLayout() );
		pidPanel.add(pidTable, BorderLayout.CENTER);
		pidPanel.add(pidTable.getTableHeader(), BorderLayout.PAGE_START);
		pidPanel.add(new JScrollPane(pidTable));
		
		piduidPanel.setLayout(null);
		piduidPanel.add(uidPanel);
		piduidPanel.add(pidPanel);
		piduidPanel.add(refreshBtn);
		piduidPanel.add(changeBtn);
		
		
		
		
		
		pidTable.setSelectionModel(new ForcedListSelectionModel());
		uidTable.setSelectionModel(new ForcedListSelectionModel());
		thcTable.setSelectionModel(new ForcedListSelectionModel());
		thczTable.setSelectionModel(new ForcedListSelectionModel());
		
		thermalComponentsList.setLayout( new BorderLayout() );
		thermalComponentsList.add(thcTable, BorderLayout.CENTER);
		thermalComponentsList.add(thcTable.getTableHeader(), BorderLayout.PAGE_START);
		thermalComponentsList.add(new JScrollPane(thcTable));
		
		thermalComponentsZList.setLayout( new BorderLayout() );
		thermalComponentsZList.add(thczTable, BorderLayout.CENTER);
		thermalComponentsZList.add(thczTable.getTableHeader(), BorderLayout.PAGE_START);
		thermalComponentsZList.add(new JScrollPane(thczTable));
		
		thermalComponents.setLayout(new GridLayout() );
		thermalComponents.add(thermalComponentsList);
		thermalComponents.add(thermalComponentsZList);
//		thermalComponents.add(changeComponentBtn);
		
		thermalChart.setLayout(new BorderLayout());
//		thermalChart.add(ChartManager.getChart(),BorderLayout.CENTER);
		
		
		buttonPanel.setLayout(new GridLayout());
//		buttonPanel.add(refreshBtn);
		buttonPanel.add(connectBtn);
		buttonPanel.add(disconnectBtn);
		buttonPanel.add(startBtn);
		buttonPanel.add(stopBtn);
		buttonPanel.add(steadyStateBtn);
		buttonPanel.add(transientBtn);
//		buttonPanel.add(changeBtn);
		
		
		getContentPane().setLayout(null);
		pieChartPanel.setLayout(null);
		xyChartPanel.setLayout(null);
		buttonPanel.setLayout(null);
		
		
		//initialize pie chart and xy chart
		//num components contains gps. LTE and dram which are not added to the application yet
		pieChart = ChartManager.getPieChart(new double[UserProperty.numOfComponent - 3], ThermTap.getComponentNames()); 
		xyChart = ChartManager.getXYChart(new double[3], new double[3]);
		
		pieChart.setBounds(10, 30, 400, 360);
		xyChart.setBounds(10, 25, 840, 360);
		
		pieChartPanel.add(pieChart);
		xyChartPanel.add(xyChart);
		
		Font borderFont = new Font("TimesRoman", Font.PLAIN, 20);
		
		xyChartPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.RED,2),"Per-Process Power Trace",
																								TitledBorder.CENTER, TitledBorder.TOP,borderFont,Color.RED));
		piduidPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.RED,2),"Applications/Processes",
				TitledBorder.CENTER, TitledBorder.TOP,borderFont,Color.RED));
		pieChartPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.RED,2),"Power Breakdown",
				TitledBorder.CENTER, TitledBorder.TOP,borderFont,Color.RED));
		thermalComponents.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.RED,2),"Device Components",
				TitledBorder.CENTER, TitledBorder.TOP,borderFont,Color.RED));
		thermalChart.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.RED,2),"3D Temperature Map",
				TitledBorder.CENTER, TitledBorder.TOP,borderFont,Color.RED));
//		thermTapPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.RED,2),"ControlPanel",
//																								TitledBorder.CENTER, TitledBorder.TOP,null,Color.RED));
		
		// ------------------------ initialize icons -----------------------
		try{
			double magnificationCoef = 0.26;
			int iconGap = 15;
			
			ImageIcon img = new ImageIcon(getClass().getResource("/icons/open.png"));
			img = new ImageIcon(img.getImage().getScaledInstance( (int)(img.getIconWidth()*magnificationCoef), (int)(img.getIconHeight()*magnificationCoef),  java.awt.Image.SCALE_SMOOTH )) ;
			openBtn.setIcon(img);
			openBtn.setIconTextGap(iconGap);
			
			img = new ImageIcon(getClass().getResource("/icons/refresh.png"));
			img = new ImageIcon(img.getImage().getScaledInstance( (int)(img.getIconWidth()*magnificationCoef), (int)(img.getIconHeight()*magnificationCoef),  java.awt.Image.SCALE_SMOOTH )) ;
			refreshBtn.setIcon(img);
			refreshBtn.setIconTextGap(iconGap);
			
			img = new ImageIcon(getClass().getResource("/icons/connect.png"));
			img = new ImageIcon(img.getImage().getScaledInstance( (int)(img.getIconWidth()*magnificationCoef), (int)(img.getIconHeight()*magnificationCoef),  java.awt.Image.SCALE_SMOOTH )) ;  
			connectBtn.setIcon(img);
			connectBtn.setIconTextGap(iconGap);
			
			img = new ImageIcon(getClass().getResource("/icons/disconnect.png"));
			img = new ImageIcon(img.getImage().getScaledInstance( (int)(img.getIconWidth()*magnificationCoef), (int)(img.getIconHeight()*magnificationCoef),  java.awt.Image.SCALE_SMOOTH )) ;  
			disconnectBtn.setIcon(img);
			disconnectBtn.setIconTextGap(iconGap);

			img = new ImageIcon(getClass().getResource("/icons/play.png"));
			img = new ImageIcon(img.getImage().getScaledInstance( (int)(img.getIconWidth()*magnificationCoef), (int)(img.getIconHeight()*magnificationCoef),  java.awt.Image.SCALE_SMOOTH )) ;  
			startBtn.setIcon(img);
			startBtn.setIconTextGap(iconGap);

			img = new ImageIcon(getClass().getResource("/icons/stop.png"));
			img = new ImageIcon(img.getImage().getScaledInstance( (int)(img.getIconWidth()*magnificationCoef), (int)(img.getIconHeight()*magnificationCoef),  java.awt.Image.SCALE_SMOOTH )) ;  
			stopBtn.setIcon(img);
			stopBtn.setIconTextGap(iconGap);

			img = new ImageIcon(getClass().getResource("/icons/steady.png"));
			img = new ImageIcon(img.getImage().getScaledInstance( (int)(img.getIconWidth()*magnificationCoef), (int)(img.getIconHeight()*magnificationCoef),  java.awt.Image.SCALE_SMOOTH )) ;  
			steadyStateBtn.setIcon(img);
			steadyStateBtn.setIconTextGap(iconGap);

			img = new ImageIcon(getClass().getResource("/icons/transient.png"));
			img = new ImageIcon(img.getImage().getScaledInstance( (int)(img.getIconWidth()*magnificationCoef), (int)(img.getIconHeight()*magnificationCoef),  java.awt.Image.SCALE_SMOOTH )) ;  
			transientBtn.setIcon(img);
			transientBtn.setIconTextGap(iconGap);

			img = new ImageIcon(getClass().getResource("/icons/change.png"));
			img = new ImageIcon(img.getImage().getScaledInstance( (int)(img.getIconWidth()*magnificationCoef), (int)(img.getIconHeight()*magnificationCoef),  java.awt.Image.SCALE_SMOOTH )) ;  
			changeBtn.setIcon(img);
			changeBtn.setIconTextGap(iconGap);

		}catch(Exception e){
			e.printStackTrace();
		}
		// -----------------------------------------------------------------
		
		buttonPanel.add(stopBtn);
		buttonPanel.add(startBtn);
		buttonPanel.add(disconnectBtn);
		buttonPanel.add(connectBtn);
//		buttonPanel.add(refreshBtn);
		buttonPanel.add(steadyStateBtn);
		buttonPanel.add(transientBtn);
		buttonPanel.add(openBtn);
//		buttonPanel.add(changeBtn);
//		buttonPanel.add(print);
		
		
		getContentPane().add(buttonPanel);
		getContentPane().add(xyChartPanel);
		getContentPane().add(piduidPanel);
		getContentPane().add(pieChartPanel);
		getContentPane().add(thermalComponents);
		getContentPane().add(thermalChart);
		//ThermTap.loadTherminatorOutput("deviceThermalComponents");
		
		// ---------------------------- JFrame charachteristics ----------------------------------
		setSize(new Dimension(1310,910));
		setResizable(false);
		setVisible(true);

		
		// ------------------ action listeners -------------------------------------
		openBtn.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent arg0) {
				File workingDirectory = new File(System.getProperty("user.dir"));
				fileChooser.setCurrentDirectory(workingDirectory);
				int rVal = fileChooser.showOpenDialog(UserInterface.this);
				
				
				if (rVal == JFileChooser.APPROVE_OPTION) {
					String fileName = fileChooser.getSelectedFile().getPath();
					//read the config file
					ThermTap.initTherminatorComponents(fileName);
					thcTableModel.addRows(ThermTap.getTherminatorComponents().keySet());
					thcTable.setEnabled(false);

					//make other buttons visible
					connectBtn.setEnabled(true);
		        }
			}
		});
			
		startBtn.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent arg0) {
				powertap.performAction(Actions.START);
				
				startBtn.setVisible(false);
				stopBtn.setVisible(true);
				
				steadyStateBtn.setEnabled(true);
				transientBtn.setEnabled(false);
				disconnectBtn.setEnabled(false);
			}
		});
		
		stopBtn.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent arg0) {
				powertap.performAction(Actions.STOP);
				stopBtn.setVisible(false);
				startBtn.setVisible(true);
				transientBtn.setEnabled(false);
				steadyStateBtn.setEnabled(false);
				disconnectBtn.setEnabled(true);
			}
		});
		
		changeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ThermTap.communicator.stopRunning();
				transientBtn.setEnabled(false);
				changeBtn.setEnabled(false);
			}
		});
		
		steadyStateBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFrame frame;
//				if(uid<0 || pid<0)
//					frame = new JFrame( "first chooose pid/uid" );
//				else{
					ThermTap.communicator.initTherminator(uid, pid);
					thcTable.setEnabled(true);
					transientBtn.setEnabled(true);
//				}
				//TODO:tell the therminator to initialize: create INIT file + power traces from beginning up to now
			}
		});
		
		refreshBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pidTable.getSelectionModel().clearSelection();
				uidTable.getSelectionModel().clearSelection();
				pidTableModel.deleteRows();
				uidTableModel.deleteRows();
				uidTableModel.addRows(powertap.getPhone().getUidMap());
				uid = pid = -1;
			}
		});
		
		transientBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(uid != 0 && pid != 0){
					changeBtn.setEnabled(true);
					ThermTap.communicator.startRunning();
					thcTable.setEnabled(true);
				}
			}
		});
		
		connectBtn.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent arg0) {
				//Does not change anything if it fails to connect
				if (powertap.performAction(Actions.CONNECT)){
					connectBtn.setVisible(false);
					disconnectBtn.setVisible(true);
					startBtn.setEnabled(true);
					openBtn.setEnabled(false);
					
					pidTableModel.deleteRows();
	//				pidTableModel.setHeader(columnNames);
					uidTableModel.addRows(powertap.getPhone().getUidMap());
	//				uidTableModel.setHeader(columnNames);
				}
			}
			
		});
		disconnectBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				powertap.performAction(Actions.DISCONNECT);
				connectBtn.setVisible(true);
				disconnectBtn.setVisible(false);
				startBtn.setEnabled(false);
				openBtn.setEnabled(true);
				
				pidTableModel.deleteRows();;
				uidTableModel.deleteRows();
			}
		});
		
		/*
		 * For debugging purposes
		 */
//		print.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent arg0) {
//				ThermTap.printStatsHumanReadable();
//			}
//		});
		
		uidTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				
				pidtLock = true;
				pidTable.getSelectionModel().clearSelection();
				if(uidTable.getSelectedRow() < 0 || uidTable.getSelectedRow() > (uidTable.getRowCount()-1) || 
						uidTable.getColumnCount() <= 0)
					return;
				UidNode node = powertap.getPhone().getUidMap()
						.get(Integer.parseInt(uidTable.getValueAt(uidTable.getSelectedRow(), 0).toString()));
				uid = node.getUid();
				pid = -1;
				pidTableModel.deleteRows();
				pidTable.addNotify();
				if(uid >= 0)
					pidTableModel.addRows(node.getPidList());
				pidtLock = false;
			}
		});
		
		pidTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				if(pidtLock)
					return;
				if(pidTable.getSelectedRow() < 0 ||
						pidTable.getSelectedRow() > (pidTable.getRowCount()-1) ||
						pidTable.getColumnCount() <= 0)
					return;
				pid = Integer.parseInt(pidTable.getValueAt(pidTable.getSelectedRow(), 0).toString());
			}
		});
		
		thcTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				thczLock = true;
				thczTable.getSelectionModel().clearSelection();
				
				
				int numLayers = ThermTap.getTherminatorComponents().get(thcTable.getValueAt(thcTable.getSelectedRow(), 0).toString());
				Integer[][] layers = new Integer[numLayers][1];
				for (int i = 0; i < numLayers; i++) {
					layers[i][0] = i+1;
				}
				thczTableModel.deleteRows();
				thczTableModel.addRowsArray(layers);
				thczLock = false;
			}
		});
		
		thczTable.addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseClicked(java.awt.event.MouseEvent evt) {
		    	if(thczLock)
					return;
				int layer = Integer.parseInt(thczTable.getValueAt(thczTable.getSelectedRow(), 0).toString());
				String compName = thcTable.getValueAt(thcTable.getSelectedRow(), 0).toString();
				thermalChart.removeAll();
				ChartManager.disposeThermalChart(thermalChartInstance);
				thermalChartInstance = ChartManager.getThermalChart(ThermTap.parseTherminatorOutput(compName, layer),0.03,0.01);
				thermalChart.add((Component)thermalChartInstance.getCanvas(),BorderLayout.CENTER);
				thermalChart.revalidate();
				thermalChart.repaint();
				getContentPane().repaint();
		    }
		});		
	}
	
	public void updatePidTable(Vector<String> header, Vector<Object> data){
//		pidTable.set
	}
}

class MyTableModel extends AbstractTableModel {

    private List<Object[]> rows;
    private List<String> header;
    int numCols, numRows;

    public MyTableModel() {
        rows = new ArrayList<Object[]>();
        header = new ArrayList<String>();
        numCols = numRows = 0;
    }

    @Override
    public int getRowCount() {
        return numRows;
    }

    @Override
    public int getColumnCount() {
        return numCols;
    }
    
    @Override
    public String getColumnName(int columnIndex) {
    	return header.get(columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object[] row = rows.get(rowIndex);
        return row[columnIndex];
    }

    public void addRow(Object[] row) {
        int rowCount = getRowCount();
        rows.add(row);
        fireTableRowsInserted(rowCount, rowCount);
        numRows ++;
    }   
    
    public void addRowsArray(Object[][] rows) {
        for(int i=0 ; i<rows.length ; i++){
        	addRow(rows[i]);
        }
    }   
    
    public void addRows(Set<String> rows) {
    	Object[][] obj = new Object[rows.size()][1];
    	int i = 0;
        for(String s : rows){
        	obj[i][0] = s;
        	i++;
        }
        addRowsArray(obj);
    }   
    
    public void addRows(HashMap<Integer, UidNode> data) {
        for (UidNode node : data.values()) {
        	if(node.getPidList().size() > 0)	//do not show junk data!
        		if(node.getUid() == -1)
        			addRow(new Object[]{-1, "Total System"});
        		else
        			addRow(new Object[]{node.getUid() , node.getName()});
        	
		}
    } 
    
    public void addRows( ArrayList<PidNode> data) {
        for (PidNode pid : data) {
        	addRow(new Object[]{pid.getPid() , pid.getName()});
		}
    } 
    
    public void deleteRows(){
    	numRows = 0;
    	rows.clear();
//    	 rows = new ArrayList<Object[]>();
    }
   
    public void setHeader(String[] header){
    	for(int i = 0 ; i < header.length ; i++){
    		this.header.add(header[i]);
    		numCols ++;
    	}
    }
}    

class ForcedListSelectionModel extends DefaultListSelectionModel {

    public ForcedListSelectionModel () {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

}

