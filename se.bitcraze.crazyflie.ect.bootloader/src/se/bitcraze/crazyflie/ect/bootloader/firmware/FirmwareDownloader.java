package se.bitcraze.crazyflie.ect.bootloader.firmware;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import se.bitcraze.crazyflie.lib.bootloader.Bootloader;

public class FirmwareDownloader {

    public static final String RELEASES_URL = "https://api.github.com/repos/bitcraze/crazyflie-release/releases";
    public static final String RELEASES_DIR = "crazyflie-firmware"; // name of local directory where the JSON file and the firmware binaries are stored
    public static final String RELEASES_JSON = "cf_releases.json"; // name of the local JSON file

    private static final Logger mLogger = LoggerFactory.getLogger("FirmwareDownloader");
    private static final long MAX_FILE_AGE = 21600000; // 21600000 milliseconds => 6 hours
    private File releasesFile = new File(RELEASES_DIR, RELEASES_JSON);
    private List<Firmware> mFirmwares = new ArrayList<Firmware>();

    public FirmwareDownloader() {
        //Intentionally left blank
    }

    /**
     * Query GitHub API and download a JSON file from crazyflie-release GitHub repo
     * that contains all releases. Skip download when the file is already downloaded
     * and not too old. Load the local file and parse the JSON.
     * 
     * @return
     */
    public boolean checkForFirmwareUpdate() {
        mLogger.info("Checking for updates...");
        if (!isFileAlreadyDownloaded(RELEASES_JSON) || isReleasesFileTooOld()) {
            mLogger.info("Downloading releases file...");
            boolean successfulDownload = downloadFile(RELEASES_URL, RELEASES_JSON);
            if (!successfulDownload) {
                mLogger.info("Releases file could not be downloaded.");
                return false;
            }
            mLogger.info("Releases file downloaded.");
        }
        loadLocalReleasesFile();
        mLogger.info("Found {} firmware files.", mFirmwares.size());
        return true;
    }

    private boolean isFileAlreadyDownloaded(String path) {
        File file = new File(RELEASES_DIR, path);
        return file.exists() && file.length() > 0;
    }

    private boolean isReleasesFileTooOld() {
        if (releasesFile.exists() && releasesFile.length() > 0) {
            return System.currentTimeMillis() - releasesFile.lastModified() > MAX_FILE_AGE;
        }
        return false;
    }

    private void loadLocalReleasesFile() {
        mLogger.info("Loading local releases file...");
        try {
            String input = new String(Bootloader.readFile(releasesFile));
            mLogger.info("Local releases file loaded.");
            mFirmwares = parseJson(input);
        } catch (IOException ioe) {
            mLogger.error(ioe.getMessage());
        }
    }

    /**
     * Download the corresponding binary (e.g Zip file) of the specified firmware
     * 
     * @param selectedFw
     * @return true if found locally or if it's downloaded successfully, false otherwise
     * @throws IOException
     */
    public boolean downloadFirmware(Firmware selectedFw) throws IOException {
        if (selectedFw != null) {
            if (isFileAlreadyDownloaded(selectedFw.getAssetName())) {
                mLogger.info("Firmware {} found locally. Skipping download.", selectedFw.getTagName());
                return true;
            }
            mLogger.info("Downloading firmware {}...", selectedFw.getTagName());
            return downloadFile(selectedFw.getBrowserDownloadUrl(), selectedFw.getAssetName());
        } else {
            mLogger.info("Selected firmware does not have assets.");
            return false;
        }
    }

    /**
     * Download file from given URL and save under given filename
     * 
     * @param url
     * @param filename
     * @return true if download is successful, false otherwise
     */
    private boolean downloadFile(String url, String filename) {
        mLogger.info("Downloading file {} from {}...", filename, url);
         // adds HTTP REDIRECT support to GET and POST methods
        CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
        
        try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            mLogger.info("Status code: {}", statusCode);
            //TODO: improve
            if (statusCode == 404) {
                return false;
            }
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                writeFile(new File(RELEASES_DIR, filename), entity);
            }
        } catch (ClientProtocolException cpe) {
            mLogger.error(cpe.getMessage());
            return false;
        } catch (IOException ioe) {
            mLogger.error(ioe.getMessage());
            return false;
        }
        return true;
    }

    private void writeFile(File file, HttpEntity entity) {
        mLogger.info("Writing file {}...", file);
        File releasesDir = new File(RELEASES_DIR);
        releasesDir.mkdirs();
        try (FileOutputStream outstream = new FileOutputStream(file)) {
            entity.writeTo(outstream);
        } catch (FileNotFoundException fnfe) {
            mLogger.error(fnfe.getMessage());
        } catch (IOException ioe) {
            mLogger.error(ioe.getMessage());
        }
    }

    /**
     * Parses the JSON file
     * 
     * A release can have more than one Zip file attached (asset).<br/>
     * For every Zip file, a {@code firmware} object is created. 
     * 
     * 
     * @param input
     * @return
     * @throws JsonProcessingException
     * @throws IOException
     */
    private List<Firmware> parseJson(String input) throws JsonProcessingException, IOException {
        mLogger.info("Parsing JSON file...");
        List<Firmware> firmwares = new ArrayList<Firmware>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(input);
        if (root.isArray()) {
            ArrayNode array = (ArrayNode) root;
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
                    mLogger.info("Firmware {} was filtered out, because it has no assets.", tagName);
                }
            }
        } else {
            mLogger.error("JSON root node is not an array!");
        }
        return firmwares;
    }

    public List<Firmware> getFirmwares() {
        return mFirmwares;
    }

}
