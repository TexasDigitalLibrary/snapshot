/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.httpclient.HttpStatus;
import org.duracloud.snapshot.spring.batch.SnapshotException;
import org.duracloud.snapshot.spring.batch.SnapshotJobManager;
import org.duracloud.snapshot.spring.batch.SnapshotNotFoundException;
import org.duracloud.snapshot.spring.batch.SnapshotStatus;
import org.duracloud.snapshot.spring.batch.SnapshotSummary;
import org.duracloud.snapshot.spring.batch.config.SnapshotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Defines the REST resource layer for interacting with the Snapshot processing
 * engine.
 * 
 * @author Daniel Bernstein Date: Feb 4, 2014
 */
@Component
public class SnapshotResource {
    
    private static Logger log = LoggerFactory.getLogger(SnapshotResource.class);

    @Context
    HttpServletRequest request;

    @Context
    HttpHeaders headers;

    @Context
    UriInfo uriInfo;
   
    private SnapshotJobManager jobManager;
    
    @Autowired
    public SnapshotResource(SnapshotJobManager jobManager) {
        this.jobManager = jobManager;
    }    
    
    /**
     * Returns a list of snapshots.
     * 
     * @return
     */
    @Path("/snapshots")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        return Response.ok().entity(getSnapshotList()).build();
    }

    
    /**
     * @return
     */
    private List<SnapshotSummary> getSnapshotList() {
        return this.jobManager.getSnapshotList();
    }

    @Path("/snapshots/{snapshotId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Returns the status of a snapshot. The fields available in the response will match
     * those in <code>SnapshotStatus</code>.
     * @param snapshotId
     * @return
     */
    public Response getStatus(@PathParam("snapshotId") String snapshotId) {
        try {
            SnapshotStatus status = this.jobManager.getStatus(snapshotId);
            return Response.ok().entity(status).build();
        } catch (SnapshotNotFoundException ex) {
            log.error(ex.getMessage(), ex);
            return Response.status(HttpStatus.SC_NOT_FOUND)
                           .entity(new ResponseDetails(ex.getMessage()))
                           .build();
        } catch (SnapshotException ex) {
            log.error(ex.getMessage(), ex);
            return Response.serverError()
                           .entity(new ResponseDetails(ex.getMessage()))
                           .build();
        }
    }

    @Path("/snapshots/{snapshotId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@PathParam("snapshotId") String snapshotId, CreateSnapshotParams params) {

        try {

            SnapshotConfig config = new SnapshotConfig();
            config.setHost(params.getHost());
            config.setPort(Integer.parseInt(params.getPort()));
            config.setStoreId(params.getStoreId());
            config.setSpace(params.getSpaceId());
            config.setSnapshotId(snapshotId);
            SnapshotStatus status = this.jobManager.executeSnapshotAsync(config);
            return Response.created(null)
                .entity(status)
                .build();
        }catch(Exception ex){
            log.error(ex.getMessage(),ex);
            return Response.serverError()
                .entity(new ResponseDetails(ex.getMessage()))
                .build();
        }
    }
}