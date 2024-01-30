package org.eclipse.iofog.supervisor;

import org.eclipse.iofog.IOFogModule;

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
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SupervisorTest {
    private Supervisor supervisor;
    private SupervisorStatus supervisorStatus;
    private MockedStatic<StatusReporter> statusReporterMockedStatic;
    private MockedStatic<ResourceConsumptionManager> resourceConsumptionManager;
    private MockedStatic<FieldAgent> fieldAgentMockedStatic;
    private MockedStatic<ProcessManager> processManagerMockedStatic;
    private MockedStatic<MessageBus> messageBusMockedStatic;
    private MockedStatic<LocalApi> localApiMockedStatic;
    private MockedStatic<LoggingService> loggingService;
    private MockedStatic<IOFogNetworkInterfaceManager> iOFogNetworkInterfaceManagerMockedStatic;
    private MockedStatic<DockerUtil> dockerUtilMockedStatic;
    private MockedStatic<DockerPruningManager> dockerPruningManagerMockedStatic;
    private MockedStatic<Runtime> runtimeMockedStatic;
    private MockedStatic<Executors> executorsMockedStatic;
    private MockedConstruction<ResourceManager> resourceManagerMockedConstruction;
    private MockedConstruction<Thread> threadMockedConstruction;
    private ScheduledExecutorService scheduledExecutorService;
    private Runtime runtime;
    @BeforeEach
    public void initialization() {
        try {
            runtimeMockedStatic = mockStatic(Runtime.class);
            executorsMockedStatic = mockStatic(Executors.class);
            statusReporterMockedStatic = mockStatic(StatusReporter.class);
            resourceConsumptionManager = mockStatic(ResourceConsumptionManager.class);
            fieldAgentMockedStatic = mockStatic(FieldAgent.class);
            processManagerMockedStatic = mockStatic(ProcessManager.class);
            messageBusMockedStatic = mockStatic(MessageBus.class);
            localApiMockedStatic = mockStatic(LocalApi.class);
            loggingService = mockStatic(LoggingService.class);
            iOFogNetworkInterfaceManagerMockedStatic = mockStatic(IOFogNetworkInterfaceManager.class);
            dockerUtilMockedStatic = mockStatic(DockerUtil.class);
            dockerPruningManagerMockedStatic = mockStatic(DockerPruningManager.class);
            DockerUtil dockerUtil = Mockito.mock(DockerUtil.class);
            supervisorStatus = Mockito.mock(SupervisorStatus.class);
            IOFogNetworkInterfaceManager ioFogNetworkInterfaceManager = Mockito.mock(IOFogNetworkInterfaceManager.class);
            DockerPruningManager dockerPruningManager = Mockito.mock(DockerPruningManager.class);
            ResourceConsumptionManager resourceConsumptionManager = mock(ResourceConsumptionManager.class);
            FieldAgent fieldAgent = mock(FieldAgent.class);
            scheduledExecutorService = mock(ScheduledExecutorService.class);
            runtime = mock(Runtime.class);
            MessageBus messageBus = mock(MessageBus.class);
            LocalApi localApi = mock(LocalApi.class);
            ProcessManager processManager = mock(ProcessManager.class);
            Mockito.when(Executors.newScheduledThreadPool(anyInt())).thenReturn(scheduledExecutorService);
            Mockito.when(scheduledExecutorService
                    .scheduleAtFixedRate(any(),anyLong(),anyLong(),any())).thenReturn(null);
            Mockito.when(Runtime.getRuntime()).thenReturn(runtime);
            Mockito.doNothing().doNothing().when(runtime).addShutdownHook(any());
            statusReporterMockedStatic.when(StatusReporter::start)
                    .thenAnswer((Answer<Void>) invocation -> null);
            Mockito.when(ResourceConsumptionManager.getInstance()).thenReturn(resourceConsumptionManager);
            Mockito.doNothing().when(resourceConsumptionManager).start();
            Mockito.when(FieldAgent.getInstance()).thenReturn(fieldAgent);
            Mockito.doNothing().when(fieldAgent).start();

            Mockito.when(ProcessManager.getInstance()).thenReturn(processManager);
            Mockito.doNothing().when(processManager).start();
            
            Mockito.when(MessageBus.getInstance()).thenReturn(messageBus);
            Mockito.doNothing().when(messageBus).start();

            Mockito.when(IOFogNetworkInterfaceManager.getInstance()).thenReturn(ioFogNetworkInterfaceManager);
            Mockito.doNothing().when(ioFogNetworkInterfaceManager).start();
            Mockito.when(DockerUtil.getInstance()).thenReturn(dockerUtil);

            Mockito.when(DockerPruningManager.getInstance()).thenReturn(dockerPruningManager);
            Mockito.doNothing().when(dockerPruningManager).start();
            Mockito.when(LocalApi.getInstance()).thenReturn(localApi);
            threadMockedConstruction = Mockito.mockConstruction(Thread.class, (mock, context) -> {
                Mockito.doNothing().when(mock).start();
            });

            resourceManagerMockedConstruction = Mockito.mockConstruction(ResourceManager.class,
                    (mock, context) -> {
                        Mockito.doNothing().when(mock).start();
                    });
            Mockito.when(StatusReporter.setSupervisorStatus()).thenReturn(supervisorStatus).thenReturn(supervisorStatus).thenReturn(supervisorStatus);
            statusReporterMockedStatic.when(StatusReporter::setSupervisorStatus).thenReturn(supervisorStatus);
            Mockito.when(supervisorStatus.setDaemonStatus(any())).thenReturn(supervisorStatus);
            Mockito.when(supervisorStatus.setDaemonLastStart(anyLong())).thenReturn(supervisorStatus);
            Mockito.when(supervisorStatus.setOperationDuration(anyLong())).thenReturn(supervisorStatus);
            Mockito.when(supervisorStatus.setModuleStatus(anyInt(), any())).thenReturn(mock(SupervisorStatus.class));

            Mockito.when(StatusReporter.setLocalApiStatus()).thenReturn(mock(LocalApiStatus.class));
            supervisor = spy(new Supervisor());
            Mockito.doNothing().when(supervisor).operationDuration();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @AfterEach
    public void verifyTest() {
        executorsMockedStatic.close();
        runtimeMockedStatic.close();
        fieldAgentMockedStatic.close();
        statusReporterMockedStatic.close();
        processManagerMockedStatic.close();
        messageBusMockedStatic.close();
        localApiMockedStatic.close();
        loggingService.close();
        iOFogNetworkInterfaceManagerMockedStatic.close();
        dockerUtilMockedStatic.close();
        dockerPruningManagerMockedStatic.close();
        resourceConsumptionManager.close();
        resourceManagerMockedConstruction.close();
        threadMockedConstruction.close();
        reset(scheduledExecutorService);
        reset(supervisorStatus);
        reset(runtime);
    }
    @Test
    public void supervisor() {
        try {
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

    /**
     * Test start module
     */
    @Test
    public void testStartModule() throws Exception{
        ResourceManager resourceManager = mock(ResourceManager.class);
        Mockito.when(resourceManager.getModuleIndex()).thenReturn(6);
        Mockito.when(resourceManager.getModuleName()).thenReturn("ResourceManager");
        Method method = Supervisor.class.getDeclaredMethod("startModule", IOFogModule.class);
        method.setAccessible(true);
        Mockito.doNothing().when(resourceManager).start();
        method.invoke(supervisor, resourceManager);
        verify(supervisor, Mockito.atLeastOnce()).logInfo(" Starting ResourceManager");
        verify(supervisor, Mockito.atLeastOnce()).logInfo(" Started ResourceManager");
    }
}