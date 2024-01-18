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

import java.util.Arrays;

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

    createTables();

    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());

    router.get("/api").handler(ctx -> {
      ctx.request().response().end("Hello!");
    });


//    All APIs instructions
    router.get("/").handler(routingContext -> {
      routingContext.response().putHeader("content-type", "text/html").end(
        "<!DOCTYPE html>\n" +
          "<html lang=\"en\">\n" +
          "<head>\n" +
          "  <meta charset=\"UTF-8\">\n" +
          "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
          "  <title>API Documentation</title>\n" +
          "</head>\n" +
          "<body>\n" +
          "<h1>API Documentation</h1>\n" +
          "<p>Welcome to the API documentation. Here are the available endpoints:</p>\n" +
          "<ul>\n" +
          "  <li><strong>GET</strong> /api/getUser - Get all users</li>\n" +
          "  <li><strong>POST</strong> /api/createUser - Create a new user</li>\n" +
          "  <li><strong>PUT</strong> /api/updateUser - Update a user</li>\n" +
          "  <li><strong>DELETE</strong> /api/deleteUser - Delete a user</li>\n" +
          "  <li><strong>GET</strong> /api/getEpisode - Get all episodes</li>\n" +
          "  <li><strong>POST</strong> /api/createEpisode - Create a new episode</li>\n" +
          "  <li><strong>PUT</strong> /api/updateEpisode - Update an episode</li>\n" +
          "  <li><strong>DELETE</strong> /api/deleteEpisode - Delete an episode</li>\n" +
          "  <li><strong>GET</strong> /api/getHealthRecords - Get all health records</li>\n" +
          "  <li><strong>POST</strong> /api/createHealthRecords - Create a new health record</li>\n" +
          "  <li><strong>PUT</strong> /api/updateHealthRecords - Update a health record</li>\n" +
          "  <li><strong>DELETE</strong> /api/deleteHealthRecords - Delete a health record</li>\n" +
          "  <li><strong>GET</strong> /api/complaintsByAge - Get complaints by age</li>\n" +
          "  <li><strong>GET</strong> /api/episodeByTime - Get episode counts by time unit</li>\n" +
          "</ul>\n" +
          "</body>\n" +
          "</html>"
      );
    });


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

  private void createTables() {
    sqlClient.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        connection.batch(Arrays.asList(
          "CREATE TABLE IF NOT EXISTS users (user_id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, age INT NOT NULL);",
          "CREATE TABLE IF NOT EXISTS health_records (record_id SERIAL PRIMARY KEY, user_id INT REFERENCES users(user_id), health_info JSONB);",
          "CREATE TABLE IF NOT EXISTS episodes (episode_id SERIAL PRIMARY KEY, user_id INT REFERENCES users(user_id), timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, complaint_type VARCHAR(255) NOT NULL, treatment VARCHAR(255));"
        ), createHandler -> {
          if (createHandler.succeeded()) {
            System.out.println("Tables created successfully");
          } else {
            System.err.println("Error creating tables: " + createHandler.cause().getMessage());
          }
          connection.close();
        });
      } else {
        System.err.println("Unable to connect to the database: " + res.cause().getMessage());
      }
    });
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
