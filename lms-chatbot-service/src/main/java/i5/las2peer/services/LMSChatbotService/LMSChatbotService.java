package i5.las2peer.services.LMSChatbotService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;

import javax.mail.internet.ContentType;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

// TODO Describe your own service
/**
 * las2peer-lms-chatbot-service
 * 
 * This is a template for a very basic las2peer service that uses the las2peer WebConnector for RESTful access to it.
 * 
 * Note: If you plan on using Swagger you should adapt the information below in the SwaggerDefinition annotation to suit
 * your project. If you do not intend to provide a Swagger documentation of your service API, the entire Api and
 * SwaggerDefinition annotation should be removed.
 * 
 */
// TODO Adjust the following configuration
@Api
@SwaggerDefinition(
		info = @Info(
				title = "las2peer lms chatbot service",
				version = "1.0.0",
				description = "A las2peer chatbot service for Moodle.",
				contact = @Contact(
						name = "Yue Yin",
						email = "yue.yin@rwth-aachen.de"),
				license = @License(
						name = "LCS",
						url = "https://github.com/rwth-acis/las2peer-lms-chatbot-service/blob/main/LICENSE")))
@ServicePath("/lms-chatbot")
public class LMSChatbotService extends RESTService {
	/*
	 * POST method to get the chat response from the LMS-Chatbot-Service
	 */
	@POST
	@Path("/chat")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "Everything is okay!") })
	@ApiOperation(
			value = "getInput",
			notes = "Method that returns a phrase containing the received input.")
	public Response chat(String body) {
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
        JSONObject json = null;
        JSONObject chatResponse = new JSONObject();
        JSONObject newEvent = new JSONObject();
        String message = null;
        String channel = null;

        try {
            json = (JSONObject) p.parse(body);
            message = json.getAsString("msg");
            channel = json.getAsString("channel");
            chatResponse.put("channel", channel);
            newEvent.put("msg", message);
            newEvent.put("channel", channel);

            // Make the POST request to localhost:5000/chat
            String url = "http://localhost:5000/chat";
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(UriBuilder.fromUri(url).build())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(newEvent.toJSONString()))
                    .build();

            // Send the request
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int responseCode = response.statusCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Update chatResponse with the result from the POST request
                chatResponse.put("text", response.body());
            } else {
                // Handle unsuccessful response
                chatResponse.appendField("text", "An error has occurred.");
            }

        } catch (ParseException | IOException | InterruptedException e) {
            e.printStackTrace();
            chatResponse.appendField("text", "An error has occurred.");
        }

        return Response.ok().entity(chatResponse).build();

	}
 
}
