package org.eclipse.iofog.edge_resources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EdgeResourceManagerTest {
    private EdgeResourceManager edgeResourceManager;

    @BeforeEach
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

    @AfterEach
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

}