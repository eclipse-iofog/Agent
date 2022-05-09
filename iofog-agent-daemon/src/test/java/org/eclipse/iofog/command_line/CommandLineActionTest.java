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

package org.eclipse.iofog.command_line;

import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.CmdProperties;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.Json;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandLineAction.class, StatusReporter.class, FieldAgent.class, Configuration.class, Orchestrator.class, CmdProperties.class, LoggingService.class})
public class CommandLineActionTest {
    private CommandLineAction commandLineAction;
    private StatusReporter statusReporter;
    private FieldAgent fieldAgent;
    private List stop;
    private HashMap<String, String> result;
    private CmdProperties cmdProperties;

    @Before
    public void setUp() throws Exception {
        commandLineAction = mock(CommandLineAction.class);
        statusReporter = mock(StatusReporter.class);
        fieldAgent = mock(FieldAgent.class);
        cmdProperties = mock(CmdProperties.class);
        mockStatic(LoggingService.class);
        stop = new ArrayList(Collections.singleton("stop"));
        result = new HashMap<>();
        result.put("ll", "info");
        mockStatic(FieldAgent.class);
        mockStatic(Orchestrator.class);
        mockStatic(Configuration.class);
        mockStatic(StatusReporter.class);
        mockStatic(CmdProperties.class);
        PowerMockito.when(FieldAgent.getInstance()).thenReturn(fieldAgent);
        PowerMockito.when(fieldAgent.provision("dummy")).thenReturn(Json.createObjectBuilder().add("status", "success").add("errorMessage", "").add("uuid", "uuid").build());
        PowerMockito.when(fieldAgent.provision("anotherkey")).thenReturn(Json.createObjectBuilder().add("status", "success").add("errorMessage", "Key not valid").build());
        PowerMockito.when(fieldAgent.provision("prod")).thenReturn(null);
        PowerMockito.when(statusReporter.getStatusReport()).thenReturn(status);
        // CommandProperties mock
        PowerMockito.when(CmdProperties.getVersion()).thenReturn("1.2.2");
        PowerMockito.when(CmdProperties.getVersionMessage()).thenReturn(version);
        PowerMockito.when(CmdProperties.getDeprovisionMessage()).thenReturn("Deprovisioning from controller ... %s");
        PowerMockito.when(CmdProperties.getProvisionMessage()).thenReturn("Provisioning with key \"%s\" ... Result: %s");
        PowerMockito.when(CmdProperties.getProvisionCommonErrorMessage()).thenReturn("\nProvisioning failed");
        PowerMockito.when(CmdProperties.getProvisionStatusErrorMessage()).thenReturn("\nProvision failed with error message: \"%s\"");
        PowerMockito.when(CmdProperties.getProvisionStatusSuccessMessage()).thenReturn("\nProvision success - Iofog UUID is %s");
    }

    @After
    public void tearDown() throws Exception {
        stop = null;
        result = null;
        reset(statusReporter);
        reset(fieldAgent);
    }

    /**
     * Get Keys method
     */
    @Test
    public void testGetKeys() {
        assertFalse(CommandLineAction.HELP_ACTION.getKeys().isEmpty());
        assertTrue(isEqual(stop, CommandLineAction.getActionByKey("stop").getKeys()));
    }

    /**
     * help command with success
     */
    @Test
    public void testHelpActionPerform() {
        String[] helpArgs = {"help", "--help", "-h", "-?"};
        try {
            assertEquals(helpContent, CommandLineAction.getActionByKey(helpArgs[0]).perform(helpArgs));
            assertEquals(helpContent, CommandLineAction.getActionByKey(helpArgs[1]).perform(helpArgs));
            assertEquals(helpContent, CommandLineAction.getActionByKey(helpArgs[2]).perform(helpArgs));
            assertEquals(helpContent, CommandLineAction.getActionByKey(helpArgs[3]).perform(helpArgs));
        } catch (AgentUserException e) {
            fail("Shall never happen");
        }
    }

    /**
     * Version command with success
     */
    @Test
    public void testVersionActionPerform() {
        String[] args = {"version", "--version", "-v"};
        try {
            assertEquals(version, CommandLineAction.getActionByKey(args[0]).perform(args));
        } catch (AgentUserException e) {
            fail("Shall never happen");
        }
    }

    /**
     * status command with success
     */
    @Test
    public void testStatusActionPerform() {
        String[] args = {"status"};
        try {
            assertTrue(! CommandLineAction.getActionByKey(args[0]).perform(args).isEmpty());
            assertEquals(status, CommandLineAction.getActionByKey(args[0]).perform(args));
        } catch (AgentUserException e) {
            fail("Shall never happen");
        }
    }

    /**
     * deprovision command with success
     */
    @Test
    public void testDeProvisionActionPerform() {
        String[] args = {"deprovision"};
        assertTrue(! CommandLineAction.getActionByKey(args[0]).getKeys().isEmpty());
        PowerMockito.when(fieldAgent.deProvision(anyBoolean())).thenReturn("\nSuccess - tokens, identifiers and keys removed");
        try {
            assertEquals("Deprovisioning from controller ... \nSuccess - tokens, identifiers and keys removed", commandLineAction.getActionByKey(args[0]).perform(args));
        } catch (AgentUserException e) {
            fail("This shall never happen");
        }
    }

    /**
     * deprovision command with failure
     * throws AgentSystemException
     */
    @Test(expected = AgentUserException.class)
    public void throwsAgentUserExcpetionWhenDeProvisionActionPerform() throws AgentUserException {
        String[] args = {"deprovision"};
        assertTrue(! CommandLineAction.getActionByKey(args[0]).getKeys().isEmpty());
        PowerMockito.when(fieldAgent.deProvision(anyBoolean())).thenReturn("\nFailure - not provisioned");
        commandLineAction.getActionByKey(args[0]).perform(args);
    }


    /**
     * Info command displaying the config report
     */
    @Test
    public void testInfoActionPerform() {
        String[] args = {"info"};
        assertTrue(! CommandLineAction.getActionByKey(args[0]).getKeys().isEmpty());
        when(Configuration.getConfigReport()).thenReturn("Config report");
        try {
            assertEquals("Config report", commandLineAction.getActionByKey(args[0]).perform(args));
        } catch (AgentUserException e) {
            fail("This shall never happen");
        }
    }

    /**
     * Switch command with no option displays help content
     */
    @Test
    public void testSwitchActionPerformWithNoValue() {
        String[] args = {"switch"};
        assertTrue(! CommandLineAction.getActionByKey(args[0]).getKeys().isEmpty());
        try {
            assertEquals(helpContent, CommandLineAction.getActionByKey(args[0]).perform(args));
        } catch (AgentUserException e) {
            fail("This shall never happen");
        }
    }

    /**
     * Switch command with valid option
     */
    @Test
    public void testSwitchActionPerformWithValidValue() {
        String[] anotherArgs = {"switch", "prod"};
        when(Configuration.setupConfigSwitcher(any())).thenReturn("success");
        try {
            assertEquals("success", commandLineAction.getActionByKey(anotherArgs[0]).perform(anotherArgs));
        } catch (AgentUserException e) {
            fail("This shall never happen");
        }
    }

    /**
     * Switch option with Invalid value
     * nn
     */
    @Test
    public void testSwitchActionPerformWithInvalidValue() {
        String[] anotherArgs = {"switch", "dummy"};
        try {
            assertEquals("Invalid switcher state", commandLineAction.getActionByKey(anotherArgs[0]).perform(anotherArgs));
        } catch (AgentUserException e) {
            fail("This shall never happen");
        }
    }

    /**
     * When no value is passed with provision option
     */
    @Test
    public void testProvisionActionPerformWithNoValue() {
        String[] args = {"provision"};
        assertTrue(! CommandLineAction.getActionByKey(args[0]).getKeys().isEmpty());
        try {
            assertEquals(helpContent, CommandLineAction.getActionByKey(args[0]).perform(args));
        } catch (AgentUserException e) {
            fail("This shall never happen");
        }
    }

    /**
     * When provisioningResult of FieldAgent returns null response.
     */
    @Test(expected = AgentUserException.class)
    public void testProvisionActionPerformWithTwoArgs() throws AgentUserException {
        String[] anotherArgs = {"provision", "prod"};
        commandLineAction.getActionByKey(anotherArgs[0]).perform(anotherArgs);
    }

    /**
     * When FieldAgent.provision(provisionKey) returns the mock response with uuid
     */
    @Test
    public void testProvisionActionPerformReturnsUUID() {
        String[] anotherArgs1 = {"provision", "dummy"};
        try {
            assertEquals("Provisioning with key \"dummy\" ... Result: \n" + "Provision success - Iofog UUID is uuid", commandLineAction.getActionByKey(anotherArgs1[0]).perform(anotherArgs1));
        } catch (AgentUserException e) {
            fail("This shall never happen");
        }
    }

    /**
     * When FieldAgent.provision(provisionKey) returns the mock response without uuid
     * throws AgentUserException
     */
    @Test(expected = AgentUserException.class)
    public void throwsAgentUserExceptionProvisionActionPerformResponseWithoutUUID() throws AgentUserException {
        String[] anotherArgs2 = {"provision", "anotherkey"};
        commandLineAction.getActionByKey(anotherArgs2[0]).perform(anotherArgs2);

    }

    /**
     * When config command with invalid options display help
     */
    @Test
    public void testConfigActionPerformWithOutOption() throws Exception {
        String[] args = {"config"};
        assertTrue(! CommandLineAction.getActionByKey(args[0]).getKeys().isEmpty());
        assertEquals(helpContent, CommandLineAction.getActionByKey(args[0]).perform(args));

    }

    /**
     * When config command without options display help
     * Test when config option value is ambiguous
     */
    @Test
    public void tesConfigActionPerformWithInvalidOption() throws Exception {
        String[] anotherArgs = {"config", "ambiguous"};
        assertEquals(helpContent, CommandLineAction.getActionByKey(anotherArgs[0]).perform(anotherArgs));
    }

    /**
     * When config command with default to reset the configuration Success
     * Test when config reset to defaults
     */
    @Test
    public void testConfigActionPerformWithDefaultOption() throws Exception {
        String[] anotherArgs1 = {"config", "defaults"};
        assertEquals("Configuration has been reset to its defaults.", CommandLineAction.getActionByKey(anotherArgs1[0]).perform(anotherArgs1));

    }

    /**
     * When config command with valid option with no value
     */
    @Test
    public void testConfigActionPerformWithValidOptionAndNoValue() throws Exception {
        String[] logArgs = {"config", "-ll"};
        assertEquals(helpContent, CommandLineAction.getActionByKey(logArgs[0]).perform(logArgs));
    }

    /**
     * When config command with valid option with InValid value then Configuration throw exception
     */
    @Test
    public void throwsExceptionsWhenConfigActionPerformWithValidOptionAndInvalidValue() throws Exception {
        String[] logArgs = {"config", "-ll", "severeAndInfo"};
        PowerMockito.when(Configuration.getOldNodeValuesForParameters(anySet(), any())).
                thenReturn(result);
        PowerMockito.when(Configuration.setConfig(anyMap(), anyBoolean())).
                thenThrow(new Exception("item not found or defined more than once"));
        assertEquals("Error updating new config : item not found or defined more than once", CommandLineAction.getActionByKey(logArgs[0]).perform(logArgs));
    }


    /**
     * When config command with valid option and value
     */
    @Test
    public void testConfigActionPerformWithValidOptionAndValue() throws Exception {

        String[] logArgs = {"config", "-ll", "severe"};
        PowerMockito.when(Configuration.setConfig(anyMap(), anyBoolean())).thenReturn(new HashMap<>());
        PowerMockito.when(Configuration.getOldNodeValuesForParameters(anySet(), any())).
                thenReturn(result);
        assertEquals("\\n\tChange accepted for Parameter : - ll, Old value was :info, New Value is : severe", CommandLineAction.getActionByKey(logArgs[0]).perform(logArgs));

    }

    /**
     * Helper method for comparing lists
     */
    private static boolean isEqual(List list1, List list2) {
        boolean value = list1.size() == list2.size() && list1.equals(list2);
        return value;
    }

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

    private String status = "ioFog daemon                : " +
            "STARTING\\nMemory Usage                :" +
            " about 0.00 MiB\\nDisk Usage                  : " +
            "about 0.00 MiB\\nCPU Usage                   : " +
            "about 0.00 %\\nRunning Microservices       : " +
            "0\\nConnection to Controller    : " +
            "not connected\\nMessages Processed          " +
            ": about 0\\nSystem Time                 : " +
            "18/09/2019 05:58 PM\\nSystem Available Disk       : " +
            "0.00 MB\\nSystem Available Memory     : " +
            "0.00 MB\\nSystem Total CPU            : 0.00 %";

    private String version = "ioFog 1 \n" +
            "Copyright (C) 2018-2022 Edgeworx, Inc. \n" +
            "Eclipse ioFog is provided under the Eclipse Public License 2.0 (EPL-2.0) \n" +
            "https://www.eclipse.org/legal/epl-v20.html";

    private String helpContent = "Usage 1: iofog-agent [OPTION]\\n" +
            "Usage 2: iofog-agent [COMMAND] <Argument>\\n" +
            "Usage 3: iofog-agent [COMMAND] [Parameter] <Value>\\n" +
            "\\n" +
            "Option           GNU long option         Meaning\\n" +
            "======           ===============         =======\\n" +
            "-h, -?           --help                  Show this message\\n" +
            "-v               --version               Display the software version and\\n" +
            "                                         license information\\n" +
            "\\n" +
            "\\n" +
            "Command          Arguments               Meaning\\n" +
            "=======          =========               =======\\n" +
            "help                                     Show this message\\n" +
            "version                                  Display the software version and\\n" +
            "                                         license information\\n" +
            "status                                   Display current status information\\n" +
            "                                         about the software\\n" +
            "provision        <provisioning key>      Attach this software to the\\n" +
            "                                         configured ioFog controller\\n" +
            "deprovision                              Detach this software from all\\n" +
            "                                         ioFog controllers\\n" +
            "info                                     Display the current configuration\\n" +
            "                                         and other information about the\\n" +
            "                                         software\\n" +
            "switch           <dev|prod|def>          Switch to different config \\n" +
            "config           [Parameter] [VALUE]     Change the software configuration\\n" +
            "                                         according to the options provided\\n" +
            "                 defaults                Reset configuration to default values\\n" +
            "                 -d <#GB Limit>          Set the limit, in GiB, of disk space\\n" +
            "                                         that the message archive is allowed to use\\n" +
            "                 -dl <dir>               Set the message archive directory to use for disk\\n" +
            "                                         storage\\n" +
            "                 -m <#MB Limit>          Set the limit, in MiB, of RAM memory that\\n" +
            "                                         the software is allowed to use for\\n" +
            "                                         messages\\n" +
            "                 -p <#cpu % Limit>       Set the limit, in percentage, of CPU\\n" +
            "                                         time that the software is allowed\\n" +
            "                                         to use\\n" +
            "                 -a <uri>                Set the uri of the fog controller\\n" +
            "                                         to which this software connects\\n" +
            "                 -ac <filepath>          Set the file path of the SSL/TLS\\n" +
            "                                         certificate for validating the fog\\n" +
            "                                         controller identity\\n" +
            "                 -c <uri>                Set the UNIX socket or network address\\n" +
            "                                         that the Docker daemon is using\\n" +
            "                 -n <network adapter>    Set the name of the network adapter\\n" +
            "                                         that holds the correct IP address of \\n" +
            "                                         this machine\\n" +
            "                 -l <#GB Limit>          Set the limit, in GiB, of disk space\\n" +
            "                                         that the log files can consume\\n" +
            "                 -ld <dir>               Set the directory to use for log file\\n" +
            "                                         storage\\n" +
            "                 -lc <#log files>        Set the number of log files to evenly\\n" +
            "                                         split the log storage limit\\n" +
            "                 -ll <log level>         Set the standard logging levels that\\n"+
            "                                         can be used to control logging output\\n" +
            "                 -sf <#seconds>          Set the status update frequency\\n" +
            "                 -cf <#seconds>          Set the get changes frequency\\n" +
            "                 -df <#seconds>          Set the post diagnostics frequency\\n" +
            "                 -sd <#seconds>          Set the scan devices frequency\\n" +
            "                 -uf <#hours>            Set the isReadyToUpgradeScan frequency\\n" +
            "                 -dt <#percentage>       Set the available disk threshold\\n" +
            "                 -idc <on/off>           Set the mode on which any not\\n" +
            "                                         registered docker container will be\\n" +
            "										  shut down\\n" +
            "                 -gps <auto/off          Set gps location of fog.\\n" +
            "                      /#GPS DD.DDD(lat), Use auto to get coordinates by IP,\\n" +
            "                            DD.DDD(lon)  use off to forbid gps,\\n" +
            "                                         use GPS coordinates in DD format to set them manually\\n" +
            "                 -ft <auto               Set fog type.\\n" +
            "                     /intel_amd/arm>     Use auto to detect fog type by system commands,\\n" +
            "                                         use arm or intel_amd to set it manually\\n" +
            "                 -sec <on/off>           Set the secure mode without using ssl \\n" +
            "                                         certificates. \\n" +
            "                 -dev <on/off>           Set the developer's mode\\n" +
            "                 -tz                     Set the device timeZone\\n" +
            "\\n" +
            "\\n" +
            "Report bugs to: edgemaster@iofog.org\\n" +
            "ioFog home page: http://iofog.org\\n" +
            "For users with Eclipse accounts, report bugs to: https://bugs.eclipse.org/bugs/enter_bug.cgi?product=iofog";
}
