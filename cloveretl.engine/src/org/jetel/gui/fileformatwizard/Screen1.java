package org.jetel.gui.fileformatwizard;

import javax.swing.JFrame;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JTextPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Font;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import org.jetel.gui.component.FormInterface;
import org.jetel.metadata.DataRecordMetadata;
import org.jetel.data.Defaults;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.Channels;

public class Screen1 extends JPanel implements FormInterface
{
	 private GridBagLayout gridBagLayout1 = new GridBagLayout();
	 private JLabel jLabel1 = new JLabel();
	 private JButton FileChooserButton = new JButton();
	 private JLabel jLabel2 = new JLabel();
	 private JLabel jLabel3 = new JLabel();
	 private JTextPane jTextPane1 = new JTextPane();
	 private JLabel jLabel4 = new JLabel();
	 private JRadioButton FixedWidthRadioButton = new JRadioButton();
	 private JRadioButton DelimitedRadioButton = new JRadioButton();
	 private JPanel jPanel1 = new JPanel();
	 private JLabel jLabel5 = new JLabel();
	 
	 private FileFormatDispatcher aDispatcher;
	 private FileFormatDataModel aFileFormatParameters;
	 private String[] linesFromFile = new String[5];
	
  public Screen1(FileFormatDispatcher aDispatcher, FileFormatDataModel aFileFormatParameters)
  {
  	this.aDispatcher = aDispatcher;
  	this.aFileFormatParameters = aFileFormatParameters;
  	
	    try
    {
      jbInit();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }

  }
  
  public Screen1() {
		try
	{
	  jbInit();
	}
	catch(Exception e)
	{
	  e.printStackTrace();
	}

  }

  public static void main(String[] args)
  {
    Screen1 filePreviewFormatPanel = new Screen1();
	String[] linesFromFile = null;
	filePreviewFormatPanel.setPreviewFileNameLabel("IAC_Data\\INF\\EP\\FERC\\FERC_423\\RawData\\Final Files");
	JFrame f = new JFrame("RulerPanel");
  f.addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent e) {System.exit(0);}
  });
  f.getContentPane().add("Center", filePreviewFormatPanel);
  //f.setSize(new Dimension(400,20));
  f.pack();
  f.show();
}

  private void jbInit() throws Exception
  {
	this.setLayout(gridBagLayout1);
    this.setSize(new Dimension(425, 303));
   
	loadData();
   jLabel1.setText("Screen 1 of 4");
   jLabel1.setFont(new Font("Dialog", 1, 12));
   FileChooserButton.setText("Select File");
   FileChooserButton.addActionListener(new ActionListener()
	 {
	   public void actionPerformed(ActionEvent e)
	   {
		 FileChooserButton_actionPerformed(e);
	   }
	 });
   jLabel3.setText("Preview File (first 5 lines):");
   jLabel3.setFont(new Font("Dialog", 1, 11));
   jLabel4.setText("Choose the file type that best describes your data:");
   jLabel4.setFont(new Font("Dialog", 1, 11));
   FixedWidthRadioButton.setText("Fixed Width - Fields are aligned in columns with optional spaces " + "between them.");
   DelimitedRadioButton.setText("Delimited     - Characters such as tab, comma separate fields.");
   jLabel5.setText("Select File");
   jLabel5.setAlignmentY((float)0.0);
   jLabel5.setAlignmentX((float)0.5);
   jLabel5.setHorizontalTextPosition(SwingConstants.CENTER);
   jLabel5.setHorizontalAlignment(SwingConstants.CENTER);
   this.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
   this.add(FileChooserButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
   this.add(jLabel2, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
   this.add(jLabel3, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
   JScrollPane scrollPane = new JScrollPane(jTextPane1,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
   this.add(scrollPane, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 50));
   this.add(jLabel4, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
   this.add(FixedWidthRadioButton, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
   this.add(DelimitedRadioButton, new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 10, 0, 0), 0, 0));
   this.add(jPanel1, new GridBagConstraints(1, 7, 2, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
   this.add(jLabel5, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

   // Group the radio buttons.
   ButtonGroup group = new ButtonGroup();
   group.add(FixedWidthRadioButton);
   group.add(DelimitedRadioButton);
  }
  
  public void setPreviewFileNameLabel(String previewFileNameLabel) {
	jLabel2.setText( previewFileNameLabel);
  }

  public String getPreviewFileNameLabel() {
	  return jLabel2.getText();
  }

private void FileChooserButton_actionPerformed(ActionEvent e)
{
  //Create a file chooser
  final JFileChooser fc = new JFileChooser();

  //In response to a button click:
  int returnVal = fc.showOpenDialog(this);
  if (returnVal == JFileChooser.APPROVE_OPTION) {
		setPreviewFileNameLabel(fc.getSelectedFile().getPath());
		File file = fc.getSelectedFile();
		try {
			BufferedReader reader = new BufferedReader(Channels.newReader( (new FileInputStream(file).getChannel()), Defaults.DataParser.DEFAULT_CHARSET_DECODER ) );
			try {
				int i = 0;
				String line = null;
				StringBuffer buf = new StringBuffer();
				line =reader.readLine();
				while(line != null) {
					buf.append( line ).append("\n" );	
					if(i< linesFromFile.length) {
						linesFromFile[i] = line;
						i++;
					}
					line = reader.readLine();
				}
				reader.close();
				jTextPane1.setText(buf.toString());
				aDispatcher.getAFileFormatWizard().enableNextButton();

			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	  }
}


	public void loadData() {
		   if(aFileFormatParameters != null) {
				if(aFileFormatParameters.fileName != null) {   	
					jLabel2.setText(aFileFormatParameters.fileName);
				}
				if(aFileFormatParameters.linesFromFile != null) {
					StringBuffer buf = new StringBuffer();
					for(int i = 0 ; i < aFileFormatParameters.linesFromFile.length; i++ ) {
						if( aFileFormatParameters.linesFromFile[i] != null)
							buf.append(aFileFormatParameters.linesFromFile[i] ).append("\n" );	
					}
					jTextPane1.setText(buf.toString());
				}
				if( aFileFormatParameters.isFileDelimited ) {
					DelimitedRadioButton.setSelected(true);
				} else {
					FixedWidthRadioButton.setSelected(true);
				}
		   } else {
			jLabel2.setText("(No File Selected)");
			jTextPane1.setText("");
		   }
	}

	/* (non-Javadoc)
	 * @see org.jetel.gui.component.PhasedPanelInterface#validateData()
	 */
	public boolean validateData() {
		// at minimum we need to have file selected
		if(jLabel2.getText().equalsIgnoreCase("(No File Selected)")){
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.jetel.gui.component.PhasedPanelInterface#saveData()
	 */
	public void saveData() {
		aFileFormatParameters.fileName = jLabel2.getText();
		aFileFormatParameters.recordMeta.setName(jLabel2.getText());
		if(DelimitedRadioButton.isSelected()){
			aFileFormatParameters.recordMeta.setRecType(DataRecordMetadata.DELIMITED_RECORD);
		} else {
			aFileFormatParameters.recordMeta.setRecType(DataRecordMetadata.FIXEDLEN_RECORD);
		}
		aFileFormatParameters.isFileDelimited = DelimitedRadioButton.isSelected();
		aFileFormatParameters.linesFromFile = linesFromFile;
	}

}