/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2024 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.pruning;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PruneResponse;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.process_manager.DockerUtil;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DockerPruningManagerTest {
    private DockerPruningManager pruningManager;
    private MockedStatic<DockerUtil> dockerUtilMockedStatic;
    private MockedStatic<MicroserviceManager> microserviceManagerMockedStatic;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private Container container = null;
    private String unwantedImageID = null;
    private List<Image> images = null;
    private Method method = null;
    private PruneResponse pruneResponse = null;
    private DockerUtil dockerUtil;
    private MicroserviceManager microserviceManager;


    @BeforeEach
    public void setUp() throws Exception {
        setMock(pruningManager);
        dockerUtil = Mockito.mock(DockerUtil.class);
        microserviceManagerMockedStatic = mockStatic(MicroserviceManager.class);
        dockerUtilMockedStatic = mockStatic(DockerUtil.class);
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
        microserviceManager = Mockito.mock(MicroserviceManager.class);
        Mockito.when(DockerUtil.getInstance()).thenReturn(dockerUtil);
        Mockito.when(MicroserviceManager.getInstance()).thenReturn(microserviceManager);
        images = new ArrayList<>();
        Image unwantedImage = Mockito.mock(Image.class);
        Image wantedImage = Mockito.mock(Image.class);
        String[] unwantedTags = {"none:<none>"};
        String[] wantedTags = {"edgeworx/calibration-sensors-arm"};
        Mockito.when(unwantedImage.getRepoTags()).thenReturn(unwantedTags);
        unwantedImageID = "unwantedImage";
        Mockito.when(unwantedImage.getId()).thenReturn(unwantedImageID);
        Mockito.when(wantedImage.getRepoTags()).thenReturn(wantedTags);
        images.add(unwantedImage);
        images.add(wantedImage);
        Mockito.when(dockerUtil.getImages()).thenReturn(images);
        List<Microservice> latestMicroservices = new ArrayList<>();
        Microservice microservice = new Microservice("uuid", "edgeworx/calibration-sensors-arm");
        latestMicroservices.add(microservice);
        Mockito.when(microserviceManager.getLatestMicroservices()).thenReturn(latestMicroservices);
        pruneResponse = mock(PruneResponse.class);
        Mockito.when(dockerUtil.dockerPrune()).thenReturn(pruneResponse);
        Mockito.doNothing().when(dockerUtil).removeImageById(anyString());
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        pruningManager = Mockito.spy(DockerPruningManager.getInstance());
        container = Mockito.mock(Container.class);
    }

    @AfterEach
    public void tearDown() throws Exception {
        microserviceManagerMockedStatic.close();
        dockerUtilMockedStatic.close();
        loggingServiceMockedStatic.close();
        container = null;
        images = null;
        pruneResponse = null;
        Field instance = DockerPruningManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        Mockito.reset(microserviceManager, dockerUtil, pruningManager);
        pruningManager = null;
        if (method != null)
            method.setAccessible(false);

    }

    /**
     * Set a mock to the {@link DockerPruningManager} instance
     * Throws {@link RuntimeException} in case if reflection failed, see a {@link Field#set(Object, Object)} method description.
     * @param mock the mock to be inserted to a class
     */
    private void setMock(DockerPruningManager mock) {
        try {
            Field instance = DockerPruningManager.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test GetUnwantedImagesList when no non iofog containers are running
     */
    @Test
    public void testGetUnwantedImagesListWhenNoNonIoFogContainersRunning() {
        Set<String> imageIDs = pruningManager.getUnwantedImagesList();
        Mockito.verify(microserviceManager).getLatestMicroservices();
        Mockito.verify(dockerUtil).getRunningNonIofogContainers();
        Mockito.verify(dockerUtil).getImages();
        assertTrue(imageIDs.contains(unwantedImageID));
        assertEquals(1, imageIDs.size());
    }

    /**
     * Test GetUnwantedImagesList when non iofog containers are running
     */
    @Test
    public void testGetUnwantedImagesListWhenNonIoFogContainersRunning() {
        String[] nonIoFogTags = {"wheelOfFortune"};
        Image nonIoFogImage = Mockito.mock(Image.class);
        images.add(nonIoFogImage);
        Mockito.when(nonIoFogImage.getRepoTags()).thenReturn(nonIoFogTags);
        List<Container> nonIoFogRunningContainer = new ArrayList<>();
        nonIoFogRunningContainer.add(container);
        Mockito.when(container.getImageId()).thenReturn("wheelOfFortune");
        Mockito.when(dockerUtil.getRunningNonIofogContainers()).thenReturn(nonIoFogRunningContainer);
        Set<String> imageIDs = pruningManager.getUnwantedImagesList();
        Mockito.verify(microserviceManager).getLatestMicroservices();
        Mockito.verify(dockerUtil).getRunningNonIofogContainers();
        Mockito.verify(dockerUtil).getImages();
        assertTrue(imageIDs.contains(unwantedImageID));
        assertFalse(imageIDs.contains("wheelOfFortune"));
        assertEquals(2, imageIDs.size());
    }

    /**
     * Test GetUnwantedImagesList when no images are found
     */
    @Test
    public void testGetUnwantedImagesListWhenNoImagesAreFound() {
        List<Image> images = new ArrayList<>();
        Mockito.when(dockerUtil.getImages()).thenReturn(images);
        List<Container> nonIoFogRunningContainer = new ArrayList<>();
        nonIoFogRunningContainer.add(container);
        Mockito.when(container.getImageId()).thenReturn(unwantedImageID);
        Mockito.when(dockerUtil.getRunningNonIofogContainers()).thenReturn(nonIoFogRunningContainer);
        Set<String> imageIDs = pruningManager.getUnwantedImagesList();
        Mockito.verify(microserviceManager).getLatestMicroservices();
        Mockito.verify(dockerUtil).getRunningNonIofogContainers();
        Mockito.verify(dockerUtil).getImages();
        assertFalse(imageIDs.contains(unwantedImageID));
        assertEquals(0, imageIDs.size());
    }

    /**
     * Test removeImagesById when no images to be pruned
     */
    @Test
    public void testRemoveImageByIDWhenNoImagesToBeRemoved() {
        try {
            Set<String> imagesList = new HashSet<>();
            method = DockerPruningManager.class.getDeclaredMethod("removeImagesById", Set.class);
            method.setAccessible(true);
            method.invoke(pruningManager, imagesList);
            Mockito.verify(dockerUtil, never()).removeImageById(anyString());
            Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logInfo("Docker Manager", "Start removing image by ID size : " + imagesList.size());
            Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logInfo("Docker Manager", "Finished removing image by ID");
        } catch (Exception e){
            fail("This should never happen");
        }
    }
    /**
     * Test removeImagesById when images to be pruned
     */
    @Test
    public void testRemoveImageByIDWhenImagesToBeRemoved() {
        try {
            String id = "image-uuid";
            Set<String> imagesList = new HashSet<>();
            imagesList.add(id);
            method = DockerPruningManager.class.getDeclaredMethod("removeImagesById", Set.class);
            method.setAccessible(true);
            method.invoke(pruningManager, imagesList);
            Mockito.verify(dockerUtil, atLeastOnce()).removeImageById(eq(id));

            Mockito.verify(LoggingService.class);
            LoggingService.logInfo("Docker Manager", "Start removing image by ID size : " + imagesList.size());
            Mockito.verify(LoggingService.class);
            LoggingService.logInfo("Docker Manager", "Removing unwanted image id : " + id);
            Mockito.verify(LoggingService.class);
            LoggingService.logInfo("Docker Manager", "Finished removing image by ID");
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    @Test
    public void testPruneAgent() {
        Mockito.when(pruneResponse.getSpaceReclaimed()).thenReturn(1223421L);
        String response = pruningManager.pruneAgent();
        Mockito.verify(dockerUtil, atLeastOnce()).dockerPrune();
        assertEquals(response, "\nSuccess - pruned dangling docker images, total reclaimed space: " + pruneResponse.getSpaceReclaimed());
    }
}
