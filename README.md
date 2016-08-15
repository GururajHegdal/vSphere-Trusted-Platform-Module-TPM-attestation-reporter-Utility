# vSphere-Trusted-Platform-Module-TPM-attestation-reporter-Utility
### 1. Details
Utility to fetch Trusted Platform Module (TPM) attestation information values of the Platform Configuration Registers (PCRs) and the TPM event log from
VMware ESXi Server through vCenter Server.  
Does the following, for each connected host in vCenter Server,
 *  QueryTPM via VC [Invoke QueryTpmAttestationReport(...)]
 * Query TPM information from HostRuntimeInfo on VC
 * Query TPM support & PCR information via ESXi [Through Host's HostRuntimeInfo -> tpmPcrValues object]

### 2. How to run the Utility?
##### Run from Dev IDE

 * Import files under the src/querytpm/ folder into your IDE.
 * Required libraries are embedded within Runnable-Jar/QueryTPM.jar, extract & import the libraries into the project.
 *  Run the utility from 'RunApp' program by providing arguments like:  
 _--vsphereip 1.2.3.4 --username adminUser --password dummyPasswd --esxUsername rootUser --esxPassword rootPwd_

If the username and password for ESXi hosts differ, source code can easily be edited to include simple logic to fetch username/password per ESXi host.

##### Run from Pre-built Jars
 * Copy/Download the QueryTPM.jar from Runnable-jar folder (from the uploaded file) and unzip on to local drive folder say c:\QueryTPM
 * Open a command prompt and cd to the folder, lets say cd QueryTPM
 * Run a command like shown below to see various usage commands:  
 _C:\QueryTPM>java -jar QueryTPM.jar --help_
