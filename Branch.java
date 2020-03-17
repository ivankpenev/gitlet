package gitlet;

import java.io.File;
import java.io.Serializable;
/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author ivanp
 */
public class Branch implements Serializable {
    /**
     * a.
     */
    private String _head;
    /**
     * a.
     */
    private String _name;

    /**
     * a.
     * @param name a
     * @param head a
     */
    Branch(String name, String head) {
        _head = head;
        _name = name;
    }

    /**
     * a.
     * @param f a
     */
    Branch(File f) {
        Branch result = Utils.readObject(f, Branch.class);
        _head = result._head;
        _name = result._name;
    }

    /**
     * a.
     * @return a
     */
    String getHead() {
        return _head;
    }

    /**
     * a.
     * @return a
     */
    String getName() {
        return _name;
    }

    /**
     * a.
     * @param parentDir a
     */
    public void writeToRepo(String parentDir) {
        File repo = new File(parentDir, ".gitlet");
        File commitFile = Utils.join(repo, _name + ".branch");
        Utils.writeObject(commitFile, this);
    }

    /**
     * a.
     * @param newHead a
     */
    public void addCommit(Commit newHead) {
        if (!newHead.getParentCommit().equals(_head)) {
            throw new GitletException("Invalid "
                    + "commit. Parent not equal to "
                    + "current head.");
        }
        _head = newHead.getHash();
    }

    /**
     * a.
     * @param parentDir a
     * @return a
     */
    static Branch getCurrentBranch(String parentDir) {
        File repo = new File(parentDir, ".gitlet");
        File headFile = new File(repo.getAbsolutePath(), "HEAD");
        String head = Utils.readContentsAsString(headFile);
        File headBranch = new File(repo.getAbsolutePath(), head + ".branch");
        return new Branch(headBranch);
    }

    /**
     * a.
     * @param newHead a
     */
    public void setHead(String newHead) {
        _head = newHead;
    }

    /**
     * getting close.
     * @param name a
     */
    public void setName(String name) {
        _name = name;
    }
}
