package i5.las2peer.services.LMSChatbotService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

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
@ServicePath("/chat")
public class LMSChatbotService extends RESTService {
	/*
	 * POST method to get the chat response from the LMS-Chatbot-Service
	 */
	@POST
	@Path("/chat")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "Everything is okay!") })
	@ApiOperation(
			value = "getInput",
			notes = "Method that returns a phrase containing the received input.")
	public Response chat(String body) {
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		JSONObject bodyJson = null;
		JSONObject payloadJson = new JSONObject();
		JSONObject monitorEvent51 = new JSONObject();
		final long start = System.currentTimeMillis();
		try {
			bodyJson = (JSONObject) p.parse(body);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		payloadJson.put("message", bodyJson.getAsString("chat"));

		// get response result from python
		try {
			String line = null;
			StringBuilder sb = new StringBuilder ();
			String res = null;

			URL url = UriBuilder.fromPath("http://localhost:5000/chat")
						.path(URLEncoder.encode(payloadJson.toString(), "UTF-8").replace("+","%20"))
						.build()
						.toURL();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.connect();
			BufferedReader rd  = new BufferedReader( new InputStreamReader(connection.getInputStream(), "UTF-8"));

			while ((line = rd.readLine()) != null ) {
				sb.append(line);
			}
			res = sb.toString();
			monitorEvent51.put("Task", "Response Result");
			monitorEvent51.put("Process time", System.currentTimeMillis() - start);
            Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_51,monitorEvent51.toString());
			return Response.ok().entity(res).build();
		} catch (IOException e) {
			e.printStackTrace();
			bodyJson.put("text", "An error has occurred.");
			monitorEvent51.put("Task", "Error: Response failed.");
			monitorEvent51.put("Process time", System.currentTimeMillis() - start);
            Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_51,monitorEvent51.toString());
			return Response.ok().entity(bodyJson.toString()).build();
		}
	}

}
