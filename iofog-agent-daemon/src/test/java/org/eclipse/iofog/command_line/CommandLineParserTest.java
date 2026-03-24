/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
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
import static org.eclipse.iofog.utils.CmdProperties.getVersion;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.json.JsonObject;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
public class CommandLineParserTest {
    private MockedStatic<CommandLineAction> cmdLineAction;
    @BeforeEach
    public void setUp() throws Exception {
        cmdLineAction = mockStatic(CommandLineAction.class);
        String[] args = {"help", "provision"};
        cmdLineAction.when(() -> CommandLineAction.getActionByKey(args[0]))
                .thenReturn(CommandLineAction.HELP_ACTION);
        cmdLineAction.when(() -> CommandLineAction.HELP_ACTION.perform(args)).thenReturn(helpContent);
        cmdLineAction.when(() -> CommandLineAction.getActionByKey(args[1]))
                .thenReturn(CommandLineAction.PROVISION_ACTION);
    }

    @AfterEach
    public void tearDown() throws Exception {
        cmdLineAction.close();
    }

    /**
     * Test parse method
     */
    @Test
    public void testParse() {
        try {
            Assertions.assertEquals(helpContent, CommandLineParser.parse("help"));
            CommandLineAction.getActionByKey("help");
        } catch (AgentUserException e) {
            fail("This should never happen");
        }
    }

    private static final String helpContent = "\n" +
        "  _        __                                     _   \n" +
        " (_)      / _|                                   | |  \n" +
        "  _  ___ | |_ ___   __ _    __ _  __ _  ___ _ __ | |_ \n" +
        " | |/ _ \\|  _/ _ \\ / _` |  / _` |/ _` |/ _ \\ '_ \\| __|\n" +
        " | | (_) | || (_) | (_| | | (_| | (_| |  __/ | | | |_ \n" +
        " |_|\\___/|_| \\___/ \\__, |  \\__,_|\\__, |\\___|_| |_|\\__|\n" +
        "                    __/ |         __/ |               \n" +
        "                   |___/         |___/                \n" +
        "                                                                                \n" +
        "  Datasance PoT ioFog Agent v" + getVersion() + "\n" +
        "  Command Line Interface\n" +
        "  =====================\n\n" +
        "Usage 1: iofog-agent [OPTION]\\n" +
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
        "cert            <base64encodedcert>      Set the controller CA certificate\\n" +
        "                                         for secure communication\\n" +
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
        "                 -gpsd <device>          Set the GPS device to use (example: /dev/ttyUSB0)\\n" +
        "                 -gpsf <#seconds>        Set the GPS scan frequency\\n" +
        "                 -egf <#seconds>         Set the edge guard frequency\\n" +
        "                 -ft <auto               Set fog type.\\n" +
        "                     /intel_amd/arm>     Use auto to detect fog type by system commands,\\n" +
        "                                         use arm or intel_amd to set it manually\\n" +
        "                 -pf <#hours>            Set the docker pruning frequency.\n" +
        "                 -sec <on/off>           Set the secure mode without using ssl \\n" +
        "                                         certificates. \\n" +
        "                 -dev <on/off>           Set the developer's mode\\n" +
        "                 -tz                     Set the device timeZone\\n" +
        "\\n" +
        "\\n" +
        "Report bugs to: developer@datasance.com\\n" +
        "Datasance PoT docs: https://docs.datasance.com\\n" +
        "For users with GitHub accounts, report bugs to: https://github.com/Datasance/Agent/issues";
}