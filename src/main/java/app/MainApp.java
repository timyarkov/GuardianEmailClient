package app;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import model.db.GEDatabaseSQLite;
import model.system.GESystem;
import model.system.GESystemImpl;
import presenter.GEPresenter;
import presenter.GEPresenterImpl;
import view.scenes.MainScene;

import java.util.List;

/**
 * Main Application class.
 */
public class MainApp extends Application {
    // Keep references for shutdown
    private GESystem ges;
    private GEPresenter p;

    public void start(Stage stage) {
        // Setup
        Parameters args = getParameters();
        List<String> argsStr = args.getRaw();

        GESystem ges;

        boolean gOnline;
        boolean eOnline;
        boolean rOnline;

        if (argsStr.size() <= 1) {
            // Assume all offline if bad arg amount
            gOnline = false;
            eOnline = false;
            rOnline = false;
        } else {
            if (argsStr.get(0).equals("online")) {
                gOnline = true;
            } else if (argsStr.get(0).equals("offline")) {
                gOnline = false;
            } else {
                System.out.println(argsStr.get(0) + " is not a valid argument!");
                return;
            }

            if (argsStr.get(1).equals("online")) {
                eOnline = true;
            } else if (argsStr.get(1).equals("offline")) {
                eOnline = false;
            } else {
                System.out.println(argsStr.get(1) + " is not a valid argument!");
                return;
            }

            try {
                if (argsStr.get(2).equals("online")) {
                    rOnline = true;
                } else if (argsStr.get(2).equals("offline")) {
                    rOnline = false;
                } else {
                    System.out.println(argsStr.get(2) + " is not a valid argument!");
                    return;
                }
            } catch (IndexOutOfBoundsException e) {
                // Only 2 args, set reddit to be same as email
                rOnline = eOnline;
            }
        }

        // If online for Guardian, create along with DB, else don't use DB at all
        ges = new GESystemImpl(gOnline,
                               eOnline,
                               rOnline,
                               gOnline ? new GEDatabaseSQLite("gedata.db") : null);

        // Do pre-check; do not proceed if online we don't have variables
        if (!ges.checkEnvironmentVars(gOnline, eOnline)) {
            return;
        }

        GEPresenter p = new GEPresenterImpl(stage, ges, getHostServices());
        ges.addObserver(p);

        this.ges = ges;
        this.p = p;

        p.setScene(new MainScene());

        stage.getIcons().add(new Image("gelogo.png"));
        stage.setMinWidth(GEPresenter.MIN_X);
        stage.setMinHeight(GEPresenter.MIN_Y);

        stage.show();
    }

    @Override
    public void stop() {
        ges.shutdown();
        p.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
