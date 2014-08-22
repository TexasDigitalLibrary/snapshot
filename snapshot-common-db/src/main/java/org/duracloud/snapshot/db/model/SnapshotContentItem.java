/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.db.model;

import java.util.Comparator;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.duracloud.snapshot.common.util.PropertiesSerializer;

/**
 * @author Daniel Bernstein
 *         Date: Jul 21, 2014
 */
@Entity
@Table (name="snapshot_content_item", uniqueConstraints=@UniqueConstraint(columnNames={"snapshot_id","content_id_hash" }))
public class SnapshotContentItem extends BaseEntity implements Comparator<SnapshotContentItem> {

    @Column(name="content_id", nullable=false, length = 1024)
    private String contentId;
    
    @Column(name="content_id_hash", nullable=false, length=50)
    private String contentIdHash;
    
    @ManyToOne(optional=false,targetEntity=Snapshot.class)
    @JoinColumn(name="snapshot_id", columnDefinition = "bigint(20)", nullable=false)
    private Snapshot snapshot;
    
    @Column(length=1024)
    private String metadata;

    /**
     * @return the contentId
     */
    public String getContentId() {
        return contentId;
    }

    /**
     * @param contentId the contentId to set
     */
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    /**
     * @return the snapshot
     */
    public Snapshot getSnapshot() {
        return snapshot;
    }

    /**
     * @param snapshot the snapshot to set
     */
    public void setSnapshot(Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    /**
     * @return the metadata
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    
    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(SnapshotContentItem o1, SnapshotContentItem o2) {
        return o1.contentId.compareTo(o2.contentId);
    }

    /**
     * @return the contentIdHash
     */
    public String getContentIdHash() {
        return contentIdHash;
    }

    /**
     * @param contentIdHash the contentIdHash to set
     */
    public void setContentIdHash(String contentIdHash) {
        this.contentIdHash = contentIdHash;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * @param props
     * @return
     */
    public static String serializeMetadata(Map<String, String> props) {
        return PropertiesSerializer.serialize(props);
    }

    /**
     * @param props
     * @return
     */
    public static Map<String,String> deserializeMetadata(String metadata) {
        return PropertiesSerializer.deserialize(metadata);
    }

}
