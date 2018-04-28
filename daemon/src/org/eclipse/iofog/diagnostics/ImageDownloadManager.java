package org.eclipse.iofog.diagnostics;

import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class ImageDownloadManager {
    private static final String MODULE_NAME = "Image Download Manager";

    private static ImageDownloadManager instance = null;

    public static ImageDownloadManager getInstance() {
        if (instance == null) {
            synchronized (ImageDownloadManager.class) {
                if (instance == null)
                    instance = new ImageDownloadManager();
            }
        }
        return instance;
    }

    public void createImageSnapshot(Orchestrator orchestrator, String imageName) {
        CommandShellResultSet<List<String>, List<String>> resultSetWithImageName =
                CommandShellExecutor.executeCommand("docker ps | grep " + imageName);
        String image;
        if (resultSetWithImageName.getValue() != null && resultSetWithImageName.getValue().size() > 0
                && resultSetWithImageName.getValue().get(0) != null) {
            image = resultSetWithImageName.getValue().get(0).split("\\s+")[1];

        } else {
            throw new IllegalArgumentException();
        }
        String imageZip = imageName + ".tar.gz";

        CommandShellResultSet<List<String>, List<String>> resultSetForCreateZip =
                CommandShellExecutor.executeCommand("docker save " + image + " | gzip -c > " + imageZip);

        CommandShellResultSet<List<String>, List<String>> resultSetWithPath =
                CommandShellExecutor.executeCommand("readlink -f " + imageZip);
        if (resultSetWithPath.getError().size() > 0) {
            LoggingService.logWarning(MODULE_NAME, resultSetWithPath.toString());
        } else {
            String path = resultSetWithPath.getValue().get(0);
            try {
                orchestrator.sendFileToController("imageSnapshotPut", getFileByFilePath(path),
                        "elementId", imageName );
            } catch (Exception e) {
                logWarning(MODULE_NAME, "unable send image snapshot path : " + e.getMessage());
            }
        }
    }

    private File getFileByFilePath(String path) {

        URL url = null;
        try {
            url = new URL("file://" + path);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        File file = null;
        if ((url != null ? url.getPath() : null) != null) {
            file = new File(url.getPath());
        }

        return file;
    }


}
