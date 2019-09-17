/*******************************************************************************
 * Copyright (c) 2019 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 * Neha Naithani
 *******************************************************************************/

package org.eclipse.iofog.command_line;

import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.Json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandLineAction.class, StatusReporter.class, FieldAgent.class, Configuration.class,
        Orchestrator.class})
public class CommandLineActionTest {
    private CommandLineAction commandLineAction;
    private StatusReporter statusReporter;
    private FieldAgent fieldAgent;
    private List stop;
    private HashMap<String, String> result;

    @Before
    public void setUp() throws Exception {
        commandLineAction = mock(CommandLineAction.class);
        statusReporter = mock(StatusReporter.class);
        fieldAgent = mock(FieldAgent.class);
        mockStatic(FieldAgent.class);
        mockStatic(Orchestrator.class);
        mockStatic(Configuration.class);

        PowerMockito.when(FieldAgent.getInstance()).thenReturn(fieldAgent);


        PowerMockito.when(fieldAgent.provision("dummy")).thenReturn(Json.createObjectBuilder()
                .add("status", "success")
                .add("errorMessage", "")
                .add("uuid", "uuid")
                .build());
        PowerMockito.when(fieldAgent.provision("anotherkey")).thenReturn(Json.createObjectBuilder()
                .add("status", "success")
                .add("errorMessage", "Key not valid")
                .build());
        PowerMockito.when(fieldAgent.provision("prod")).thenReturn(null);

        // PowerMockito.when(Configuration.setConfig(anyMap(), anyBoolean())).thenReturn(null);
        stop = new ArrayList(Collections.singleton("stop"));

        result = new HashMap<>();
        result.put("ll", "info");
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
    public void getKeys() {

        assertFalse(CommandLineAction.HELP_ACTION.getKeys().isEmpty());
        assertTrue(isEqual(stop, CommandLineAction.getActionByKey("stop").getKeys()));
    }

    /**
     * help command with success
     */
    @Test
    public void helpActionPerform() {
        String[] helpArgs = {"help", "-h", "-?"};
        assertEquals(helpContent, CommandLineAction.getActionByKey(helpArgs[0]).perform(helpArgs));
    }

    /**
     * status command with success
     */
    @Test
    public void versionActionPerform() {
        String[] args = {"version", "--version", "-v"};
        assertEquals(version, CommandLineAction.getActionByKey(args[0]).perform(args));
    }

    /**
     * status command with success
     */
    @Test
    public void statusActionPerform() {
        String[] args = {"status"};
        statusReporter.setSupervisorStatus().setDaemonStatus(Constants.ModulesStatus.STARTING);
        assertTrue(!CommandLineAction.getActionByKey(args[0]).perform(args).isEmpty());
    }

    /**
     * deprovision command with success
     */
    @Test
    public void deProvisionActionPerform() {
        String[] args = {"deprovision"};
        assertTrue(!CommandLineAction.getActionByKey(args[0]).getKeys().isEmpty());
        PowerMockito.when(fieldAgent.deProvision(anyBoolean())).thenReturn("done");
        assertEquals("Deprovisioning from controller ... done", commandLineAction.getActionByKey(args[0]).perform(args));
    }


    /**
     * Info command displaying the config report
     */
    @Test
    public void infoActionPerform() {

        String[] args = {"info"};
        assertTrue(!CommandLineAction.getActionByKey(args[0]).getKeys().isEmpty());
        when(Configuration.getConfigReport()).thenReturn("Config report");
        assertEquals("Config report", commandLineAction.getActionByKey(args[0]).perform(args));

    }

    /**
     * Switch command with no option displays help content
     */
    @Test
    public void switchActionPerformWithNoValue() {
        String[] args = {"switch"};
        assertTrue(!CommandLineAction.getActionByKey(args[0]).getKeys().isEmpty());
        assertEquals(helpContent, CommandLineAction.getActionByKey(args[0]).perform(args));
    }

    /**
     * Switch command with valid option
     */
    @Test
    public void switchActionPerform() {

        String[] anotherArgs = {"switch", "prod"};
        when(Configuration.setupConfigSwitcher(any())).thenReturn("success");
        assertEquals("success", commandLineAction.getActionByKey(anotherArgs[0]).perform(anotherArgs));
    }

    /**
     * Switch option with Invalid value
     * nn
     */
    @Test
    public void switchActionPerformWithInvalidValue() {

        String[] anotherArgs = {"switch", "dummy"};
        assertEquals("Invalid switcher state", commandLineAction.getActionByKey(anotherArgs[0]).perform(anotherArgs));
    }

    /**
     * When no value is passed with provision option
     */
    @Test
    public void provisionActionPerform() {
        String[] args = {"provision"};
        assertTrue(!CommandLineAction.getActionByKey(args[0]).getKeys().isEmpty());
        assertEquals(helpContent, CommandLineAction.getActionByKey(args[0]).perform(args));
    }

    /**
     * When provisioningResult of FieldAgent returns null response.
     */
    @Test
    public void provisionActionPerformTwoArgs() {

        String[] anotherArgs = {"provision", "prod"};
        assertEquals("Provisioning with key \"prod\" ... Result: \n" +
                "Provisioning failed", commandLineAction.getActionByKey(anotherArgs[0]).perform(anotherArgs));
    }

    /**
     * When FieldAgent.provision(provisionKey) returns the mock response with uuid
     */
    @Test
    public void provisionActionPerformReturnsUUID() {

        String[] anotherArgs1 = {"provision", "dummy"};
        assertEquals("Provisioning with key \"dummy\" ... Result: \n" +
                "Provision success - Iofog UUID is uuid", commandLineAction.getActionByKey(anotherArgs1[0]).perform(anotherArgs1));
    }

    /**
     * When FieldAgent.provision(provisionKey) returns the mock response without uuid
     */
    @Test
    public void provisionActionPerformResponseWithoutUUID() {

        String[] anotherArgs2 = {"provision", "anotherkey"};
        assertEquals("Provisioning with key \"anotherkey\" ... Result: \n" +
                        "Provision failed with error message: \"Key not valid\"",
                commandLineAction.getActionByKey(anotherArgs2[0]).perform(anotherArgs2));
    }

    /**
     * When config command without options display help
     */
    @Test
    public void configActionPerformWithOutOption() throws Exception {
        String[] args = {"config"};
        assertTrue(!CommandLineAction.getActionByKey(args[0]).getKeys().isEmpty());
        assertEquals(helpContent, CommandLineAction.getActionByKey(args[0]).perform(args));

    }
    /**
     * When config command without options display help
     */
    @Test
    public void configActionPerformWithInvalidOption() throws Exception {

        // Test when config option value is ambiguous
        String[] anotherArgs = {"config", "ambiguous"};
        assertEquals(helpContent, CommandLineAction.getActionByKey(anotherArgs[0]).perform(anotherArgs));

    }

    /**
     * When config command with default to reset the configuration Success
     */
    @Test
    public void configActionPerformWithDefaultOption() throws Exception {

        // Test when config reset to defaults
        String[] anotherArgs1 = {"config", "defaults"};
        assertEquals("Configuration has been reset to its defaults.",
                CommandLineAction.getActionByKey(anotherArgs1[0]).perform(anotherArgs1));

    }

    /**
     * When config command with valid option with no value
     */
    @Test
    public void configActionPerformWithValidOptionNoValue() throws Exception {

        // Test when config reset to defaults
        String[] logArgs = {"config", "-ll"};
        assertEquals(helpContent, CommandLineAction.getActionByKey(logArgs[0]).perform(logArgs));


    }

    /**
     * When config command with valid option with InValid value then Configuration throw exception
     */
    @Test
    public void configActionPerformWithValidOptionInvalidValue() throws Exception {

        // Test when config reset to defaults
        String[] logArgs = {"config", "-ll", "severeAndInfo"};

        PowerMockito.when(Configuration.getOldNodeValuesForParameters(anySet(), any())).
                thenReturn(result);
        PowerMockito.when(Configuration.setConfig(anyMap(), anyBoolean())).thenThrow(new Exception("item not found or defined more than once"));

        assertEquals("Error updating new config : item not found or defined more than once",
                CommandLineAction.getActionByKey(logArgs[0]).perform(logArgs));

    }


    /**
     * When config command with valid option and value
     */
    @Test
    public void configActionPerformWithValidOptionAndValue() throws Exception {

        String[] logArgs = {"config", "-ll", "severe"};
        PowerMockito.when(Configuration.setConfig(anyMap(), anyBoolean())).thenReturn(new HashMap<>());
        PowerMockito.when(Configuration.getOldNodeValuesForParameters(anySet(), any())).
                thenReturn(result);

        assertEquals("\\n\tChange accepted for Parameter : - ll, Old value was :info, New Value is : severe",
                CommandLineAction.getActionByKey(logArgs[0]).perform(logArgs));

    }

    /**
     * Helper method for comparing lists
     */
    private static boolean isEqual(List list1, List list2) {
        boolean value = list1.size() == list2.size() && list1.equals(list2);
        return value;
    }

    private String version = "ioFog 1.2.2 \n" +
            "Copyright (C) 2018 Edgeworx, Inc. \n" +
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
            "                                         that the software is allowed to use\\n" +
            "                 -dl <dir>               Set the directory to use for disk\\n" +
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
            "                                         can be used to control logging output" +
            "                 -sf <#seconds>          Set the status update frequency\\n" +
            "                 -cf <#seconds>          Set the get changes frequency\\n" +
            "                 -df <#seconds>          Set the post diagnostics frequency\\n" +
            "                 -sd <#seconds>          Set the scan devices frequency\\n" +
            "                 -idc <on/off>           Set the mode on which any not\\n" +
            "										  registered docker container will be\\n" +
            "										  shut down\\n" +
            "                 -gps <auto/off          Set gps location of fog.\\n" +
            "                      /#GPS DD.DDD(lat), Use auto to get coordinates by IP,\\n" +
            "                            DD.DDD(lon)  use off to forbid gps,\\n" +
            "                                         use GPS coordinates in DD format to set them manually\\n" +
            "                 -ft <auto               Set fog type.\\n" +
            "                     /intel_amd/arm>     Use auto to detect fog type by system commands,\\n" +
            "                                         use arm or intel_amd to set it manually\\n" +
            "                 -dev <on/off>           Set the developer's mode without using ssl \\n" +
            "                                         certificates. \\n" +
            "\\n" +
            "\\n" +
            "Report bugs to: edgemaster@iofog.org\\n" +
            "ioFog home page: http://iofog.org\\n" +
            "For users with Eclipse accounts, report bugs to: https://bugs.eclipse.org/bugs/enter_bug.cgi?product=iofog";
}
