/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.snapshot.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.duracloud.snapshot.restoration.RestoreStatus;
import org.duracloud.snapshot.restoration.SnapshotRestorationManager;
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
@Path("/restorations")
public class RestorationResource {
    
    private static Logger log = LoggerFactory.getLogger(RestorationResource.class);

    @Context
    HttpServletRequest request;

    @Context
    HttpHeaders headers;

    @Context
    UriInfo uriInfo;
   
    private SnapshotRestorationManager restorationManager;

    @Autowired
    public RestorationResource(SnapshotRestorationManager restorationManager) {
        this.restorationManager = restorationManager;
    }

    @Path("{snapshotId}/restore")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response restoreSnapshot(@PathParam("snapshotId") String snapshotId) {
        try {
            RestoreStatus status =  this.restorationManager.restoreSnapshot(snapshotId);
            return Response.ok().entity(status).build();
        }catch(Exception ex){
            log.error(ex.getMessage(),ex);
            return Response.serverError()
                .entity(new ResponseDetails(ex.getMessage()))
                .build();
        }
    }
    

    @Path("{snapshotId}/restore-complete")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response restoreComplete(@PathParam("snapshotId") String snapshotId) {

        try {
            RestoreStatus status =
                this.restorationManager.snapshotRestorationCompleted(snapshotId);
            return Response.ok().entity(status).build();
        }catch(Exception ex){
            log.error(ex.getMessage(),ex);
            return Response.serverError()
                .entity(new ResponseDetails(ex.getMessage()))
                .build();
        }
    }

}