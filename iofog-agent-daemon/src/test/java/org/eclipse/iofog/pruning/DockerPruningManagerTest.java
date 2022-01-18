/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2022 Edgeworx, Inc.
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DockerPruningManager.class, DockerUtil.class, MicroserviceManager.class, Image.class, Container.class, LoggingService.class,
        ScheduledExecutorService.class})
public class DockerPruningManagerTest {
    private DockerPruningManager pruningManager;
    private DockerUtil dockerUtil;
    private MicroserviceManager microserviceManager;
    private Container container = null;
    private String unwantedImageID = null;
    private List<Image> images = null;
    private Method method = null;
    private PruneResponse pruneResponse = null;


    @Before
    public void setUp() throws Exception {
        setMock(pruningManager);
        dockerUtil = Mockito.mock(DockerUtil.class);
        mockStatic(MicroserviceManager.class);
        mockStatic(DockerUtil.class);
        mockStatic(LoggingService.class);
        microserviceManager = PowerMockito.mock(MicroserviceManager.class);
        PowerMockito.when(DockerUtil.getInstance()).thenReturn(dockerUtil);
        PowerMockito.when(MicroserviceManager.getInstance()).thenReturn(microserviceManager);
        images = new ArrayList<>();
        Image unwantedImage = Mockito.mock(Image.class);
        Image wantedImage = Mockito.mock(Image.class);
        String[] unwantedTags = {"none:<none>"};
        String[] wantedTags = {"edgeworx/calibration-sensors-arm"};
        PowerMockito.when(unwantedImage.getRepoTags()).thenReturn(unwantedTags);
        unwantedImageID = "unwantedImage";
        PowerMockito.when(unwantedImage.getId()).thenReturn(unwantedImageID);
        PowerMockito.when(wantedImage.getRepoTags()).thenReturn(wantedTags);
        images.add(unwantedImage);
        images.add(wantedImage);
        PowerMockito.when(dockerUtil.getImages()).thenReturn(images);
        List<Microservice> latestMicroservices = new ArrayList<>();
        Microservice microservice = new Microservice("uuid", "edgeworx/calibration-sensors-arm");
        latestMicroservices.add(microservice);
        PowerMockito.when(microserviceManager.getLatestMicroservices()).thenReturn(latestMicroservices);
        pruneResponse = mock(PruneResponse.class);
        PowerMockito.when(dockerUtil.dockerPrune()).thenReturn(pruneResponse);
        PowerMockito.doNothing().when(dockerUtil).removeImageById(anyString());
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        pruningManager = PowerMockito.spy(DockerPruningManager.getInstance());
        container = Mockito.mock(Container.class);


    }

    @After
    public void tearDown() throws Exception {
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
        assertTrue(imageIDs.size() == 1);
    }

    /**
     * Test GetUnwantedImagesList when non iofog containers are running
     */
    @Test
    public void testGetUnwantedImagesListWhenNonIoFogContainersRunning() {
        String[] nonIoFogTags = {"wheelOfFortune"};
        Image nonIoFogImage = Mockito.mock(Image.class);
        images.add(nonIoFogImage);
        PowerMockito.when(nonIoFogImage.getRepoTags()).thenReturn(nonIoFogTags);
        List<Container> nonIoFogRunningContainer = new ArrayList<>();
        nonIoFogRunningContainer.add(container);
        PowerMockito.when(container.getImageId()).thenReturn("wheelOfFortune");
        PowerMockito.when(dockerUtil.getRunningNonIofogContainers()).thenReturn(nonIoFogRunningContainer);
        Set<String> imageIDs = pruningManager.getUnwantedImagesList();
        Mockito.verify(microserviceManager).getLatestMicroservices();
        Mockito.verify(dockerUtil).getRunningNonIofogContainers();
        Mockito.verify(dockerUtil).getImages();
        assertTrue(imageIDs.contains(unwantedImageID));
        assertTrue(!imageIDs.contains("wheelOfFortune"));
        assertTrue(imageIDs.size() == 2);
    }

    /**
     * Test GetUnwantedImagesList when no images are found
     */
    @Test
    public void testGetUnwantedImagesListWhenNoImagesAreFound() {
        List<Image> images = new ArrayList<>();
        PowerMockito.when(dockerUtil.getImages()).thenReturn(images);
        List<Container> nonIoFogRunningContainer = new ArrayList<>();
        nonIoFogRunningContainer.add(container);
        PowerMockito.when(container.getImageId()).thenReturn(unwantedImageID);
        PowerMockito.when(dockerUtil.getRunningNonIofogContainers()).thenReturn(nonIoFogRunningContainer);
        Set<String> imageIDs = pruningManager.getUnwantedImagesList();
        Mockito.verify(microserviceManager).getLatestMicroservices();
        Mockito.verify(dockerUtil).getRunningNonIofogContainers();
        Mockito.verify(dockerUtil).getImages();
        assertTrue(!imageIDs.contains(unwantedImageID));
        assertTrue(imageIDs.size() == 0);
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
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logInfo("Docker Manager", "Start removing image by ID size : " + imagesList.size());
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
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

            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logInfo("Docker Manager", "Start removing image by ID size : " + imagesList.size());
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logInfo("Docker Manager", "Removing unwanted image id : " + id);
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logInfo("Docker Manager", "Finished removing image by ID");
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    @Test
    public void testPruneAgent() {
        PowerMockito.when(pruneResponse.getSpaceReclaimed()).thenReturn(1223421l);
        String response = pruningManager.pruneAgent();
        Mockito.verify(dockerUtil, atLeastOnce()).dockerPrune();
        assertTrue(response.equals("\nSuccess - pruned dangling docker images, total reclaimed space: " + pruneResponse.getSpaceReclaimed()));
    }
}
