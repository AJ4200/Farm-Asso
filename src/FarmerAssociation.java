import acsse.csc2b.ui.FarmerAssociationPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @author Sthe 17
 *
 */
public class FarmerAssociation extends Application{

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		FarmerAssociationPane root = new FarmerAssociationPane(primaryStage);
		primaryStage.setTitle("Farmer's Accociation: Corn Crop Health Detection");
		primaryStage.setScene(new Scene(root,600,400));
		primaryStage.show();
	}

}
