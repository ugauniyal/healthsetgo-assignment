//package io.vertx.example;
//
//import io.vertx.core.AbstractVerticle;
//import io.vertx.core.Promise;
//import io.vertx.core.http.HttpServer;
//import io.vertx.core.json.JsonArray;
//import io.vertx.core.json.JsonObject;
//import io.vertx.ext.jdbc.JDBCClient;
//import io.vertx.ext.sql.SQLClient;
//import io.vertx.ext.sql.SQLConnection;
//import io.vertx.ext.web.Router;
//import io.vertx.ext.web.RoutingContext;
//import io.vertx.ext.web.handler.BodyHandler;
//
//public class UserVerticle extends AbstractVerticle {
//
//  private SQLClient sqlClient;
//
//  @Override
//  public void start(Promise<Void> startPromise) {
//    // Configure the database connection
//    JsonObject dbConfig = new JsonObject()
//      .put("url", "jdbc:postgresql://localhost:5432/yourdatabase")
//      .put("driver_class", "org.postgresql.Driver")
//      .put("user", "youruser")
//      .put("password", "yourpassword");
//
//    sqlClient = JDBCClient.createShared(vertx, dbConfig);
//
//    // Deploy HTTP server
//    HttpServer server = vertx.createHttpServer();
//
//    // Create a router to handle HTTP requests
//    Router router = Router.router(vertx);
//
//    // Enable parsing of request bodies
//    router.route().handler(BodyHandler.create());
//
//    // Define API routes
//    router.post("/api/users").handler(this::handleCreateUser);
//    router.get("/api/users/:userId").handler(this::handleGetUser);
//    router.put("/api/users/:userId").handler(this::handleUpdateUser);
//    router.delete("/api/users/:userId").handler(this::handleDeleteUser);
//
//    // Start the HTTP server
//    server.requestHandler(router).listen(8080, result -> {
//      if (result.succeeded()) {
//        System.out.println("HTTP server started on port 8080");
//        startPromise.complete();
//      } else {
//        System.out.println("Failed to start HTTP server");
//        startPromise.fail(result.cause());
//      }
//    });
//  }
//
//  // Handler for creating a user
//  private void handleCreateUser(RoutingContext routingContext) {
//    routingContext.request().bodyHandler(buffer -> {
//      JsonObject user = buffer.toJsonObject();
//      sqlClient.getConnection(res -> {
//        if (res.succeeded()) {
//          SQLConnection connection = res.result();
//          connection.callWithParams(
//            "{call create_user(?, ?)}",
//            new JsonArray().add(user.getString("name")).add(user.getInteger("age")),
//            call -> handleDatabaseResponse(connection, call, routingContext, result -> {
//              routingContext.response().setStatusCode(201).end("User created successfully");
//            }));
//        } else {
//          handleDatabaseFailure(routingContext, res.cause());
//        }
//      });
//    });
//  }
//
//  // Handler for getting a user
//  private void handleGetUser(RoutingContext routingContext) {
//    int userId = Integer.parseInt(routingContext.pathParam("userId"));
//    sqlClient.getConnection(res -> {
//      if (res.succeeded()) {
//        SQLConnection connection = res.result();
//        connection.callWithParams(
//          "{call get_user(?)}",
//          new JsonArray().add(userId),
//          call -> handleDatabaseResponse(connection, call, routingContext, result -> {
//            JsonObject user = result.get(0);
//            routingContext.response().putHeader("content-type", "application/json").end(user.encode());
//          }));
//      } else {
//        handleDatabaseFailure(routingContext, res.cause());
//      }
//    });
//  }
//
//  // Handler for updating a user
//  private void handleUpdateUser(RoutingContext routingContext) {
//    int userId = Integer.parseInt(routingContext.pathParam("userId"));
//    routingContext.request().bodyHandler(buffer -> {
//      JsonObject user = buffer.toJsonObject();
//      sqlClient.getConnection(res -> {
//        if (res.succeeded()) {
//          SQLConnection connection = res.result();
//          connection.callWithParams(
//            "{call update_user(?, ?, ?)}",
//            new JsonArray().add(userId).add(user.getString("name")).add(user.getInteger("age")),
//            call -> handleDatabaseResponse(connection, call, routingContext, result -> {
//              routingContext.response().end("User updated successfully");
//            }));
//        } else {
//          handleDatabaseFailure(routingContext, res.cause());
//        }
//      });
//    });
//  }
//
//
//
//  // Handler for deleting a user
//  private void handleDeleteUser(RoutingContext routingContext) {
//    int userId = Integer.parseInt(routingContext.pathParam("userId"));
//    sqlClient.getConnection(res -> {
//      if (res.succeeded()) {
//        SQLConnection connection = res.result();
//        connection.callWithParams(
//          "{call delete_user(?)}",
//          new JsonArray().add(userId),
//          call -> handleDatabaseResponse(connection, call, routingContext, result -> {
//            routingContext.response().end("User deleted successfully");
//          }));
//      } else {
//        handleDatabaseFailure(routingContext, res.cause());
//      }
//    });
//  }
//
//  private void handleDatabaseResponse(SQLConnection connection, Object call, RoutingContext routingContext, Object userUpdatedSuccessfully) {
//    if (call instanceof io.vertx.ext.sql.SQLCallResult) {
//      io.vertx.ext.sql.SQLCallResult result = (io.vertx.ext.sql.SQLCallResult) call;
//      connection.close();
//      if (result.succeeded()) {
//        // Assuming the result contains user data
//        JsonArray userData = result.results().get(0);
//        userUpdatedSuccessfully.handle(userData);
//      } else {
//        handleDatabaseFailure(routingContext, result.cause());
//      }
//    } else {
//      // Handle unexpected call type
//      routingContext.response().setStatusCode(500).end("Unexpected call type");
//    }
//  }
//
//  private void handleDatabaseFailure(RoutingContext routingContext, Throwable cause) {
//    // Handle database connection failure
//    routingContext.response().setStatusCode(500).end("Database connection failure: " + cause.getMessage());
//  }
//
//
//  @Override
//  public void stop(Promise<Void> stopPromise) {
//    // Close the database connection
//    sqlClient.close();
//    stopPromise.complete();
//  }
//
//}
