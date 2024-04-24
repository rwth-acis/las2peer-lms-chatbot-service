package i5.las2peer.services.LMSChatbotService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.http.HttpRequest;

import javax.mail.internet.ContentType;
import javax.ws.rs.Consumes;
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
    private static HashMap<String, Boolean> isActive = new HashMap<String, Boolean>();
    
    public static String removeBrackets(String input) {
        String regex = "(?<=<)[^>\s]+(?=>)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        int x;
        int y;
        StringBuilder result = new StringBuilder(input);
        while (matcher.find()) {
            x = matcher.start()-1;
            result.setCharAt(x, '\'');
            y = matcher.end();
            result.setCharAt(y, '\'');
        }
        return result.toString();    
    }
	/*
	 * POST method to get the chat response from the LMS-Chatbot-Service
	 */
	@POST
	@Path("/chat")
	@Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
	@ApiResponses(
        value = { 
                @ApiResponse(
                    code = HttpURLConnection.HTTP_OK,
                    message = "Everything is okay!")})
	@ApiOperation(
			value = "Get the chat response from the LMS-Chatbot-Service",
			notes = "Returns the chat response from the LMS-Chatbot-Service")
	public Response chat(String body) throws IOException, ParseException {
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
        JSONObject json = new JSONObject();
        JSONObject chatResponse = new JSONObject();
        JSONObject newEvent = new JSONObject();
        // JSONObject monitorEvent61 = new JSONObject();
        // final long start = System.currentTimeMillis();
        json = (JSONObject) p.parse(body);
        System.out.println(json.toJSONString());
        String message = json.getAsString("msg");
        String channel = json.getAsString("channel");
        chatResponse.put("channel", channel);
        newEvent.put("msg", message);
        newEvent.put("channel", channel);
        isActive.put(channel, true);
        try {
			new Thread(new Runnable() {
				@Override
				public void run() {
                    try {
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
                            String final_response = removeBrackets(response.body());
                            System.out.print("Response from service: " + final_response);
                            // Update chatResponse with the result from the POST request
                            chatResponse.put("text", final_response);
                        } else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                            // Handle unsuccessful response
                            chatResponse.appendField("text", "An error has occurred.");
                        }

                    } catch (Exception e) {
						e.printStackTrace();
						isActive.put(channel, false);
						chatResponse.put("text", e.toString());
                    }

                    isActive.put(channel, false);
                }
            }).start();
            return Response.ok().entity(chatResponse).build();
        } catch (Exception e) {
            e.printStackTrace();
            chatResponse.appendField("text", "An unknown error has occurred.");
            return Response.ok().entity(chatResponse).build();
        }
	}
 
}
