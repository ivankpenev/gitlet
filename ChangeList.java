package gitlet;

import java.io.File;
import java.util.HashMap;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author ivanp
 */
public class ChangeList extends HashMap<String, String> {
    /**
     * a.
     */
    private String _name;

    /**
     * a.
     * @param name a
     */
    ChangeList(String name) {
        super();
        _name = name;
    }

    /**
     * a.
     * @param parentDir a
     */
    public void writeToRepo(String parentDir) {
        File repo = new File(parentDir, ".gitlet");
        File stagingFile = Utils.join(repo, _name
                + ".staging");
        Utils.writeObject(stagingFile, this);
    }

    /**
     * a.
     * @param parentDir a
     * @param name a
     * @return a
     */
    static ChangeList readFromRepo(String parentDir, String name) {
        File repo = new File(parentDir, ".gitlet");
        File stagingFile = Utils.join(repo,
                name + ".staging");
        ChangeList result = Utils.readObject(stagingFile,
                ChangeList.class);
        return result;
    }

    /**
     * a.
     * @param workingDir a
     * @param fileName a
     */
    public void removeFile(String workingDir,
                           String fileName) {
        this.remove(fileName);
        this.writeToRepo(workingDir);
    }
}
