package io.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class MainVerticle extends AbstractVerticle {

  private SQLClient sqlClient;

  @Override
  public void start() {
    JsonObject dbConfig = new JsonObject()
      .put("url", "jdbc:postgresql://localhost:5432/postgres")
      .put("driver_class", "org.postgresql.Driver")
      .put("user", "postgres")
      .put("password", "admin");

    sqlClient = JDBCClient.createShared(vertx, dbConfig);

    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());

    // Define a route for handling /api/users
    router.get("/api").handler(ctx -> {
      ctx.request().response().end("Hello!");
    });

    router.get("/api").handler(this::easy);
    router.get("/api/getUser").handler(this::handleGetAllUsers);
    router.post("/api/createUser").handler(this::handleCreateUser);
    router.put("/api/updateUser").handler(this::handleUpdateUser);
    router.delete("/api/deleteUser").handler(this::handleDeleteUser);
    router.get("/api/getEpisode").handler(this::handleGetAllEpisodes);
    router.post("/api/createEpisode").handler(this::handleCreateEpisode);
    router.put("/api/updateEpisode").handler(this::handleUpdateEpisode);
    router.delete("/api/deleteEpisode").handler(this::handleDeleteEpisode);
    router.get("/api/getHealthRecords").handler(this::handleGetAllHealthRecords);
    router.post("/api/createHealthRecords").handler(this::handleCreateHealthRecord);
    router.put("/api/updateHealthRecords").handler(this::handleUpdateHealthRecord);
    router.delete("/api/deleteHealthRecords").handler(this::handleDeleteHealthRecord);
    router.get("/api/complaintsByAge").handler(this::handleComplaintsByAge);
    router.get("/api/episodeByTime").handler(this::handleGetEpisodesCount);


    vertx.createHttpServer().requestHandler(router).listen(8000);
  }

  public void easy(RoutingContext routingContext) {
    routingContext.response().end("Hello");
  }

  private void handleGetAllUsers(RoutingContext routingContext) {
    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.query("SELECT * FROM users", query -> {
          if (query.succeeded()) {
            JsonArray users = new JsonArray();
            for (JsonObject row : query.result().getRows()) {
              users.add(row);
            }
            routingContext.response().putHeader("content-type", "application/json").end(users.encode());
          } else {
            routingContext.response().setStatusCode(500).end("Database query failure: " + query.cause().getMessage());
          }
          connection.close();
        });
      } else {
        routingContext.response().setStatusCode(500).end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }

  private void handleCreateUser(RoutingContext routingContext) {
    JsonObject newUser = routingContext.getBodyAsJson();

    String name = newUser.getString("name");
    int age = newUser.getInteger("age");

    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.updateWithParams(
          "SELECT create_user(?, ?) as result",
          new JsonArray().add(name).add(age),
          insert -> {
            if (insert.succeeded()) {
              // Convert the result to a JsonObject
              JsonObject result = insert.result().toJson();
              // Check the result of the function call
              if (result.getBoolean("result")) {
                routingContext.response().setStatusCode(201).end("User created successfully");
              } else {
                routingContext.response().setStatusCode(500)
                  .end("Error creating user: " + result.getString("error"));
              }
            } else {
              routingContext.response().setStatusCode(500)
                .end("Database query failure: " + insert.cause().getMessage());
            }
            connection.close();
          });
      } else {
        routingContext.response().setStatusCode(500)
          .end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }


  private void handleUpdateUser(RoutingContext routingContext) {
    JsonObject updatedUser = routingContext.getBodyAsJson();

    // Assuming your JSON input looks like: {"id": 1, "name": "Updated Name", "age": 30}

    int userId = updatedUser.getInteger("id");
    String updatedName = updatedUser.getString("name");
    int updatedAge = updatedUser.getInteger("age");

    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.updateWithParams(
          "SELECT update_user(?, ?, ?) as result",
          new JsonArray().add(updatedName).add(updatedAge).add(userId),
          update -> {
            if (update.succeeded()) {
              routingContext.response().setStatusCode(200).end("User updated successfully");
            } else {
              routingContext.response().setStatusCode(500)
                .end("Database query failure: " + update.cause().getMessage());
            }
            connection.close();
          });
      } else {
        routingContext.response().setStatusCode(500)
          .end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }


  private void handleDeleteUser(RoutingContext routingContext) {
    JsonObject deleteUser = routingContext.getBodyAsJson();
    int userId = deleteUser.getInteger("user_id");

    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.updateWithParams(
          "CALL delete_user(?)",
          new JsonArray().add(userId),
          delete -> {
            if (delete.succeeded()) {
              routingContext.response().setStatusCode(200).end("User deleted successfully");
            } else {
              routingContext.response().setStatusCode(500)
                .end("Error deleting user: " + delete.cause().getMessage());
            }
            connection.close();
          });
      } else {
        routingContext.response().setStatusCode(500)
          .end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }



  private void handleGetAllEpisodes(RoutingContext routingContext) {
    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.query("SELECT episode_id, user_id, TO_CHAR(timestamp, 'YYYY-MM-DD HH24:MI:SS') AS timestamp, complaint_type, treatment FROM episodes", query -> {
          if (query.succeeded()) {
            JsonArray episodes = new JsonArray();
            for (JsonObject row : query.result().getRows()) {
              episodes.add(row);
            }
            routingContext.response().putHeader("content-type", "application/json").end(episodes.encode());
          } else {
            routingContext.response().setStatusCode(500).end("Database query failure: " + query.cause().getMessage());
          }
          connection.close();
        });
      } else {
        routingContext.response().setStatusCode(500).end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }


  private void handleCreateEpisode(RoutingContext routingContext) {
    JsonObject newEpisode = routingContext.getBodyAsJson();

    int userId = newEpisode.getInteger("user_id");
    String complaintType = newEpisode.getString("complaint_type");
    String treatment = newEpisode.getString("treatment");

    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.updateWithParams(
          "CALL create_episode(?, ?, ?)",
          new JsonArray().add(userId).add(complaintType).add(treatment),
          insert -> {
            if (insert.succeeded()) {
              routingContext.response().setStatusCode(201).end("Episode created successfully");
            } else {
              routingContext.response().setStatusCode(500)
                .end("Database query failure: " + insert.cause().getMessage());
            }
            connection.close();
          });
      } else {
        routingContext.response().setStatusCode(500)
          .end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }




  private void handleUpdateEpisode(RoutingContext routingContext) {
    JsonObject updatedEpisode = routingContext.getBodyAsJson();

    int episodeId = updatedEpisode.getInteger("episode_id");
    int userId = updatedEpisode.getInteger("user_id");
    String complaintType = updatedEpisode.getString("complaint_type");
    String treatment = updatedEpisode.getString("treatment");

    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.updateWithParams(
          "CALL update_episode(?, ?, ?, ?)",
          new JsonArray().add(episodeId).add(userId).add(complaintType).add(treatment),
          update -> {
            if (update.succeeded()) {
              routingContext.response().setStatusCode(200).end("Episode updated successfully");
            } else {
              routingContext.response().setStatusCode(500)
                .end("Database query failure: " + update.cause().getMessage());
            }
            connection.close();
          });
      } else {
        routingContext.response().setStatusCode(500)
          .end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }



  private void handleDeleteEpisode(RoutingContext routingContext) {
    JsonObject deleteEpisode = routingContext.getBodyAsJson();
    int episodeId = deleteEpisode.getInteger("episode_id");

    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.updateWithParams(
          "CALL delete_episode(?)",
          new JsonArray().add(episodeId),
          delete -> {
            if (delete.succeeded()) {
              routingContext.response().setStatusCode(200).end("Episode deleted successfully");
            } else {
              routingContext.response().setStatusCode(500)
                .end("Database query failure: " + delete.cause().getMessage());
            }
            connection.close();
          });
      } else {
        routingContext.response().setStatusCode(500)
          .end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }



  private void handleGetAllHealthRecords(RoutingContext routingContext) {
    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.query("SELECT * FROM health_records", query -> {
          if (query.succeeded()) {
            JsonArray healthRecords = new JsonArray();
            for (JsonObject row : query.result().getRows()) {
              healthRecords.add(row);
            }
            routingContext.response().putHeader("content-type", "application/json").end(healthRecords.encode());
          } else {
            routingContext.response().setStatusCode(500).end("Database query failure: " + query.cause().getMessage());
          }
          connection.close();
        });
      } else {
        routingContext.response().setStatusCode(500).end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }



  // Create Health Record
  private void handleCreateHealthRecord(RoutingContext routingContext) {
    JsonObject newHealthRecord = routingContext.getBodyAsJson();

    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.updateWithParams(
          "CALL create_health_record(?, ?)",
          new JsonArray()
            .add(newHealthRecord.getInteger("user_id"))
            .add(newHealthRecord.getString("health_info")),
          insert -> {
            if (insert.succeeded()) {
              routingContext.response().setStatusCode(201).end("Health Record created successfully");
            } else {
              routingContext.response().setStatusCode(500)
                .end("Database query failure: " + insert.cause().getMessage());
            }
            connection.close();
          });
      } else {
        routingContext.response().setStatusCode(500)
          .end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }

  // Update Health Record
  private void handleUpdateHealthRecord(RoutingContext routingContext) {
    JsonObject updatedHealthRecord = routingContext.getBodyAsJson();

    int recordId = updatedHealthRecord.getInteger("record_id");
    int userId = updatedHealthRecord.getInteger("user_id");
    String healthInfo = updatedHealthRecord.getString("health_info");

    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.updateWithParams(
          "CALL update_health_record(?, ?, ?)",
          new JsonArray().add(recordId).add(userId).add(healthInfo),
          update -> {
            if (update.succeeded()) {
              routingContext.response().setStatusCode(200).end("Health Record updated successfully");
            } else {
              routingContext.response().setStatusCode(500)
                .end("Database query failure: " + update.cause().getMessage());
            }
            connection.close();
          });
      } else {
        routingContext.response().setStatusCode(500)
          .end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }

  // Delete Health Record
  private void handleDeleteHealthRecord(RoutingContext routingContext) {
    JsonObject deleteHealthRecord = routingContext.getBodyAsJson();
    int recordId = deleteHealthRecord.getInteger("record_id");

    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.updateWithParams(
          "CALL delete_health_record(?)",
          new JsonArray().add(recordId),
          delete -> {
            if (delete.succeeded()) {
              routingContext.response().setStatusCode(200).end("Health Record deleted successfully");
            } else {
              routingContext.response().setStatusCode(500)
                .end("Database query failure: " + delete.cause().getMessage());
            }
            connection.close();
          });
      } else {
        routingContext.response().setStatusCode(500)
          .end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }


  private void handleComplaintsByAge(RoutingContext routingContext) {
    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.query("SELECT e.complaint_type, u.age, COUNT(*) AS episode_count " +
            "FROM episodes e " +
            "JOIN users u ON e.user_id = u.user_id " +
            "GROUP BY e.complaint_type, u.age " +
            "ORDER BY e.complaint_type, u.age",
          query -> {
            if (query.succeeded()) {
              JsonArray complaintsByAge = new JsonArray();
              for (JsonObject row : query.result().getRows()) {
                complaintsByAge.add(row);
              }
              routingContext.response().putHeader("content-type", "application/json")
                .end(complaintsByAge.encode());
            } else {
              routingContext.response().setStatusCode(500)
                .end("Database query failure: " + query.cause().getMessage());
            }
            connection.close();
          });
      } else {
        routingContext.response().setStatusCode(500)
          .end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }



  private void handleGetEpisodesCount(RoutingContext routingContext) {
    String timeUnit = routingContext.request().getParam("timeUnit");
    if (timeUnit == null || (!timeUnit.equalsIgnoreCase("WEEK") && !timeUnit.equalsIgnoreCase("MONTH"))) {
      routingContext.response().setStatusCode(400).end("Invalid timeUnit parameter. Use 'WEEK' or 'MONTH'.");
      return;
    }

    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();

        String query = "SELECT user_id, COUNT(*) AS episode_count " +
          "FROM episodes " +
          "WHERE timestamp >= CURRENT_DATE - INTERVAL '1 " + timeUnit + "' " +
          "GROUP BY user_id, DATE_TRUNC('" + timeUnit + "', timestamp::DATE)";

        connection.query(query, queryResult -> {
          if (queryResult.succeeded()) {
            JsonArray episodeCounts = new JsonArray();
            for (JsonObject row : queryResult.result().getRows()) {
              episodeCounts.add(row);
            }
            routingContext.response().putHeader("content-type", "application/json").end(episodeCounts.encode());
          } else {
            routingContext.response().setStatusCode(500).end("Database query failure: " + queryResult.cause().getMessage());
          }
          connection.close();
        });
      } else {
        routingContext.response().setStatusCode(500).end("Database connection failure: " + res.cause().getMessage());
      }
    });
  }








  @Override
  public void stop() {
    // Close the database connection
    sqlClient.close();
  }
}
