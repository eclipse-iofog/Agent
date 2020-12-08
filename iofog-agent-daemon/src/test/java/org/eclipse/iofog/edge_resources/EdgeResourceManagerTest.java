package org.eclipse.iofog.edge_resources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EdgeResourceManager.class})
public class EdgeResourceManagerTest {
    private EdgeResourceManager edgeResourceManager;

    @Before
    public void setUp() throws Exception {
        edgeResourceManager = Mockito.spy(EdgeResourceManager.class);
        setMock(edgeResourceManager);
    }
    /**
     * Set a mock to the {@link EdgeResourceManager} instance
     * Throws {@link RuntimeException} in case if reflection failed, see a {@link Field#set(Object, Object)} method description.
     * @param mock the mock to be inserted to a class
     */
    private void setMock(EdgeResourceManager mock) {
        try {
            Field instance = EdgeResourceManager.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void tearDown() throws Exception {
        Field instance = EdgeResourceManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    /**
     * Asserts mock is same as the StraceDiagnosticManager.getInstance()
     */
    @Test
    public void testGetInstanceIsSameAsMock() {
        assertEquals(edgeResourceManager, EdgeResourceManager.getInstance());
    }

    @Test
    public void testGetAndSetLatestEdgeResources() {
        assertEquals(0, edgeResourceManager.getLatestEdgeResources().size());
        EdgeResource edgeResource = mock(EdgeResource.class);
        List<EdgeResource> edgeResourceList = new ArrayList<>();
        edgeResourceList.add(edgeResource);
        edgeResourceManager.setLatestEdgeResources(edgeResourceList);
        assertEquals(edgeResourceManager.getLatestEdgeResources().get(0), edgeResource);
        assertEquals(edgeResourceManager.getLatestEdgeResources().size(), edgeResourceList.size());
    }

    @Test
    public void testGetAndCurrentEdgeResources() {
        assertEquals(0, edgeResourceManager.getCurrentEdgeResources().size());
        EdgeResource edgeResource = mock(EdgeResource.class);
        List<EdgeResource> edgeResourceList = new ArrayList<>();
        edgeResourceList.add(edgeResource);
        edgeResourceManager.setCurrentEdgeResources(edgeResourceList);
        assertEquals(edgeResourceManager.getCurrentEdgeResources().size(), edgeResourceList.size());
        assertEquals(edgeResourceManager.getCurrentEdgeResources().get(0), edgeResource);
    }

    @Test
    public void testClear() {
        EdgeResource edgeResource = mock(EdgeResource.class);
        List<EdgeResource> edgeResourceList = new ArrayList<>();
        edgeResourceList.add(edgeResource);
        edgeResourceManager.setCurrentEdgeResources(edgeResourceList);
        edgeResourceManager.setLatestEdgeResources(edgeResourceList);
        assertEquals(edgeResourceManager.getCurrentEdgeResources().size(), edgeResourceList.size());
        assertEquals(edgeResourceManager.getLatestEdgeResources().size(), edgeResourceList.size());
        edgeResourceManager.clear();
        assertEquals(0, edgeResourceManager.getCurrentEdgeResources().size());
        assertEquals(0, edgeResourceManager.getLatestEdgeResources().size());


    }
    @Test(expected = UnsupportedOperationException.class)
    public void testIfListIsImmutable(){
        edgeResourceManager.getCurrentEdgeResources().add(null);
        edgeResourceManager.getLatestEdgeResources().add(null);
    }

}