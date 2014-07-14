/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.restoration;

/**
 * @author Daniel Bernstein
 *         Date: Jul 15, 2014
 */
public class RestorationConfig {
    private String restorationRootDir;

    private String[] duracloudEmailAddresses;
    private String[] dpnEmailAddresses;

    public RestorationConfig(){}
    
    /**
     * @return the restorationRootDir
     */
    public String getRestorationRootDir() {
        return restorationRootDir;
    }

    /**
     * @param restorationRootDir the restorationRootDir to set
     */
    public void setRestorationRootDir(String restorationRootDir) {
        this.restorationRootDir = restorationRootDir;
    }

    /**
     * @return the duracloudEmailAddresses
     */
    public String[] getDuracloudEmailAddresses() {
        return duracloudEmailAddresses;
    }

    /**
     * @param duracloudEmailAddresses the duracloudEmailAddresses to set
     */
    public void setDuracloudEmailAddresses(String[] duracloudEmailAddresses) {
        this.duracloudEmailAddresses = duracloudEmailAddresses;
    }

    /**
     * @return the dpnEmailAddresses
     */
    public String[] getDpnEmailAddresses() {
        return dpnEmailAddresses;
    }

    /**
     * @param dpnEmailAddresses the dpnEmailAddresses to set
     */
    public void setDpnEmailAddresses(String[] dpnEmailAddresses) {
        this.dpnEmailAddresses = dpnEmailAddresses;
    }

    

}
