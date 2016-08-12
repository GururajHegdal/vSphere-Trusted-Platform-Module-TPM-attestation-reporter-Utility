/**
 * Utility class to fetch Trusted Platform Module (TPM) attestation information -
 * values of the Platform Configuration Registers (PCRs) and the TPM event log from
 * VMware ESXi Server through vCenter Server.
 *
 * Does the following, for each connected host in vCenter Server,
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

import java.net.URL;
import java.rmi.RemoteException;

import com.vmware.vim25.HostRuntimeInfo;
import com.vmware.vim25.HostSystemConnectionState;
import com.vmware.vim25.HostTpmAttestationReport;
import com.vmware.vim25.HostTpmDigestInfo;
import com.vmware.vim25.HostTpmEventDetails;
import com.vmware.vim25.HostTpmEventLogEntry;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;


public class FetchTPMInfo
{
    private String vsphereIp;
    private String userName;
    private String password;
    private String esx_username;
    private String esx_password;
    private String url;
    private ServiceInstance si;
    private HostSystem hostdHostSys;

    // VC inventory related objects
    private static final String HOST_MOR_TYPE = "HostSystem";

    /**
     * Constructor
     */
    public FetchTPMInfo(String[] cmdProps)
    {
        makeProperties(cmdProps);
    }

    /**
     * Read properties from command line arguments
     */
    private void
    makeProperties(String[] cmdProps)
    {
        // get the property value and print it out
        System.out.println("Reading vSphere IP and Credentials information from command line arguments");
        System.out.println("-------------------------------------------------------------------");

        for (int i = 0; i < cmdProps.length; i++) {
            if (cmdProps[i].equals("--vsphereip")) {
                vsphereIp = cmdProps[i + 1];
                System.out.println("vSphere IP:" + vsphereIp);
            } else if (cmdProps[i].equals("--username")) {
                userName = cmdProps[i + 1];
                System.out.println("VC Username:" + userName);
            } else if (cmdProps[i].equals("--password")) {
                password = cmdProps[i + 1];
                System.out.println("VC password: ******");
            }  else if (cmdProps[i].equals("--esxUsername")) {
                esx_username = cmdProps[i + 1];
                System.out.println("ESXi Username:" + esx_username);
            } else if (cmdProps[i].equals("--esxPassword")) {
                esx_password = cmdProps[i + 1];
                System.out.println("ESXi Password: ******");
            }
        }
        System.out.println("-------------------------------------------------------------------\n");
    }

    /**
     * Validate property values
     */
    boolean
    validateProperties()
    {
        boolean val = false;
        if (vsphereIp != null) {
            url = "https://" + vsphereIp + "/sdk";

            // Login to provided server IP to determine if we are running against single ESXi
            try {
                System.out.println("Logging into vSphere : " + vsphereIp + ", with provided credentials");
                si = loginTovSphere(url);

                if (si != null) {
                    System.out.println("Succesfully logged into vSphere: " + vsphereIp);
                    val = true;
                } else {
                    System.err.println(
                        "Service Instance object for vSphere:" + vsphereIp + " is null, probably we failed to login");
                    printFailedLoginReasons();
                }
            } catch (Exception e) {
                System.err.println(
                    "Caught an exception, while logging into vSphere :" + vsphereIp + " with provided credentials");
                printFailedLoginReasons();
            }

            if (!(esx_username != null && esx_password != null)) {
                System.err.println("Pls specify ESXi host credentials for logging into ESXi host");
            }

        }
        return val;
    }

    /**
     * Method prints out possible reasons for failed login
     */
    private void
    printFailedLoginReasons()
    {
        System.err.println(
            "Possible reasons:\n1. Provided username/password credentials are incorrect\n"
                + "2. If username/password or other fields contain special characters, surround them with double "
                + "quotes and for non-windows environment with single quotes (Refer readme doc for more information)\n"
                + "3. vCenter Server/ESXi server might not be reachable"
                + "4. vCenter Server service is configured with custom port (other than 443), If so specify vsphereip as \"serverip:customport\"");
    }

    /**
     * Login method to VC
     */
    private ServiceInstance
    loginTovSphere(String url)
    {
        try {
            si = new ServiceInstance(new URL(url), userName, password, true);
        } catch (Exception e) {
            System.out.println("Caught exception while logging into vSphere server");
            e.printStackTrace();
        }
        return si;
    }

    /**
     * Fetch all Stats method
     */
    public void
    fetchTPMInfo()
    {
        String tempHostName = null;
        System.out.println("Retrieving all hosts from VC ...");
        ManagedEntity[] allHosts = retrieveAllHosts();

        if (allHosts != null) {
            for (ManagedEntity host : allHosts) {
                try {
                    tempHostName = host.getName();
                    System.out.println(
                        "\n******************************************************************************");
                    System.out.println("\t\t\tHost : " + tempHostName);
                    System.out.println(
                        "******************************************************************************");
                    System.out.println("\n---- QueryTPM via VC ...");
                    HostSystem hostSysFrmVC = new HostSystem(si.getServerConnection(), host.getMOR());
                    queryTpm(hostSysFrmVC);

                    System.out.println("\n---- Query TPM information from HostRuntimeInfo on VC ...");
                    queryRuntimeInfoForTpmPcr(hostSysFrmVC);

                    System.out.println("\n---- Query TPM support & PCR information via ESXi ...");
                    queryTPMviaESXi(tempHostName);

                } catch (Exception e) {
                    System.err.println("Caught exception while QueryingTPM information from host: " + tempHostName);
                }

            } // End of hosts for loop

        } else {
            System.err.println("Could not find any hosts in inventory");
        }
    }

    /**
     * Query TPM on Host through VC
     */
    private void
    queryTpm(HostSystem hostSys) throws RemoteException
    {
        try {
            HostTpmAttestationReport tpmReport = hostSys.queryTpmAttestationReport();
            System.out.println("_______________________________________________________________________________");
            System.out.println(" * * * * Query TPM events... * * * *");
            System.out.println("_______________________________________________________________________________");
            HostTpmEventLogEntry[] tpmEvents = tpmReport.getTpmEvents();
            for (HostTpmEventLogEntry eveEntry : tpmEvents) {
                System.out.println("---------------------------------");
                HostTpmEventDetails details = eveEntry.getEventDetails();
                System.out.println("PCR Index: " + eveEntry.getPcrIndex());
                System.out.println("Hash: ");
                for (Byte hash : details.getDataHash()) {
                    System.out.print(hash.byteValue() + ", ");
                }
                System.out.println("\n---------------------------------");

            }

            // Print out PCR values
            printPCRVal(tpmReport.getTpmPcrValues());

        } catch (NullPointerException npe) {
            System.out.println("[ALERT] Caught NullPointerException, Indicates that TPM property is unset");
        }
    }

    /**
     * Query TPM information via ESXi
     */
    private void
    queryTPMviaESXi(String hostName)
    {
        boolean hostdLoginSuccess = false;
        ServiceInstance hostSI = null;

        try {
            String hostUrl = "https://" + hostName + "/sdk";
            hostSI = new ServiceInstance(new URL(hostUrl), esx_username, esx_password, true);
            if (hostSI != null) {
                hostdLoginSuccess = true;
            }
        } catch (Exception e) {
            System.err.println("Caught exception while logging into Host: " + hostName);
        }

        try {
            if (hostdLoginSuccess) {
                hostdHostSys = retrieveSingleHostSys(hostSI, hostName, false);

                if (hostdHostSys != null) {
                    System.out.println("ESXi host TPM Support: " + hostdHostSys.getCapability().getTpmSupported());
                    queryRuntimeInfoForTpmPcr(hostdHostSys);
                } else {
                    System.err.println("Failed retrieve HostSystem object through HostAgent API");
                }
            } else {
                System.err.println("Failed to login to Host through HostAgent APIs");
            }
        } catch (Exception e) {
            System.err.println("Caught exception while Querying TPM information");
        }
    }

    /**
     * Print TPM PCR value
     */
    private void
    printPCRVal(HostTpmDigestInfo[] pcrVals)
    {
        System.out.println("_______________________________________________________________________________");
        System.out.println("* * * * Query TPM Digest Information... * * * *");
        System.out.println("_______________________________________________________________________________");
        for (HostTpmDigestInfo tpmDigestInfo : pcrVals) {
            System.out.println("---------------------------------");
            System.out.println("Digest Method: " + tpmDigestInfo.getDigestMethod());
            System.out.println("Object Name: " + tpmDigestInfo.getObjectName());
            System.out.println("PCR Number: " + tpmDigestInfo.getPcrNumber());
            System.out.println("Digest Value: ");
            for (Byte digVal : tpmDigestInfo.getDigestValue()) {
                System.out.print(digVal.byteValue() + ", ");
            }
            System.out.println("\n---------------------------------");
        }
    }

    /**
     * Retrieve HostRuntimeInfo -> tpmPcrValues object and print out TPM PCR values
     */
    private void
    queryRuntimeInfoForTpmPcr(HostSystem tempHostSys)
    {
        System.out.println("Querying TPMPCR information ...");
        if (tempHostSys != null) {
            HostRuntimeInfo runtimeInfo = tempHostSys.getRuntime();
            if (runtimeInfo != null) {
                System.out.println("Retrieved Host runtime information");
                HostTpmDigestInfo[] pcrVals = runtimeInfo.tpmPcrValues;
                if (pcrVals != null && pcrVals.length > 0) {
                    printPCRVal(pcrVals);
                } else {
                    System.err.println("TPM PCR information is null");
                }
            } else {
                System.err.println("Host runtimeinformation is null");
            }
        } else {
            System.err.println("Provided HostSystem object is null");
        }
    }

    /**
     * Get All hosts
     */
    private ManagedEntity[]
    retrieveAllHosts()
    {
        // get first datacenters in the environment.
        InventoryNavigator navigator = new InventoryNavigator(si.getRootFolder());
        ManagedEntity[] hosts = null;
        try {
            hosts = navigator.searchManagedEntities(HOST_MOR_TYPE);
        } catch (Exception e) {
            System.err.println("[Error] Unable to retrive Hosts from inventory");
            e.printStackTrace();
        }
        return hosts;
    }

    /**
     * Return hosts HostSystem object
     */
    private HostSystem
    retrieveSingleHostSys(ServiceInstance servInstance, String hostName, boolean searchInVc)
    {
        HostSystem hostSys = null;

        // get first datacenter in the environment.
        InventoryNavigator navigator = new InventoryNavigator(servInstance.getRootFolder());

        try {
            if (searchInVc) {
                hostSys = (HostSystem) navigator.searchManagedEntity(HOST_MOR_TYPE, hostName);
            } else {
                hostSys = (HostSystem) navigator.searchManagedEntities(HOST_MOR_TYPE)[0];
            }

            if (hostSys != null) {
                HostRuntimeInfo hostruntimeInfo = hostSys.getRuntime();
                if ((hostruntimeInfo.getConnectionState().equals(HostSystemConnectionState.connected))) {
                    System.out.println("Found user provided ESXi host: " + hostName + " in connected state");
                } else {
                    System.err.println("User provided ESXi host: " + hostName + " in not found in connected state");
                }
            } else {
                System.err.println("Unable to retrieve provided Host's: " + hostName + " HostSystem object from inventory");
            }

        } catch (Exception e) {
            System.err.println(
                "Caught exception while retrieving provided Host's " + hostName + " HostSystem object from inventory");
            hostSys = null;
        }
        return hostSys;
    }
}