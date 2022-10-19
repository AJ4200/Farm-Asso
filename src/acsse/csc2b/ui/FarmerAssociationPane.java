/**
 * 
 */
package acsse.csc2b.ui;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.HashMap;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * @author Sthe 17
 *
 */
public class FarmerAssociationPane extends GridPane{

	//ui elements
	private Button btnPickImg = null;
	private Button btnDiagnose = null;
	private TextArea taResults = null;
	private ImageView imgPreview = null;
	
	//URLs
	final static String GRAY_URL = "/api/GrayScale";
	final static String DILATION_URL = "/api/Dilation";
	final static String EROSION_URL = "/api/Erosion";
	final static String ORB_URL = "/api/ORB";
	final static String FAST_URL = "/api/Fast";
	final static String CANNY_URL = "/api/Canny";
	final static String ORB_FEAT_URL = "/api/ORBFeatures";
	final static String FAST_FEAT_URL = "/api/FastFeatures";
	final static String CANNY_FEAT_URL = "/api/CannyFeatures";
	
	//Trained Model
	final static double[] BLIGHT_CROP = {73.47058824, 146.1633987, 4545.671024};
	final static double[] COMMON_RUST_CROP = {64.78213508, 128.9738562, 4460.305011};
	final static double[] GRAY_LEAF_SPOT_CROP = {75.39651416, 149.9368192, 3751.328976};
	final static double[] HEALTHY_CROP = {91.77995643, 182.4030501, 1649.708061};
	final static int MAX_CROP_FEATS = 90000;
	final static int MAX_BOUND = 300;
	
	//streams and socketsSocket s = null;
	private Socket client = null;
	private BufferedReader txtin = null;
	private DataOutputStream dos = null;
	private BufferedOutputStream bos = null;
	private OutputStream os = null;
	private InputStream is = null;
	private File originalImg = null;
	private String encoded = null;
	private int lower_b = 0;
	private int upper_b = 0;
	private int fast = 0;
	
	public FarmerAssociationPane(Stage stage) {
		setupUI();
		
		btnPickImg.setOnAction(e -> {
			FileChooser fc = new FileChooser();
			originalImg = fc.showOpenDialog(stage);
			imgPreview.setImage(new Image("file:"+ originalImg.getAbsolutePath()));
			encodeImage(originalImg);
		});
		
		btnDiagnose.setOnAction(e -> {
			taResults.clear();
			String imgStr = "";
			//Decoding the read data
			imgStr = getAPIFunc(encoded, CANNY_URL);
			String decodedCanny = imgStr.substring(imgStr.indexOf('\'') +1, imgStr.lastIndexOf('}')-1);

			imgStr = getAPIFunc(encoded, CANNY_FEAT_URL);
			lower_b = Integer.parseInt(imgStr.substring(imgStr.lastIndexOf('[') +1, imgStr.lastIndexOf(',')));
			upper_b = Integer.parseInt(imgStr.substring(imgStr.lastIndexOf(',') +2, imgStr.lastIndexOf(']')-2));
			
			imgStr = getAPIFunc(encoded, FAST_FEAT_URL);
			fast = Integer.parseInt(imgStr.substring(imgStr.indexOf('[') +1, imgStr.lastIndexOf('}')-1));
			
			taResults.appendText(diagnoseImage());
			
			byte[] decodedBytes = Base64.getDecoder().decode(decodedCanny);
			//Display the Canny image
			Image greyImg = new Image(new ByteArrayInputStream(decodedBytes));
			imgPreview.setImage(greyImg);
		});
	}

	private String diagnoseImage() {
		double healthyProb = 0;
		double blightProb = 0;
		double grayProb = 0;
		double rustProb = 0;
		
		if(fast < 6000) {
			healthyProb += 50;
			blightProb += 35;
			rustProb += 17;
			grayProb += 22;
		} else if((fast>=6000) && (fast < 12000)) {
			blightProb += 22;
			rustProb += 6;
			grayProb += 9;
		} else if((fast>=12000) && (fast < 20000)) {
			blightProb += 12;
			rustProb += 8;
			grayProb += 5;
		} else if((fast>=20000) && (fast < 30000)) {
			blightProb += 6;
			rustProb += 9;
			grayProb += 4;
		} else if((fast>=30000) && (fast < 40000)) {
			rustProb += 6;
		} else if((fast>=40000) && (fast < 60000)) {
			blightProb += 1;
			rustProb += 3;
			grayProb += 1;
		} else if((fast>=60000) && (fast < 80000)) {
			blightProb += 4;
			rustProb += 2;
		}
		
		if(lower_b < 50) {
			blightProb += 5;
			rustProb += 10;
			grayProb += 2;
			healthyProb += 5;
		} else if((lower_b >= 50) && (lower_b<100)) {
			blightProb += 18;
			rustProb += 15;
			grayProb += 17;
			healthyProb += 11;
		} else if((lower_b >= 100) && (lower_b<150)) {
			blightProb += 2;
			grayProb += 6;
			healthyProb += 9;
		}
		
		if(upper_b < 50) {
			blightProb += 1;
			rustProb += 5;
		} else if((lower_b >= 50) && (lower_b<100)) {
			blightProb += 3;
			rustProb += 5;
			grayProb += 3;
			healthyProb += 6;
		} else if((lower_b >= 100) && (lower_b<150)) {
			blightProb += 13;
			rustProb += 11;
			grayProb += 11;
			healthyProb += 1;
		} else if((lower_b >= 150) && (lower_b<200)) {
			blightProb += 9;
			rustProb += 3;
			grayProb += 10;
			healthyProb += 12;
		} else if((lower_b >= 200) && (lower_b<250)) {
			blightProb += 2;
			rustProb += 1;
			grayProb += 1;
			healthyProb += 5;
		} else if((lower_b >= 250) && (lower_b<300)) {
			healthyProb += 1;
		}
		
		/*
		double bCE = 40*(Math.abs(BLIGHT_CROP[2] - fast))/MAX_CROP_FEATS;
		double rCE = 40*(Math.abs(COMMON_RUST_CROP[2] - fast))/MAX_CROP_FEATS;
		double gCE = 40*(Math.abs(GRAY_LEAF_SPOT_CROP[2] - fast))/MAX_CROP_FEATS;
		double hCE = 40*(Math.abs(HEALTHY_CROP[2] - fast))/MAX_CROP_FEATS;
		
		bCE += 30*(Math.abs(BLIGHT_CROP[0] - lower_b))/MAX_BOUND;
		rCE += 30*(Math.abs(COMMON_RUST_CROP[0] - lower_b))/MAX_BOUND;
		gCE += 30*(Math.abs(GRAY_LEAF_SPOT_CROP[0] - lower_b))/MAX_BOUND;
		hCE += 30*(Math.abs(HEALTHY_CROP[0] - lower_b))/MAX_BOUND;
		bCE += 30*(Math.abs(BLIGHT_CROP[1] - upper_b))/MAX_BOUND;
		rCE += 30*(Math.abs(COMMON_RUST_CROP[1] - upper_b))/MAX_BOUND;
		gCE += 30*(Math.abs(GRAY_LEAF_SPOT_CROP[1] - upper_b))/MAX_BOUND;
		hCE += 30*(Math.abs(HEALTHY_CROP[1] - upper_b))/MAX_BOUND;
		*/
		
		DecimalFormat f = new DecimalFormat("##.00");
		String result = "Healthy Probability: " + f.format(healthyProb) + "\n";
		result += "Blight Infection Probability: " + f.format(blightProb) + "\n";
		result += "Common Rust Probability: " + f.format(rustProb) + "\n";
		result += "Gray Leaf Spot Infection Probability: " + f.format(grayProb) + "\n";
		
		return result;
	}

	private void estConnection() {
		// TODO Auto-generated method stub
		try {
			client = new Socket("localhost",5000);
			//bind all streams
			is = client.getInputStream();
			txtin = new BufferedReader(new InputStreamReader(is));
			os = client.getOutputStream();
			bos = new BufferedOutputStream(os);
			dos = new DataOutputStream(bos);
			
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void encodeImage(File img) {
		// TODO Auto-generated method stub
		FileInputStream fis;
		try {
			fis = new FileInputStream(img);
			//Get bytes of image for encoding
			byte[] bytes = new byte[(int)img.length()];
			fis.read(bytes);
			//Encoding into base64 string
			encoded = new String(Base64.getEncoder().encodeToString(bytes));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private String getAPIFunc(String encodedImg, String url) {
		// TODO Auto-generated method stubString imgStr = "";
		estConnection();
		String imgData = "";
		try {
			//byte to send
			byte[] outBytes = encodedImg.getBytes();
			
			//Construct POST HTTP request
			dos.write(("POST " + url + " HTTP/1.1\r\n").getBytes());
			dos.write(("Content-Type: " + "application/text\r\n").getBytes());
			dos.write(("Content-Length: " + encodedImg.length() + "\r\n").getBytes());
			dos.write(("\r\n").getBytes());
			dos.write(outBytes);
			dos.flush();
			dos.write(("'\r\n").getBytes());
			
			System.out.println("POST REQUEST sent successfully");
			
			//Receive header response
			String response = "";
			String line = "";
			while(!(line = txtin.readLine()).equals("")) {
				response += line + "\n";
			}
			System.out.println(response);
			
			//Receive image data
			imgData = "";
			while((line = txtin.readLine()) != null) {
				imgData += line;
			}
			System.out.println(imgData);
			
		} catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return imgData;
	}

	private void setupUI() {
		// TODO Auto-generated method stub
		btnPickImg = new Button("Select Image");
		btnDiagnose = new Button("Diagnose Image");
		taResults = new TextArea();
		taResults.setPrefHeight(180);
		taResults.setPrefWidth(250);
		imgPreview = new ImageView();
		imgPreview.setFitHeight(320);
		imgPreview.setFitWidth(320);

		setVgap(10);
		setHgap(10);
		setAlignment(Pos.CENTER);
		
		add(btnPickImg, 0, 0);
		add(btnDiagnose, 1, 0);
		add(imgPreview, 3, 0, 4, 4);
		add(taResults, 0, 2, 3, 3);
	}
}
