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
            System.out.println("FirmwareDownloader: Downloading releases file...");
            downloadFile(RELEASES_URL, RELEASES_JSON);
            System.out.println("FirmwareDownloader: Releases file downloaded.");
        }
        loadLocalReleasesFile();
        System.out.println("FirmwareDownloader: Found " + mFirmwares.size() + " firmware files.");
    }

    private boolean isFileAlreadyDownloaded(String path) {
        File file = new File(RELEASES_DIR, path);
        return file.exists() && file.length() > 0;
    }

    private boolean isReleasesFileTooOld () {
        if (releasesFile.exists() && releasesFile.length() > 0) {
            return System.currentTimeMillis() - releasesFile.lastModified() > MAX_FILE_AGE;
        }
        return false;
    }

    private void loadLocalReleasesFile() {
        System.out.println("FirmwareDownloader: Loading local releases file...");
        try {
            String input = new String(Bootloader.readFile(releasesFile));
            System.out.println("FirmwareDownloader: Releases JSON loaded from local file.");
            mFirmwares = parseJson(input);
        } catch (IOException ioe) {
            System.out.println("FirmwareDownloader: Problems loading JSON file.");
        }
    }

    public void downloadFirmware(Firmware selectedFirmware) throws IOException {
        if (selectedFirmware != null) {
            //TODO: if asset does not exist
            System.out.println("FirmwareDownloader: Downloading firmware " + selectedFirmware.getTagName() + "...");
            if (isFileAlreadyDownloaded(selectedFirmware.getTagName() + "/" + selectedFirmware.getAssetName())) {
                return;
            }
            String browserDownloadUrl = selectedFirmware.getBrowserDownloadUrl();
            downloadFile(browserDownloadUrl, selectedFirmware.getAssetName());
        } else {
            System.out.println("FirmwareDownloader: Selected firmware does not have assets.");
            return;
        }
    }

    private void downloadFile(String url, String filename) {
        System.out.println("FirmwareDownloader: Downloading file " + filename + "...");
        CloseableHttpClient client = HttpClients.createDefault();
        try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            System.out.println("FirmwareDownloader: The status code is: " + statusCode);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                writeFile(new File(RELEASES_DIR, filename), entity);
            }
        } catch (ClientProtocolException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void writeFile(File file, HttpEntity entity) {
        System.out.println("FirmwareDownloader: Writing file " + file + "...");
        File releasesDir = new File(RELEASES_DIR);
        releasesDir.mkdirs();
        try (FileOutputStream outstream = new FileOutputStream(file)) {
            entity.writeTo(outstream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
