// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.status;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.openliberty.guides.models.Order;

@ApplicationScoped
@Path("/status")
public class StatusResource {

    private static Logger logger = Logger.getLogger(StatusResource.class.getName());

    @Inject
    private StatusManager manager;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrdersList() {
        List<Order> ordersList = manager.getOrders()
                .values()
                .stream()
                .collect(Collectors.toList());
        return Response
                .status(Response.Status.OK)
                .entity(ordersList)
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/order/{orderId}")
    public Response getOrder(@PathParam("orderId") String orderId) {
        Optional<Order> order = manager.getOrder(orderId);
        if (order.isPresent()) {
            return Response
                    .status(Response.Status.OK)
                    .entity(order)
                    .build();
        }
        return Response
                .status(Response.Status.NOT_FOUND)
                .entity("Order id does not exist.")
                .build();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/table/{tableId}")
    public Response getOrdersList(@PathParam("tableId") String tableId) {
        List<Order> ordersList = manager.getOrders()
                .values()
                .stream()
                .filter(order -> order.getTableId().equals(tableId))
                .collect(Collectors.toList());
        return Response
                .status(Response.Status.OK)
                .entity(ordersList)
                .build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetOrder() {
        manager.resetOrder();
        return Response
                .status(Response.Status.OK)
                .build();
    }
    
    // tag::updateStatus[]
    @Incoming("updateStatus")
    // end::updateStatus[]
    // tag::ackManual[]
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    // end::ackManual[]
    // tag::updateStatusMethod[]
    public CompletionStage<Void> updateStatus(Message<Order> order)  {
        String orderId = order.getPayload().getOrderId();
        if (manager.getOrder(orderId).isPresent()) {
            manager.updateStatus(orderId, order.getPayload().getStatus());
            logger.info("Order " + orderId + " status updated to "
                + order.getPayload().getStatus() + ": " + order);
        } else {
            manager.addOrder(order.getPayload());
            logger.info("Order " + orderId + " was added: " + order);	
        }
        // tag::manualAckUser[]
        return order.ack();
        // end::manualAckUser[]
    }
    // end::updateStatusMethod[]
}
