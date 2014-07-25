/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.db.model;

public enum SnapshotStatus {
    INITIALIZED,
    TRANSFERRING_FROM_DURACLOUD,
    WAITING_FOR_DPN,
    SNAPSHOT_COMPLETE, 
    FAILED_TO_TRANSFER_FROM_DURACLOUD;
}