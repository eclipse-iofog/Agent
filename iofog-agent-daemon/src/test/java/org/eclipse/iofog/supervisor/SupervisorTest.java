package org.eclipse.iofog.supervisor;

import org.eclipse.iofog.IOFogModule;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.local_api.LocalApi;
import org.eclipse.iofog.local_api.LocalApiStatus;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
import org.eclipse.iofog.process_manager.DockerUtil;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.pruning.DockerPruningManager;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.resource_manager.ResourceManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.lang.reflect.Method;

import static org.eclipse.iofog.utils.Constants.ModulesStatus.RUNNING;
import static org.eclipse.iofog.utils.Constants.ModulesStatus.STARTING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Supervisor.class, StatusReporter.class, ResourceConsumptionManager.class,
        FieldAgent.class, ProcessManager.class, SecurityManager.class,
        MessageBus.class, LocalApi.class, LoggingService.class, Configuration.class, IOFogNetworkInterfaceManager.class, DockerPruningManager.class,
        DockerUtil.class, SupervisorStatus.class})
public class SupervisorTest {
    private Supervisor supervisor;
    private Method method = null;
    private ResourceManager resourceManager;
    private IOFogNetworkInterfaceManager ioFogNetworkInterfaceManager;
    private DockerPruningManager dockerPruningManager;
    private DockerUtil dockerUtil;
    private SupervisorStatus supervisorStatus;

    @Before
    public void initialization() {
        try {
            supervisor = spy(new Supervisor());
            mockStatic(StatusReporter.class);
            mockStatic(ResourceConsumptionManager.class);
            mockStatic(FieldAgent.class);
            mockStatic(ProcessManager.class);
            mockStatic(SecurityManager.class);
            mockStatic(MessageBus.class);
            mockStatic(LocalApi.class);
            mockStatic(LoggingService.class);
            mockStatic(IOFogNetworkInterfaceManager.class);
            mockStatic(DockerUtil.class);
            mockStatic(DockerPruningManager.class);
            dockerUtil = PowerMockito.mock(DockerUtil.class);
            supervisorStatus = PowerMockito.mock(SupervisorStatus.class);
            ioFogNetworkInterfaceManager = PowerMockito.mock(IOFogNetworkInterfaceManager.class);
            dockerPruningManager = PowerMockito.mock(DockerPruningManager.class);
            PowerMockito.when(StatusReporter.setSupervisorStatus()).thenReturn(supervisorStatus);
            PowerMockito.when(supervisorStatus.setDaemonStatus(any())).thenReturn(supervisorStatus);
            PowerMockito.when(supervisorStatus.setDaemonLastStart(anyLong())).thenReturn(supervisorStatus);
            PowerMockito.when(supervisorStatus.setOperationDuration(anyLong())).thenReturn(supervisorStatus);
            PowerMockito.when(StatusReporter.setLocalApiStatus()).thenReturn(mock(LocalApiStatus.class));


            PowerMockito.when(ResourceConsumptionManager.getInstance()).thenReturn(null);
            PowerMockito.when(FieldAgent.getInstance()).thenReturn(null);
            PowerMockito.when(ProcessManager.getInstance()).thenReturn(null);
            PowerMockito.when(MessageBus.getInstance()).thenReturn(null);
            PowerMockito.when(IOFogNetworkInterfaceManager.getInstance()).thenReturn(ioFogNetworkInterfaceManager);
            PowerMockito.doNothing().when(ioFogNetworkInterfaceManager).start();
            PowerMockito.when(DockerUtil.getInstance()).thenReturn(dockerUtil);
            PowerMockito.when(DockerPruningManager.getInstance()).thenReturn(dockerPruningManager);
            PowerMockito.doNothing().when(dockerPruningManager).start();
            PowerMockito.doNothing().when(StatusReporter.class, "start");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void supervisor() {
        try {
            supervisor = spy(new Supervisor());
            suppress(method(Supervisor.class, "startModule"));
            suppress(method(Supervisor.class, "operationDuration"));
            supervisor.start();
            verify(supervisor, Mockito.atLeastOnce()).start();
            verify(supervisor, Mockito.never()).getModuleIndex();
            verify(supervisor, Mockito.atLeastOnce()).getModuleName();
            verify(supervisor, Mockito.atLeastOnce()).logDebug("Starting Supervisor");
            verify(supervisor, Mockito.atLeastOnce()).logDebug("Started Supervisor");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void verifyTest() {
        if (method != null)
            method.setAccessible(false);
    }
    /**
     * Test start module
     */
    @Test
    public void testStartModule() throws Exception{
        resourceManager = mock(ResourceManager.class);
        PowerMockito.when(resourceManager.getModuleIndex()).thenReturn(6);
        PowerMockito.when(resourceManager.getModuleName()).thenReturn("ResourceManager");
        PowerMockito.when(StatusReporter.setSupervisorStatus().setModuleStatus(6, STARTING)).thenReturn(mock(SupervisorStatus.class));
        PowerMockito.when(StatusReporter.setSupervisorStatus().setModuleStatus(6, RUNNING)).thenReturn(null);
        method = Supervisor.class.getDeclaredMethod("startModule", IOFogModule.class);
        method.setAccessible(true);
        PowerMockito.doNothing().when(resourceManager).start();
        method.invoke(supervisor, resourceManager);
        verify(supervisor, Mockito.atLeastOnce()).logInfo(" Starting ResourceManager");
        verify(supervisor, Mockito.atLeastOnce()).logInfo(" Started ResourceManager");
    }
}