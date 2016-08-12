/**
 * Utility class to fetch Trusted Platform Module (TPM) attestation information -
 * values of the Platform Configuration Registers (PCRs) and the TPM event log from
 * VMware ESXi Server through vCenter Server.
 *
 * Does the following, for each connected host in vCenter Server,,
 * ---- QueryTPM via VC [Invoke QueryTpmAttestationReport(...)]
 * ---- Query TPM information from HostRuntimeInfo on VC
 * ---- Query TPM support & PCR information via ESXi [Through Host's HostRuntimeInfo -> tpmPcrValues object]
 *
 * Copyright (c) 2016
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * @author Gururaja Hegdal (ghegdal@vmware.com)
 * @version 1.0
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package querytpm;

// Entry point into the QueryTPM utility
public class RunApp
{
    /**
     * Usage method - how to use/invoke the script, reveals the options supported through this script
     */
    public static void usageQueryTPMScript()
    {
        System.out.println(
            "Usage: java -jar QueryTPM.jar --vsphereip <vCenter Server IP> --username <uname> --password <pwd> --esxUsername <uname> --esxPassword <pwd>");
        System.out.println(
            "\"java -jar QueryTPM.jar --vsphereip 10.4.5.6 --username admin --password dummyPwd --esxUsername rootUser --esxPassword dummyPwd\"");
    }

    /**
     * Main entry point
     */
    public static void main(String[] args) {

        System.out
            .println("######################### Host TPM information fetcher Script execution STARTED #########################");

        // Read command line arguments
        if (args.length > 0 && args.length >= 6) {
            FetchTPMInfo fetchTpmStatObj = new FetchTPMInfo(args);
            if (fetchTpmStatObj.validateProperties()) {
                fetchTpmStatObj.fetchTPMInfo();
            } else {
                usageQueryTPMScript();
            }
        } else {
            usageQueryTPMScript();
        }
        try {
            Thread.sleep(1000 * 2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(
            "######################### Host TPM information fetcher Script execution completed #########################");
    }
}