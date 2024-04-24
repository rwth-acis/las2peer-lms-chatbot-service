package i5.las2peer.services.LMSChatbotService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
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
import i5.las2peer.connectors.webConnector.client.ClientResponse;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import i5.las2peer.connectors.webConnector.client.MiniClient;
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
    private Boolean responseOK = false;
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
        chatResponse.put("closeContext", false);
        newEvent.put("msg", message);
        newEvent.put("channel", channel);

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

            return Response.ok().entity(chatResponse.toJSONString()).build();
        } catch (Exception e) {
            e.printStackTrace();
            chatResponse.appendField("text", "An unknown error has occurred.");
            return Response.ok().entity(chatResponse.toJSONString()).build();
        }
    }

	private JSONObject chatResponse = new JSONObject();

    @POST
	@Path("/chatAsync")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(
		value = { 
				@ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "Connected.")})
	@ApiOperation(
			value = "Get the chat response from LMS-Chatbot",
			notes = "Returns the chat response from LMS-Chatbot")
    public Response chatAsync(String body) throws IOException, ParseException {
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
        JSONObject json = new JSONObject();
        json = (JSONObject) p.parse(body);
        String message = json.getAsString("msg");
        String channel = json.getAsString("channel");
        String sbfmUrl = "https://git.tech4comp.dbis.rwth-aachen.de/SBFManager/MoodleAsync";
        chatResponse.put("channel", channel);
        chatResponse.put("closeContext", false);

        if (isActive.containsKey(channel)) {
            if(isActive.getOrDefault(channel, false)) {
                chatResponse.put("text", "Einen Moment bitte, ich verarbeit noch deine erste Nachricht.");
                chatResponse.put("closeContext", false);
                return Response.ok().entity(chatResponse.toJSONString()).build();
            }
        }
        
        isActive.put(channel, true);

        biwibotAsync(message, channel, sbfmUrl);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        if (!responseOK) {
            chatResponse.put("text", "Bitte warte einen Moment ich denke darüber nach.");
            chatResponse.put("closeContext", false);

            scheduler.scheduleAtFixedRate(() -> {
                callback(sbfmUrl, chatResponse);
                if (responseOK) {
                    chatResponse.clear();
                    responseOK=false;
                    scheduler.shutdown();
                }
            }, 0, 20, TimeUnit.SECONDS);
        }

        return Response.ok().entity(chatResponse.toString()).build();
    }

    public void biwibotAsync(String sbfmUrl, String msg, String channel){
        JSONObject chatResponse = new JSONObject();
        JSONObject newEvent = new JSONObject();
        chatResponse.put("closeContext", false);
        chatResponse.put("channel", channel);
        try {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        System.out.println("Thread started.");
                        String question = msg;
                        newEvent.put("msg", msg);
                        newEvent.put("channel", channel);

                        // Make the POST request to localhost:5000/chat
                        String url = "localhost:5000";
                        HttpClient httpClient = HttpClient.newHttpClient();
                        HttpRequest httpRequest = HttpRequest.newBuilder()
                                .uri(UriBuilder.fromUri(url).build())
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(newEvent.toJSONString()))
                                .build();

                        // Send the request
                        HttpResponse<String> serviceResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                        int responseCode = serviceResponse.statusCode();

                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            responseOK = true;
                            System.out.println("Response from service: " + serviceResponse.body());
                            chatResponse.put("text", serviceResponse.body());
                            // callback(sbfmUrl, chatResponse);
                        } else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                            responseOK = true;
                            // Handle unsuccessful response
                            chatResponse.put("text", "Error has occured.");
                            callback(sbfmUrl, chatResponse);
                        }
                        //System.out.println(chatResponse);
                        isActive.put(channel, false);
                    } catch ( IOException | InterruptedException e) {
                        responseOK = true;
                        e.printStackTrace();
                        chatResponse.put("text", "An error has occurred.");
                        isActive.put(channel, false);
                        callback(sbfmUrl, chatResponse);
                    } catch (Throwable e) {
                        responseOK = true;
                        e.printStackTrace();
                        chatResponse.put("text", "An unknown error has occurred.");
                        isActive.put(channel, false);
                        callback(sbfmUrl, chatResponse);
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            isActive.put(channel, false);
            // chatResponse.appendField("text","An error has occured (Exception).");
            return;
        } catch (Throwable e) {
            e.printStackTrace();
            isActive.put(channel,false);
            // chatResponse.appendField("text", "An unknown error has occured.");
            return;
        }

        return;
    }

    public void callback(String callbackUrl, JSONObject body){
		try {
			System.out.println("Starting callback to botmanager with url: " + callbackUrl);
			Client textClient = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
			String mp = null;
			System.out.println(body);
			mp = body.toJSONString();
			WebTarget target = textClient
					.target(callbackUrl);
			Response response = target.request()
					.post(javax.ws.rs.client.Entity.entity(mp, MediaType.APPLICATION_JSON));
					String test = response.readEntity(String.class);
			System.out.println("Finished callback to botmanager with response: " + test);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
