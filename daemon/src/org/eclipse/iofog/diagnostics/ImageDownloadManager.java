/*
 * *******************************************************************************
 *  * Copyright (c) 2018 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */

package org.eclipse.iofog.diagnostics;

import com.github.dockerjava.api.model.Container;
import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.process_manager.DockerUtil;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.JsonObject;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.eclipse.iofog.utils.logging.LoggingService.logError;
import static org.eclipse.iofog.utils.logging.LoggingService.logInfo;
import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class ImageDownloadManager {

    private static final String MODULE_NAME = "Image Download Manager";

    public static void createImageSnapshot(Orchestrator orchestrator, String microserviceUuid) {
        Optional<Container> containerOptional = DockerUtil.getInstance().getContainer(microserviceUuid);
        String image;
        if (containerOptional.isPresent()) {
            Container container = containerOptional.get();
            image = container.getImage();
        } else {
            logWarning(MODULE_NAME, "Image snapshot: container not running.");
            return;
        }

        String imageZip = microserviceUuid + ".tar.gz";
        String imagePath = "/tmp/" + imageZip;
        CommandShellExecutor.executeCommand("docker save " + image + " | gzip -c > " + imagePath);

        CommandShellResultSet<List<String>, List<String>> resultSetWithPath =
                CommandShellExecutor.executeCommand("readlink -f " + imagePath);
        if (resultSetWithPath.getError().size() > 0) {
            LoggingService.logWarning(MODULE_NAME, resultSetWithPath.toString());
        } else {
            String path = resultSetWithPath.getValue().get(0);
            try {
                //TODO: think about send few files
                File imageFile = getFileByImagePath(path);
                orchestrator.sendFileToController("image-snapshot", imageFile);
                imageFile.delete();
                logInfo(MODULE_NAME, "Image snapshot " + imageFile.getName() + " deleted");
            } catch (Exception e) {
                logError(MODULE_NAME, "Unable send image snapshot path : " + e.getMessage(), e);
            }
        }
    }

    private static File getFileByImagePath(String path) {

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
