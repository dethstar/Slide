package me.ccrama.redditslide;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;

import net.dean.jraw.models.Submission;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by ccrama on 5/26/2015.
 */
public class ContentType {
    /**
     * Checks if {@code host} is contains by any of the provided {@code bases}
     * <p/>
     * For example "www.youtube.com" contains "youtube.com" but not "notyoutube.com" or
     * "youtube.co.uk"
     *
     * @param host  A hostname from e.g. {@link URI#getHost()}
     * @param bases Any number of hostnames to compare against {@code host}
     * @return If {@code host} contains any of {@code bases}
     */
    public static boolean hostContains(String host, String... bases) {
        if (host == null || host.isEmpty()) return false;

        for (String base : bases) {
            if (base == null || base.isEmpty()) continue;

            final int index = host.indexOf(base);
            if (index < 0 || index + base.length() != host.length()) continue;
            if (base.length() == host.length() || host.charAt(index - 1) == '.') return true;
        }

        return false;
    }

    public static boolean isGif(URI uri) {
        try {
            final String host = uri.getHost().toLowerCase();
            final String path = uri.getPath().toLowerCase();

            return hostContains(host, "gfycat.com")
                    || path.endsWith(".gif")
                    || path.endsWith(".gifv")
                    || path.endsWith(".webm")
                    || path.endsWith(".mp4");

        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean isImage(URI uri) {
        try {
            final String host = uri.getHost().toLowerCase();
            final String path = uri.getPath().toLowerCase();

            return host.equals("i.reddituploads.com")
                    || path.endsWith(".png")
                    || path.endsWith(".jpg")
                    || path.endsWith(".jpeg");

        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean isAlbum(URI uri) {
        try {
            final String host = uri.getHost().toLowerCase();
            final String path = uri.getPath().toLowerCase();

            return hostContains(host, "imgur.com", "bildgur.de")
                    && (path.startsWith("/a/")
                    || path.startsWith("/gallery/")
                    || path.startsWith("/g/")
                    || path.contains(","));

        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean isVideo(URI uri) {
        try {
            final String host = uri.getHost().toLowerCase();
            final String path = uri.getPath().toLowerCase();

            return Reddit.videoPlugin
                    && hostContains(host, "youtu.be", "youtube.com", "youtube.co.uk")
                    && !path.contains("/user/");

        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean isImgurLink(String url) {
        try {
            final URI uri = new URI(url);
            final String host = uri.getHost().toLowerCase();

            return hostContains(host, "imgur.com", "bildgur.de")
                    && !isAlbum(uri)
                    && !isGif(uri)
                    && !isImage(uri);

        } catch (URISyntaxException | NullPointerException e) {
            return false;
        }
    }

    /**
     * Attempt to determine the content type of a link from the URL
     *
     * @param url URL to get ContentType from
     * @return ContentType of the URL
     */
    public static Type getContentType(String url) {
        if (!url.startsWith("//") && ((url.startsWith("/") && url.length() < 4)
                || url.startsWith("#spoil")
                || url.startsWith("/spoil")
                || url.startsWith("#s-")
                || url.equals("#s")
                || url.equals("#ln")
                || url.equals("#b")
                || url.equals("#sp"))) {
            return Type.SPOILER;
        }

        if (url.startsWith("//")) url = "https:" + url;
        if (url.startsWith("/")) url = "reddit.com" + url;
        if (!url.contains("://")) url = "http://" + url;

        try {
            final URI uri = new URI(url);
            final String host = uri.getHost().toLowerCase();
            final String scheme = uri.getScheme().toLowerCase();

            if (!scheme.equals("http") && !scheme.equals("https")) {
                return Type.EXTERNAL;
            }
            if (isVideo(uri)) {
                return Type.VIDEO;
            }
            if (PostMatch.openExternal(url)) {
                return Type.EXTERNAL;
            }
            if (isGif(uri)) {
                return Type.GIF;
            }
            if (isImage(uri)) {
                return Type.IMAGE;
            }
            if (isAlbum(uri)) {
                return Type.ALBUM;
            }
            if (hostContains(host, "imgur.com", "bildgur.de")) {
                return Type.IMGUR;
            }
            if (hostContains(host, "xkcd.com") && !hostContains("imgs.xkcd.com") && !hostContains("what-if.xkcd.com")) {
                return Type.XKCD;
            }
            if (hostContains(host, "reddit.com", "redd.it")) {
                return Type.REDDIT;
            }
            if (hostContains(host, "vid.me")) {
                return Type.VID_ME;
            }
            if (hostContains(host, "deviantart.com")) {
                return Type.DEVIANTART;
            }
            if (hostContains(host, "streamable.com")) {
                return Type.STREAMABLE;
            }

            return Type.LINK;

        } catch (URISyntaxException | NullPointerException e) {
            if (e.getMessage() != null && e.getMessage().contains("Illegal character in fragment")) //a valid link but something un-encoded in the URL
                return Type.LINK;
            return Type.NONE;
        }
    }

    /**
     * Attempts to determine the content of a submission, mostly based on the URL
     *
     * @param submission Submission to get the content type from
     * @return Content type of the Submission
     * @see #getContentType(String)
     */
    public static Type getContentType(Submission submission) {
        if (submission == null)
            return Type.SELF; //hopefully shouldn't be null, but catch it in case

        final String url = submission.getUrl();
        final Type basicType = getContentType(url);

        if (submission.isSelfPost()) {
            return Type.SELF;
        }
        // TODO: Decide whether internal youtube links should be EMBEDDED or LINK
        if (basicType.equals(Type.LINK)
                && submission.getDataNode().has("media_embed")
                && submission.getDataNode().get("media_embed").has("content")) {
            return Type.EMBEDDED;
        }

        return basicType;
    }

    public static boolean displayImage(Type t) {
        switch (t) {

            case ALBUM:
            case DEVIANTART:
            case IMAGE:
            case XKCD:
            case IMGUR:
            case SELF:
                return true;
            default:
                return false;

        }
    }

    public static boolean fullImage(Type t) {
        switch (t) {

            case ALBUM:
            case DEVIANTART:
            case GIF:
            case IMAGE:
            case IMGUR:
            case STREAMABLE:
            case XKCD:
            case VIDEO:
            case SELF:
            case VID_ME:
                return true;

            case EMBEDDED:
            case EXTERNAL:
            case LINK:
            case NONE:
            case REDDIT:
            case SPOILER:
            default:
                return false;

        }
    }

    public static boolean mediaType(Type t) {
        switch (t) {
            case ALBUM:
            case DEVIANTART:
            case GIF:
            case IMAGE:
            case XKCD:
            case IMGUR:
            case STREAMABLE:
            case VID_ME:
                return true;
            default:
                return false;

        }
    }

    /**
     * Returns a string identifier for a submission e.g. Link, GIF, NSFW Image
     *
     * @param submission Submission to get the description for
     * @return the String identifier
     */
    private static int getContentID(Submission submission) {
        return getContentID(getContentType(submission), submission.isNsfw());
    }

    public static int getContentID(Type contentType, boolean nsfw) {
        if (nsfw) {
            switch (contentType) {
                case ALBUM:
                    return R.string.type_nsfw_album;
                case EMBEDDED:
                    return R.string.type_nsfw_emb;
                case EXTERNAL:
                    return R.string.type_nsfw_link;
                case GIF:
                    return R.string.type_nsfw_gif;
                case IMAGE:
                    return R.string.type_nsfw_img;
                case IMGUR:
                    return R.string.type_nsfw_imgur;
                case LINK:
                    return R.string.type_nsfw_link;
                case VIDEO:
                case VID_ME:
                    return R.string.type_nsfw_video;
            }
        } else {
            switch (contentType) {
                case ALBUM:
                    return R.string.type_album;
                case XKCD:
                    return R.string.type_xkcd;
                case DEVIANTART:
                    return R.string.type_deviantart;
                case EMBEDDED:
                    return R.string.type_emb;
                case EXTERNAL:
                    return R.string.type_external;
                case GIF:
                    return R.string.type_gif;
                case IMAGE:
                    return R.string.type_img;
                case IMGUR:
                    return R.string.type_imgur;
                case LINK:
                    return R.string.type_link;
                case NONE:
                    return R.string.type_title_only;
                case REDDIT:
                    return R.string.type_reddit;
                case SELF:
                    return R.string.type_selftext;
                case STREAMABLE:
                    return R.string.type_streamable;
                case VIDEO:
                    return R.string.type_youtube;
                case VID_ME:
                    return R.string.type_vidme;
            }
        }
        return R.string.type_link;
    }

    static HashMap<String, String> contentDescriptions = new HashMap<>();

    /**
     * Returns a description of the submission, for example "Link", "NSFW link", if the link is set
     * to open externally it returns the package name of the app that opens it, or "External"
     *
     * @param submission The submission to describe
     * @param context    Current context
     * @return The content description
     */
    public static String getContentDescription(Submission submission, Context context) {
        final int generic = getContentID(submission);
        final Resources res = context.getResources();
        final String domain = submission.getDomain();

        if (generic != R.string.type_external) {
            return res.getString(generic);
        }

        if (contentDescriptions.containsKey(domain)) {
            return contentDescriptions.get(domain);
        }

        try {
            final PackageManager pm = context.getPackageManager();
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(submission.getUrl()));
            final String packageName = pm.resolveActivity(intent, 0).activityInfo.packageName;
            String description;

            if (!packageName.equals("android")) {
                description = pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString();
            } else {
                description = res.getString(generic);
            }

            // Looking up a package name takes a long time (3~10ms), memoize it
            contentDescriptions.put(domain, description);
            return description;
        } catch (PackageManager.NameNotFoundException | NullPointerException e) {
            contentDescriptions.put(domain, res.getString(generic));
            return res.getString(generic);
        }
    }

    public static boolean isImgurImage(String lqUrl) {
        try {
            final URI uri = new URI(lqUrl);
            final String host = uri.getHost().toLowerCase();
            final String path = uri.getPath().toLowerCase();

            return (host.contains("imgur.com") || host.contains("bildgur.de"))
                    && (path.endsWith(".png")
                    || path.endsWith(".jpg")
                    || path.endsWith(".jpeg"));

        } catch (Exception e) {
            return false;
        }
    }

    public enum Type {
        ALBUM,
        DEVIANTART,
        EMBEDDED,
        EXTERNAL,
        GIF,
        IMAGE,
        IMGUR,
        LINK,
        NONE,
        REDDIT,
        SELF,
        SPOILER,
        STREAMABLE,
        VIDEO,
        XKCD,
        VID_ME
    }
}
