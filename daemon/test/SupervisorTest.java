import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.local_api.LocalApi;
import org.eclipse.iofog.local_api.LocalApiStatus;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.security_manager.SecurityManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.supervisor.Supervisor;
import org.eclipse.iofog.supervisor.SupervisorStatus;
import org.eclipse.iofog.tracking.Tracker;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Supervisor.class, StatusReporter.class, ResourceConsumptionManager.class,
        FieldAgent.class, ProcessManager.class, Tracker.class, SecurityManager.class,
        MessageBus.class, LocalApi.class, LoggingService.class})
public class SupervisorTest {
    private Supervisor supervisor;

    @Before
    public void initialization() {
        try {
            mockStatic(StatusReporter.class);
            mockStatic(ResourceConsumptionManager.class);
            mockStatic(FieldAgent.class);
            mockStatic(ProcessManager.class);
            mockStatic(Tracker.class);
            mockStatic(SecurityManager.class);
            mockStatic(MessageBus.class);
            mockStatic(LocalApi.class);
            mockStatic(LoggingService.class);

            PowerMockito.when(StatusReporter.setSupervisorStatus()).thenReturn(new SupervisorStatus());
            PowerMockito.when(StatusReporter.setLocalApiStatus()).thenReturn(new LocalApiStatus());
            PowerMockito.when(ResourceConsumptionManager.getInstance()).thenReturn(null);
            PowerMockito.when(FieldAgent.getInstance()).thenReturn(null);
            PowerMockito.when(ProcessManager.getInstance()).thenReturn(null);
            PowerMockito.when(Tracker.getInstance()).thenReturn(new Tracker());
            PowerMockito.when(SecurityManager.getInstance()).thenReturn(null);
            PowerMockito.when(MessageBus.getInstance()).thenReturn(null);
            PowerMockito.when(LocalApi.getInstance()).thenReturn(null);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void verifyTest() {
        try {
            verify(supervisor, Mockito.atLeastOnce()).start();
            verify(supervisor, Mockito.never()).getModuleIndex();
            verify(supervisor, Mockito.atLeastOnce()).getModuleName();

            verify(supervisor, Mockito.atLeastOnce()).logInfo("starting status reporter");
            verify(supervisor, Mockito.atLeastOnce()).logInfo("Started");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}