package org.first.team2485.scoutingform.bluetooth2;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.DefaultCaret;

import org.first.team2485.scoutingform.ScoutingForm;

/**
 * 
 * @author Nicholas Contreras
 *
 */

public class BluetoothPanel extends JPanel implements ListCellRenderer<ExpandedRemoteDevice> {

	private JFrame frame;

	private String fileName, dataToSend;

	private JList<ExpandedRemoteDevice> deviceList;
	private JTextArea console;

	private JButton sendButton, refreshButton, clearConsoleButton;

	private PrintStream oldPrintStream;

	private boolean stop;

	public static void main(String[] args) {
		new BluetoothPanel("tripplecow.txt", "test test 1 2 3");
	}

	public BluetoothPanel(String fileName, String dataToSend) {

		oldPrintStream = System.out;

		System.setOut(new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				writeToConsole(((char) b) + "");
			}
		}));

		this.fileName = fileName;
		this.dataToSend = dataToSend;

		setUpPanel();
		setUpMainWindows();
		setUpButtons();

		frame.pack();
		frame.setVisible(true);

		new PaintThread().start();
	}

	class PaintThread extends Thread {

		@Override
		public void run() {

			while (!stop) {

				frame.repaint();

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				continue;
			}

		}
	}

	private void setUpPanel() {

		frame = new JFrame("Bluetooth Connection Window");

		frame.add(this);

		this.setLayout(new BorderLayout());

	}

	private void setUpMainWindows() {

		JLabel deviceListLabel = new JLabel("Known Devices", SwingConstants.CENTER);
		JLabel consoleLabel = new JLabel("Console Output", SwingConstants.CENTER);

		BluetoothActionListener.setBluetoothPanel(this);

		DefaultListModel<ExpandedRemoteDevice> listModel = new DefaultListModel<ExpandedRemoteDevice>();
		deviceList = new JList<ExpandedRemoteDevice>(listModel);
		deviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		deviceList.setCellRenderer(this);

		console = new JTextArea();

		JScrollPane deviceListScroller = new JScrollPane(deviceList);
		JScrollPane consoleScroller = new JScrollPane(console);

		DefaultCaret caret = (DefaultCaret) console.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		JPanel mainWindow = new JPanel(new GridLayout(1, 2));

		JPanel leftCol = new JPanel();
		JPanel rightCol = new JPanel();
		leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
		rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));

		leftCol.add(deviceListLabel);
		leftCol.add(deviceListScroller);

		rightCol.add(consoleLabel);
		rightCol.add(consoleScroller);

		mainWindow.add(leftCol);
		mainWindow.add(rightCol);

		this.add(mainWindow, BorderLayout.CENTER);

	}

	private void setUpButtons() {

		JPanel buttonPanel = new JPanel(new GridLayout(1, 3));

		sendButton = new JButton("Send Data");
		refreshButton = new JButton("Scan For Devices");
		clearConsoleButton = new JButton("Clear Console");

		sendButton.setActionCommand("sendButton");
		sendButton.addActionListener(new BluetoothActionListener());

		refreshButton.setActionCommand("refreshButton");
		refreshButton.addActionListener(new BluetoothActionListener());

		clearConsoleButton.setActionCommand("clearButton");
		clearConsoleButton.addActionListener(new BluetoothActionListener());

		buttonPanel.add(sendButton);
		buttonPanel.add(refreshButton);
		buttonPanel.add(clearConsoleButton);

		this.add(buttonPanel, BorderLayout.SOUTH);
	}

	public ExpandedRemoteDevice getSelectedDevice() {
		return deviceList.getSelectedValue();
	}

	public String getFileName() {
		return fileName;
	}

	public String getDataToSend() {
		return dataToSend;
	}

	public JTextArea getConsole() {
		return console;
	}

	public void preformFullScan() {

		ExpandedRemoteDevice[] alreadyPaired = BluetoothSystem.pairedDevices();

		addToList(alreadyPaired);

		int input = JOptionPane.showConfirmDialog(null,
				"The preliminary query has gotten " + alreadyPaired.length
						+ " responces. Would you like to evaluate these devices now?",
				"Bluetooth System", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null);

		if (input == JOptionPane.YES_OPTION) {

			for (ExpandedRemoteDevice curDevice : alreadyPaired) {

				System.out.println("****************");

				System.out.println("Setting Values For: " + curDevice.getName());

				BluetoothSystem.setValuesForDevice(curDevice, BluetoothSystem.OBEX);

				System.out.println("Just updated values for: " + curDevice.getName());

				System.out.println("Value is now: " + curDevice.state);

			}
		}

		ExpandedRemoteDevice[] newDevices = BluetoothSystem.discoverDevices();

		addToList(newDevices);

		for (ExpandedRemoteDevice curDevice : newDevices) {

			System.out.println("****************");

			System.out.println("Setting Values For: " + curDevice.getName());

			BluetoothSystem.setValuesForDevice(curDevice, BluetoothSystem.OBEX);

			System.out.println("Just updated values for: " + curDevice.getName());

			System.out.println("Value is now: " + curDevice.state);

		}

		System.out.println("Scan complete");

	}

	private void addToList(ExpandedRemoteDevice[] newDevices) {

		DefaultListModel<ExpandedRemoteDevice> listModel = (DefaultListModel<ExpandedRemoteDevice>) deviceList
				.getModel();

		for (ExpandedRemoteDevice newDevice : newDevices) {

			System.out.println("Attempting to add: " + newDevice.getName());

			boolean breakOut = false;

			for (int i = 0; i < listModel.size(); i++) {

				if (listModel.get(i).equals(newDevice) || listModel.get(i).getRemoteDevice().getBluetoothAddress()
						.equals(newDevice.getRemoteDevice().getBluetoothAddress())) {

					System.out.println("Found equal");

					System.out.println(
							"Old Bluetooth Address: " + listModel.get(i).getRemoteDevice().getBluetoothAddress());
					System.out.println("New device Address: " + newDevice.getRemoteDevice().getBluetoothAddress());

					listModel.set(i, newDevice);

					breakOut = true;
					break;
				}
			}

			if (breakOut) {
				continue;
			}

			listModel.addElement(newDevice);
		}

		System.out.println("Added new values");

	}

	private void writeToConsole(String s) {
		console.setText(console.getText() + s);
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends ExpandedRemoteDevice> list,
			ExpandedRemoteDevice value, int index, boolean isSelected, boolean cellHasFocus) {

		JTextField text = new JTextField(value.getName());

		switch (value.state) {

		case ExpandedRemoteDevice.OBEX_SUPPORTED:
			text.setBackground(Color.GREEN);
			text.setToolTipText("This device is ready to recieve files");
			break;

		case ExpandedRemoteDevice.UNCHECKED_DEVICE:
			text.setBackground(Color.YELLOW);
			text.setToolTipText("This device has been detected, but its status is unknown");
			break;

		case ExpandedRemoteDevice.OBEX_UNSUPPORTED:
			text.setBackground(Color.RED);
			text.setToolTipText("This device does not support file transfers");
			break;
		}

		if (isSelected) {
			text.setBackground(text.getBackground().darker());
			text.setText(text.getText() + " <--- SELECTED");
		}

		return text;
	}

	public void shutdownBluetooth() {
		System.setOut(oldPrintStream);

		stop = true;
		
		ScoutingForm.main(null); //Open up another form

		frame.dispose();
	}
}