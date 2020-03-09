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
package io.openliberty.guides.kitchen;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.GET;
// JAX-RS
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import io.openliberty.guides.models.Order;
import io.openliberty.guides.models.Status;

@ApplicationScoped
@Path("/foodMessaging")
public class KitchenResource {

	private Executor executor = Executors.newSingleThreadExecutor();
	private BlockingQueue<Order> inProgress = new LinkedBlockingQueue<>();
	private Random random = new Random();
	Jsonb jsonb = JsonbBuilder.create();

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response getProperties() {
		return Response.ok().entity(" In food service ").build();
	}

	@Incoming("foodOrderConsume")
	@Outgoing("foodOrderPublishIntermediate")
	@Acknowledgment(Acknowledgment.Strategy.NONE)
	public CompletionStage<Message<String>> initFoodOrder(Message<String> newOrder) throws Exception {
		/*System.out.println("1");
		//newOrder.ack();
		System.out.println("2");*/
		System.out.println("\n New Food Order received ");
		/*if ( true ) {
			newOrder.ack();
		}*/
		
		
		System.out.println(" Order : " + newOrder.getPayload());
		Order order = jsonb.fromJson(newOrder.getPayload(), Order.class);
		
		/*if ( order.getItem().equals("fries777")) {
			throw new Exception();
		}*/ 
		
		return prepareOrder(order).thenApply(Order -> Message.of(jsonb.toJson(Order)));
	}

	/*@Incoming("foodOrderConsume")
	@Outgoing("foodOrderPublishIntermediate")
	@Acknowledgment(Acknowledgment.Strategy.MANUAL)
	public CompletionStage<String> initFoodOrder( String newOrder) {
		System.out.println("1");
		//newOrder.ack();
		System.out.println("2");
	    System.out.println("\n New Food Order received ");
	    System.out.println( " Order : " + newOrder);
	    Order order = jsonb.fromJson(newOrder, Order.class);
	    return prepareOrder(order).thenApply(Order -> jsonb.toJson(Order));
	}*/

	private CompletionStage<Order> prepareOrder(Order order) {
		return CompletableFuture.supplyAsync(() -> {
			prepare(10);
			System.out.println(" Food Order in Progress... ");
			Order inProgressOrder = order.setStatus(Status.IN_PROGRESS);
			System.out.println(" Order : " + jsonb.toJson(inProgressOrder));
			inProgress.add(inProgressOrder);
			return inProgressOrder;
		}, executor);
	}

	private void prepare(int inputVal) {
		try {
			// Thread.sleep((random.nextInt(10)+20) * 1000);
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Outgoing("foodOrderPublish")
	public PublisherBuilder<String> sendReadyOrder() {
		return ReactiveStreams.generate(() -> {
			try {
				Order order = inProgress.take();
				prepare(20);
				order.setStatus(Status.READY);
				System.out.println(" Food Order Ready... ");
				System.out.println(" Order : " + jsonb.toJson(order));
				return jsonb.toJson(order);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		});
	}
}
