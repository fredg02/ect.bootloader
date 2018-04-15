package se.bitcraze.crazyflie.ect.bootloader.firmware;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Firmware implements Comparable<Firmware> {

    private String mTagName;
    private String mName;
    private String mCreatedAt;

    private String mAssetName;
    private int mSize;
    private String mBrowserDownloadUrl;
    private String mReleaseNotes;

    private final SimpleDateFormat inputFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    private final SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public Firmware() {
    }

    public Firmware(String tagName, String name, String createdAt) {
        this.mTagName = tagName;
        this.mName = name;

        try {
            Date date = inputFormatter.parse(createdAt);
            this.mCreatedAt = outputFormatter.format(date);
        } catch (ParseException e) {
            this.mCreatedAt = createdAt;
        }
    }

    /**
     * The Git tag of this release
     * 
     * @return
     */
    public String getTagName() {
        return mTagName;
    }

    public String getName() {
        return mName;
    }

    /**
     * The creation date of this release
     * 
     * @return
     */
    public String getCreatedAt() {
        return mCreatedAt;
    }

    public void setAsset(String assetName, int assetSize, String URL) {
        this.mAssetName = assetName;
        this.mSize = assetSize;
        this.mBrowserDownloadUrl = URL;
    }

    /**
     * The file name
     * 
     * @return
     */
    public String getAssetName() {
        return mAssetName;
    }

    /**
     * The file size
     * 
     * @return
     */
    public int getAssetSize() {
        return mSize;
    }

    /**
     * The download URL
     * 
     * @return
     */
    public String getBrowserDownloadUrl() {
        return mBrowserDownloadUrl;
    }

    public void setReleaseNotes(String releaseNotes) {
        this.mReleaseNotes = releaseNotes;
    }

    public String getReleaseNotes() {
        return mReleaseNotes;
    }

    /**
     * Return optional(!) info contained in the file name<br/>
     * <br/>
     * crazyflie[-info]-YYYY.MM.zip<br/>
     * 
     * @return
     */
    public String getInfo() {
        String info = "";
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9]+(?:-([a-zA-Z0-9\\-]+))?-([0-9.]+)\\.zip$");
        Matcher matcher = pattern.matcher(mAssetName);
        if (matcher.find() && matcher.group(1) != null) {
            info = matcher.group(1);
        }
        return info;
    }
    
    /**
     * The type of firmware (either for CF1, CF2, or both)
     * 
     * @return "CF1", "CF2", "CF1 & CF2" or "Unknown"
     */
    public String getType() {
        // TODO: make this more reliable

        // additional whitelist based on the tag name, since CF1 support has been dropped with firmware 2018.01
        List<String> cf1cf2Whitelist = new ArrayList<String>();
        cf1cf2Whitelist.add("2016.02");
        cf1cf2Whitelist.add("2016.09");
        cf1cf2Whitelist.add("2016.11");
        cf1cf2Whitelist.add("2017.04");
        cf1cf2Whitelist.add("2017.05");
        cf1cf2Whitelist.add("2017.06");

        String lcAssetName = mAssetName.toLowerCase(Locale.US);
        if (lcAssetName.startsWith("cf1") || lcAssetName.startsWith("crazyflie1")) {
            return "CF1";
        } else if (lcAssetName.startsWith("cf2") || lcAssetName.startsWith("crazyflie2") || lcAssetName.startsWith("cflie2")) {
            return "CF2";
        } else if (lcAssetName.startsWith("crazyflie-") && cf1cf2Whitelist.contains(mTagName)) {
            return "CF1 & CF2";
        } else if (lcAssetName.startsWith("crazyflie-") && !cf1cf2Whitelist.contains(mTagName)) {
            return "CF2";
        } else {
            return "Unknown";
        }
    }

    @Override
    public String toString() {
        return "Firmware [mTagName=" + mTagName + ", mName=" + mName + ", mCreatedAt=" + mCreatedAt + ", mAssetName="
                + mAssetName + ", mSize=" + mSize + ", mBrowserDownloadUrl=" + mBrowserDownloadUrl + "]";
    }

    @Override
    public int compareTo(Firmware another) {
        return this.mTagName.compareTo(another.getTagName());
    }

}
