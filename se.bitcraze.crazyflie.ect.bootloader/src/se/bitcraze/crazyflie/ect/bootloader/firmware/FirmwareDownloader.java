package se.bitcraze.crazyflie.ect.bootloader.firmware;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import se.bitcraze.crazyflie.lib.bootloader.Bootloader;

public class FirmwareDownloader {

    public static final String RELEASES_URL = "https://api.github.com/repos/bitcraze/crazyflie-release/releases";
    public static final String RELEASES_DIR = "crazyflie-firmware";
    public static final String RELEASES_JSON = "cf_releases.json";
    private static final long MAX_FILE_AGE = 21600000; // 21600 seconds => 6 hours

    private File releasesFile = new File(RELEASES_DIR, RELEASES_JSON);

    private List<Firmware> mFirmwares = new ArrayList<Firmware>();

    public FirmwareDownloader() {
    }

    public void checkForFirmwareUpdate() {
        System.out.println("FirmwareDownloader: Checking for updates...");
        if (!isFileAlreadyDownloaded(RELEASES_JSON) || isReleasesFileTooOld()) {
            downloadReleasesFile();
        } else {
            loadLocalReleasesFile();
        }
        System.out.println("FirmwareDownloader: Found " + mFirmwares.size() + " firmware files.");
    }

    private boolean isFileAlreadyDownloaded(String path) {
        File firmwareFile = new File(RELEASES_DIR, path);
        return firmwareFile.exists() && firmwareFile.length() > 0;
    }

    private boolean isReleasesFileTooOld () {
        if (releasesFile.exists() && releasesFile.length() > 0) {
            return System.currentTimeMillis() - releasesFile.lastModified() > MAX_FILE_AGE;
        }
        return false;
    }

    private void downloadReleasesFile() {
        String input = null;
        try {
            input = downloadUrl(RELEASES_URL);
            System.out.println("FirmwareDownloader: Releases JSON downloaded.");
            mFirmwares = parseJson(input);
        } catch (IOException ioe) {
//                Log.d(LOG_TAG, ioe.getMessage());
            System.out.println("FirmwareDownloader: Unable to retrieve web page. Check your connectivity.");
//                } catch (JSONException je) {
//                  Log.d(LOG_TAG, je.getMessage());
//                  System.out.println("FirmwareDownloader: Error during parsing JSON content.");
        }

        // Write JSON to disk
        try {
            writeFile(input, RELEASES_JSON);
//                Log.d(LOG_TAG, "Wrote JSON file.");
        } catch (IOException ioe) {
//                Log.d(LOG_TAG, ioe.getMessage());
            System.out.println("FirmwareDownloader: Unable to save JSON file.");
        }
    }

    private void loadLocalReleasesFile() {
        try {
            String input = new String(Bootloader.readFile(releasesFile));
            System.out.println("FirmwareDownloader: Releases JSON loaded from local file.");
            mFirmwares = parseJson(input);
//        } catch (JSONException jsone) {
//            Log.d(LOG_TAG, jsone.getMessage());
//            System.out.println("FirmwareDownloader: Error while parsing JSON content.");
        } catch (IOException ioe) {
//            Log.d(LOG_TAG, ioe.getMessage());
            System.out.println("FirmwareDownloader: Problems loading JSON file.");
        }
    }

    private void writeFile(String input, String filename) throws IOException {
        File releasesDir = new File(RELEASES_DIR);
        releasesDir.mkdirs();
        if (!releasesFile.exists()) {
            releasesFile.createNewFile();
        }
        PrintWriter out = new PrintWriter(releasesFile);
        out.println(input);
        out.flush();
        out.close();
    }

    public void downloadFirmware(Firmware selectedFirmware) throws IOException {
        if (selectedFirmware != null) {
            //TODO: if asset does not exist
            
            if (isFileAlreadyDownloaded(selectedFirmware.getTagName() + "/" + selectedFirmware.getAssetName())) {
                return;
            }

            String browserDownloadUrl = selectedFirmware.getBrowserDownloadUrl();
            String downloadedFw = downloadUrl(browserDownloadUrl);
            if (downloadedFw != null) {
                writeFile(downloadedFw, selectedFirmware.getAssetName());
            } else {
                System.out.println("FirmwareDownloader: Empty download.");
            }
        } else {
            System.out.println("FirmwareDownloader: Selected firmware does not have assets.");
            return;
        }
    }

    private String downloadUrl(String myUrl) throws IOException {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(myUrl);
        CloseableHttpResponse response = httpclient.execute(httpGet);
        
        try {
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();

            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                reader = new BufferedReader(new InputStreamReader(content, "UTF-8"));

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                EntityUtils.consume(entity);
            } else {
                System.out.println("FirmwareDownloader: The response is: " + response);
                return "The response is: " + response;
            }
        } finally {
            response.close();
            // Makes sure that the InputStream is closed after the app is finished using it.
            if (reader != null) {
                reader.close();
            }
        }
        return builder.toString();
    }

    private List<Firmware> parseJson(String input) throws JsonProcessingException, IOException {
        List<Firmware> firmwares = new ArrayList<Firmware>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(input);
        ArrayNode array;
        if (root.isArray()) {
            array = (ArrayNode) root;
            for (int i = 0; i < array.size(); i++) {
                JsonNode releaseObject = array.get(i);
                String tagName = releaseObject.get("tag_name").asText();
                String name = releaseObject.get("name").asText();
                String createdAt = releaseObject.get("created_at").asText();
                String body = releaseObject.get("body").asText();

                ArrayNode assetsArray = (ArrayNode) releaseObject.get("assets");
                if (assetsArray != null && assetsArray.size() > 0) {
                    for (int n = 0; n < assetsArray.size(); n++) {
                        JsonNode assetsObject = assetsArray.get(n);
                        String assetName = assetsObject.get("name").asText();
                        int size = assetsObject.get("size").asInt();
                        String downloadURL = assetsObject.get("browser_download_url").asText();
                        // hardcoded filter for DFU zip file (find a better way to handle this)
                        if (assetName.contains("_dfu")) {
                            continue;
                        }
                        Firmware firmware = new Firmware(tagName, name, createdAt);
                        firmware.setReleaseNotes(body);
                        firmware.setAsset(assetName, size, downloadURL);
                        firmwares.add(firmware);
                    }
                } else {
                    // Filter out firmwares without assets
                    System.out.println("FirmwareDownloader: Firmware " + tagName + " was filtered out, because it has no assets.");
                }

            }
        } else {
            System.out.println("FirmwareDownloader: JSON root node is not an array!");
        }
        return firmwares;
    }

    public List<Firmware> getFirmwares() {
        return mFirmwares;
    }

}
