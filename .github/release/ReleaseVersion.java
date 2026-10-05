import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validates release tags and writes the release policy as GitHub step outputs. */
public class ReleaseVersion {
    private static final String NUMBER = "(0|[1-9][0-9]*)";
    private static final Pattern TAG = Pattern.compile(
            "v" + NUMBER + "\\." + NUMBER + "\\." + NUMBER + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?");

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Expected: tag published-stable-tags-file output-file");
        }
        Version version = parse(args[0]);
        boolean latest = version.prerelease() == null;
        boolean advanceMajor = latest;
        for (String tag : Files.readAllLines(Path.of(args[1]))) {
            if (tag.isBlank()) {
                continue;
            }
            Version previous = parse(tag);
            if (previous.prerelease() != null) {
                throw new IllegalArgumentException("Expected published stable tags only: " + tag);
            }
            if (compare(previous, version) > 0) {
                latest = false;
                if (previous.major().equals(version.major())) {
                    advanceMajor = false;
                }
            }
        }
        Files.writeString(Path.of(args[2]), "tag=" + args[0] + "\nmajor=v" + version.major()
                + "\nprerelease=" + (version.prerelease() != null) + "\nlatest=" + latest
                + "\nadvance_major=" + advanceMajor + "\n");
    }

    private static Version parse(String tag) {
        Matcher matcher = TAG.matcher(tag);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Use vMAJOR.MINOR.PATCH with an optional prerelease suffix: " + tag);
        }
        String prerelease = matcher.group(4);
        if (prerelease != null) {
            for (String part : prerelease.split("\\.")) {
                if (part.matches("[0-9]+") && part.length() > 1 && part.startsWith("0")) {
                    throw new IllegalArgumentException("Numeric prerelease identifiers cannot have leading zeroes: " + tag);
                }
            }
        }
        return new Version(new BigInteger(matcher.group(1)), new BigInteger(matcher.group(2)),
                new BigInteger(matcher.group(3)), prerelease);
    }

    private static int compare(Version first, Version second) {
        int major = first.major().compareTo(second.major());
        int minor = first.minor().compareTo(second.minor());
        return major != 0 ? major : minor != 0 ? minor : first.patch().compareTo(second.patch());
    }

    private record Version(BigInteger major, BigInteger minor, BigInteger patch, String prerelease) {
    }
}
