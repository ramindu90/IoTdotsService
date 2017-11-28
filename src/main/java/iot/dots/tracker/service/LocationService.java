package iot.dots.tracker.service;

/**
 * Created by ramindu on 3/17/17.
 */

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.Geometry;
import iot.dots.tracker.service.bean.Device;
import iot.dots.tracker.service.bean.GeoLocation;
import iot.dots.tracker.service.utils.GeometryUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

// Specifies the path to the RESTful service
@Path("/service")
public class LocationService {
    // Specifies that the method processes HTTP GET requests
    private static Gson gson = new Gson();
    private CloseableHttpClient httpClient = null;
    private String auth;
    private byte[] encodedAuth;
    private String authHeader;
    private Logger log = LoggerFactory.getLogger(LocationService.class);
    private int distanceRadiusInMeters = 20;

    public LocationService() {
        auth = "admin:admin";
        encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
        authHeader = "Basic " + new String(encodedAuth, Charset.forName("UTF-8"));
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            String currentDir = System.getProperty("user.dir");
//            String currentDir = "/Users/ramindu/wso2/supportFolder/IotDots/wso2iot-3.1.0/wso2/analytics";
            TrustManager[] trustManagers =
                    getTrustManagers("jks", new FileInputStream(
                            new File(currentDir + "/repository/resources/security/client-truststore.jks")),
                                     "wso2carbon");
            KeyManager[] keyManagers =
                    getKeyManagers("jks", new FileInputStream(
                            new File(currentDir +  "/repository/resources/security/wso2carbon.jks")), "wso2carbon");
            ctx.init(keyManagers, trustManagers, new SecureRandom());
            HttpClientBuilder builder = HttpClientBuilder.create();
            SSLConnectionSocketFactory sslConnectionFactory =
                    new SSLConnectionSocketFactory(ctx, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            builder.setSSLSocketFactory(sslConnectionFactory);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", sslConnectionFactory)
                    .build();
            HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
            builder.setConnectionManager(ccm);
            httpClient = builder.build();
        } catch (NoSuchAlgorithmException | KeyManagementException | FileNotFoundException e) {
            System.out.println("Error while creating SSL context. "+ e.getMessage());
        } catch (Exception e) {
            System.out.println("Error while creating SSL context. "+ e.getMessage());
        }
    }

    private JSONObject doPOST(String host, String path, String contectType, String body, boolean isArray) {
        JSONObject resultObject = new JSONObject();
        URI uri = null;
        HttpResponse response;
        try {
            uri = new URIBuilder()
                    .setScheme("https")
                    .setHost(host)
                    .setPath(path)
                    .build();

            StringEntity stringEntity = new StringEntity(body);
            HttpPost post = new HttpPost(uri);
            post.addHeader("content-type", contectType);
            post.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
            post.setEntity(stringEntity);
            response = httpClient.execute(post);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();

            switch (status) {
                case 200:
                    if (entity != null) {
                        InputStream instream = entity.getContent();
                        String result = convertStreamToString(instream);
                        instream.close();
                        resultObject.put("success", true);
                        if (isArray) {
                            resultObject.put("result", new JSONArray(result));
                        } else {
                            resultObject.put("result", new JSONObject(result));
                        }
                    }
                    break;
                default:
                    resultObject.put("success", false);
                    resultObject.put("result", response.getStatusLine().getReasonPhrase());
                    break;
            }

        } catch (URISyntaxException e) {
            log.error("URI generated for node url '" + host + path + "' is invalid.", e);
        } catch (JSONException | IOException e) {
            try {
                resultObject.put("success", false);
                resultObject.put("result", e.getMessage());
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }
        return resultObject;
    }

    public JSONObject doGet(String host, String path, String contectType, boolean isArray) {
        JSONObject resultObject = new JSONObject();
        URI uri = null;
        HttpResponse response;
        try {
            uri = new URIBuilder()
                    .setScheme("https")
                    .setHost(host)
                    .setPath(path)
                    .build();
            HttpGet get = new HttpGet(uri);
            get.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
            response = httpClient.execute(get);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            switch (status) {
                case 200:
                    if (entity != null) {
                        InputStream instream = entity.getContent();
                        String result = convertStreamToString(instream);
                        instream.close();
                        resultObject.put("success", true);
                        if (isArray) {
                            resultObject.put("result", new JSONArray(result));
                        } else {
                            resultObject.put("result", result);
                        }
                    }
                    break;
                default:
                    resultObject.put("success", false);
                    resultObject.put("result", response.getStatusLine().getReasonPhrase());
                    break;
            }
        } catch (URISyntaxException e) {
            log.error("URI generated for node url '" + host + path + "' is invalid.", e);
        } catch (JSONException | IOException e) {
            try {
                resultObject.put("success", false);
                resultObject.put("result", e.getMessage());
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }
        return resultObject;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/running_total")
    public Response getRunningTotalForDevice(Device device) {
        try {
            String body = "{\n"
                    + "\t\"tableName\":\""+device.getAnalyticsTableName()+"\", \n"
                    + "\t\"query\":\"id:"+ device.getId() +"\", \n"
                    + "\t\"start\":0, \n"
                    + "\t\"count\":1, \n"
                    + "\t\"columns\": [\"runningTotal\"]\n"
                    + "}";

            JSONObject result = doPOST("localhost:9445/", "analytics/search", "application/json", body, true);
            JSONArray distance = result.getJSONArray("result");
            return Response.status(200).
                    entity(distance.getJSONObject(0).getJSONObject("values").getDouble("runningTotal")).build();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Response.status(500).entity("internal error occurred.").build();
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/distances")
    public Response getDistancesForGepLocations(GeoLocation geoLocation) {

        JSONArray response = new JSONArray();

        //get total count
        JSONObject result = doGet("localhost:9445/", "analytics/tables/"+geoLocation.getAnalyticsTableName()+"/recordcount",
                                  "application/json", false);

        try {
            int count = Integer.parseInt(result.getString("result").replace("\n", ""));
            ArrayList<Long> startTimestamps = new ArrayList<Long>();
            ArrayList<Long> endTimestamps = new ArrayList<Long>();

            String body = "{\n"
                    + "\t\"tableName\":\""+geoLocation.getAnalyticsTableName()+"\", \n"
                    + "\t\"query\":\"id:" + geoLocation.getId() + " AND "
                    + "latitude1:[" + (geoLocation.getLatitude1() - 0.0001) + " TO "
                    + (geoLocation.getLatitude1() + 0.0001) + "] AND "
                    + "longitude1:[" + (geoLocation.getLongitude1() - 0.0001) + " TO "
                    + (geoLocation.getLongitude1() + 0.0001) + "]\", \n"
                    + "\t\"start\":0, \n"
                    + "\t\"count\":" + count + ", \n"
                    + "\t\"columns\": [\"distance\", \"date1\", \"timeStamp1\"], \n"
                    + "\t\"sortBy\" : [{\"field\" : \"timeStamp1\", \"sortType\" : \"ASC\"}]\n"
                    + "}";

            JSONObject startingLocations = doPOST("localhost:9445/", "analytics/search", "application/json", body,
                                                  true);
            JSONArray locations = startingLocations.getJSONArray("result");
            long startingTimestamp = 0L;
            for (int i = 0; i < locations.length(); i++) {
                long tempTimestamp = locations.getJSONObject(i).getJSONObject("values").getLong("timeStamp1");
                if (!startTimestamps.contains(tempTimestamp)) {
                    startTimestamps.add(tempTimestamp);
                }
            }

            String body2 = "{\n"
                    + "\t\"tableName\":\""+geoLocation.getAnalyticsTableName()+"\", \n"
                    + "\t\"query\":\"id:" + geoLocation.getId() + " AND "
                    + "latitude2:[" + (geoLocation.getLatitude2() - 0.0001) + " TO "
                    + (geoLocation.getLatitude2() + 0.0001) + "] AND "
                    + "longitude2:[" + (geoLocation.getLongitude2() - 0.0001) + " TO "
                    + (geoLocation.getLongitude2() + 0.0001) + "]\", \n"
                    + "\t\"start\":0, \n"
                    + "\t\"count\":" + count + ", \n"
                    + "\t\"columns\": [\"distance\", \"date1\", \"timeStamp2\"], \n"
                    + "\t\"sortBy\" : [{\"field\" : \"timeStamp2\", \"sortType\" : \"ASC\"}]\n"
                    + "}";

            JSONObject endingLocations = doPOST("localhost:9445/", "analytics/search", "application/json", body2, true);
            JSONArray locations2 = endingLocations.getJSONArray("result");
            long endingTimestamp = 0L;
            for (int i = 0; i < locations2.length(); i++) {
                long tempTimestamp = locations2.getJSONObject(i).getJSONObject("values").getLong("timeStamp2");
                if (!endTimestamps.contains(tempTimestamp)) {
                    endTimestamps.add(tempTimestamp);
                }
            }
            Collections.sort(startTimestamps);
            Collections.sort(endTimestamps);
            System.out.println(startingTimestamp + " - " + endingTimestamp);

            JSONArray completePaths = new JSONArray();

            long[][] possibleDoubles = getPossibleDoubles(startTimestamps, endTimestamps);
            for (int i=0; i<possibleDoubles[0].length && possibleDoubles[0][i] != 0 && possibleDoubles[1][i] != 0;
                 i++) {
                String body3 = "{\n"
                        + "\t\"tableName\":\""+geoLocation.getAnalyticsTableName()+"\", \n"
                        + "\t\"query\":\"id:" + geoLocation.getId() + " AND "
                        + "timeStamp1:[" + possibleDoubles[0][i] + " TO " + possibleDoubles[1][i] + "] AND "
                        + "timeStamp2:[" + possibleDoubles[0][i] + " TO " + possibleDoubles[1][i] + "]\", \n"
                        + "\t\"start\":0, \n"
                        + "\t\"count\":" + count + ", \n"
                        + "\t\"columns\": [\"distance\", \"date1\", \"timeStamp1\", \"latitude1\", \"latitude2\", "
                        + "\"longitude1\", \"longitude2\"], \n"
                        + "\t\"sortBy\" : [{\"field\" : \"timeStamp1\", \"sortType\" : \"ASC\"}]\n"
                        + "}";


                JSONObject result2 = doPOST("localhost:9445/", "analytics/search", "application/json", body3, true);
                JSONArray path = result2.getJSONArray("result");
                boolean successPath = true;
                JSONArray tempPath = new JSONArray();
                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                for (int j=0; j<path.length(); j++) {
                    JSONObject lat_log = new JSONObject();
                    JSONObject current = path.getJSONObject(j);
                    if (j==0) {
                        lat_log.put("latitude", current.getJSONObject("values").getDouble("latitude1"));
                        lat_log.put("longitude", current.getJSONObject("values").getDouble("longitude1"));
                        tempPath.put(lat_log);
                    }
                    lat_log.put("latitude", current.getJSONObject("values").getDouble("latitude2"));
                    lat_log.put("longitude", current.getJSONObject("values").getDouble("longitude2"));
                    tempPath.put(lat_log);

                    if(path.length() != j+1) {
                        JSONObject next = path.getJSONObject(j+1);
                        if ((current.getJSONObject("values").getDouble("latitude2") !=
                                next.getJSONObject("values").getDouble("latitude1") ||
                                current.getJSONObject("values").getDouble("longitude2") !=
                                        next.getJSONObject("values").getDouble("longitude1")) &&
                                ( current.getJSONObject("values").getDouble("distance") != 0 &&
                                        next.getJSONObject("values").getDouble("distance") != 0)) {
                            System.out.println("CULPRIT");
                            System.out.println(current.toString());
                            System.out.println(next.toString());
                            successPath = false;
                            System.out.println("Success FALSE");
                            break;
                        }
                    }
                }
                if (successPath) {
                    JSONObject successPathObj = new JSONObject();
                    successPathObj.put("path", tempPath);
//                    System.out.println(tempPath.toString());
//                    String body4 = "{\n"
//                            + "\t\"tableName\" : \""+geoLocation.getAnalyticsTableName()+"\",\n"
//                            + "\t\"fieldName\" : \"id\",\n"
//                            + "\t\"categoryPath\" : [],\n"
//                            + "\t\"query\" : \"timeStamp1:["+ possibleDoubles[0][i] +" TO "+ possibleDoubles[1][i] +"] "
//                            +   "AND timeStamp2:["+ possibleDoubles[0][i] +" TO "+ possibleDoubles[1][i] +"] AND "
//                            + "id:" + geoLocation.getId() + "\",\n"
//                            + "\t\"scoreFunction\" : \"distance\"\n"
//                            + "}";
//                    JSONObject result3 = doPOST("localhost:9445/", "analytics/facets", "application/json", body4, false);
//                    System.out.println(result3.toString());
//                    successPathObj.put("total_distance", result3.getJSONObject("result").getJSONObject("categories")
//                            .getLong(geoLocation.getId()));
//                    System.out.println(result3.getJSONObject("result").getJSONObject("categories")
//                                               .getLong(geoLocation.getId()));

                    String body4 = "{\n"
                                + "\t\"tableName\":\""+geoLocation.getAnalyticsTableName()+"\",\n"
                                + "\t\"parentPath\": [],\n"
                                + "\t\"aggregateLevel\": 0,\n"
                                + "\t\"groupByField\":\"id\",\n"
                                + "\t\"query\":\"timeStamp1:["+ possibleDoubles[0][i] +" TO "+ possibleDoubles[1][i] +"] AND "
                                + "timeStamp2:["+ possibleDoubles[0][i] +" TO "+ possibleDoubles[1][i] +"] AND "
                                + "id:"+ geoLocation.getId() +"\",\n"
                                + "\t\"aggregateFields\":[\n"
                                + "\t\t{\n"
                                + "\t    \t\"fieldName\":\"distance\",\n"
                                + "\t    \t\"aggregate\":\"SUM\",\n"
                                + "\t    \t\"alias\":\"total_distance_travelled\"\n"
                                + "\t\t}\n"
                                + "\t]\n"
                                + "}";
                    JSONObject result3 = doPOST("localhost:9445/", "analytics/aggregates", "application/json", body4,
                                                true);
                    System.out.println(result3.toString());
                    successPathObj.put("total_distance", result3.getJSONArray("result").getJSONObject(0)
                            .getJSONObject("values").getDouble("total_distance_travelled"));
                    System.out.println(result3.getJSONArray("result").getJSONObject(0)
                                               .getJSONObject("values").getDouble("total_distance_travelled"));

                    response.put(successPathObj);
                    //get the path !! :D
                }
            }

            return Response.status(200).entity(response.toString()).build();

        } catch (JSONException e) {
            e.printStackTrace();
            return Response.status(500).entity(e.getMessage()).build();
        }
        //get nearby starting points
    }

    private long[][] getPossibleDoubles(ArrayList<Long> startTimestamps, ArrayList<Long> endTimestamps) {
        System.out.println(startTimestamps.toString());
        System.out.println(endTimestamps.toString());
        long[][] possibleDoubles;
        int pos = 0;
        if (startTimestamps.size() > endTimestamps.size()) {
            possibleDoubles = new long[2][startTimestamps.size()];
        } else {
            possibleDoubles = new long[2][endTimestamps.size()];
        }
        for (int i = 0; i < startTimestamps.size(); i++) {
            long current = startTimestamps.get(i);
            long next;
            if (startTimestamps.size() == i+1) {
                next = -1;
            } else {
                next = startTimestamps.get(i+1);
            }
            for (int j = 0; j < endTimestamps.size(); j++) {
                long end = endTimestamps.get(j);
                if (end > current && (end < next || next == -1)) {
                    possibleDoubles[0][pos] = current;
                    possibleDoubles[1][pos] = end;
                    System.out.println(possibleDoubles[0][pos] + " *** " + possibleDoubles[1][pos]);
                    pos++;
                }
            }
        }
        System.out.println("possibile doubles");
        System.out.println(Arrays.deepToString(possibleDoubles));
        return possibleDoubles;
    }


    public static void main(String[] args) {
        LocationService locationService = new LocationService();
        GeoLocation geoLocation = new GeoLocation();
        geoLocation.setLatitude1(34.037112);
        geoLocation.setLatitude2(34.041718);
        geoLocation.setLongitude1(-84.456992);
        geoLocation.setLongitude2(-84.451965);
        geoLocation.setId("0004");
//        geoLocation.setAnalyticsTableName("DISTANCESTREAM12");
        geoLocation.setAnalyticsTableName("IOT_DOTS_POINTDISTANCESTREAM");
        locationService.getDistancesForGepLocations(geoLocation);

//        Device device = new Device();
//        device.setId("0004");
//        device.setAnalyticsTableName("RUNNING_TOTAL");
//        locationService.getRunningTotalForDevice(device);
    }

    public Geometry getCurrentGeometry(Object[] data, boolean point) {
        if (point) {
            double longitude = (Double) data[0];
            double latitude = (Double) data[1];
            return GeometryUtils.createPoint(longitude, latitude);
        } else {
            return GeometryUtils.createGeometry(data[0]);
        }
    }

    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    protected static KeyManager[] getKeyManagers(String keyStoreType, InputStream keyStoreFile,
                                                 String keyStorePassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(keyStoreFile, keyStorePassword.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyStorePassword.toCharArray());
        return kmf.getKeyManagers();
    }

    protected static TrustManager[] getTrustManagers(String trustStoreType, InputStream trustStoreFile,
                                                     String trustStorePassword) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(trustStoreType);
        trustStore.load(trustStoreFile, trustStorePassword.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf.getTrustManagers();
    }
}
