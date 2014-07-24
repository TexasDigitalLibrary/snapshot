/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.db.model;

public enum RestorationStatus {
    INITIALIZED,
    WAITING_FOR_DPN,
    DPN_TRANSFER_COMPLETE,
    TRANSFERRING_TO_DURACLOUD,
    VERIFYING_TRANSFERRED_CONTENT,
    RESTORATION_COMPLETE;
}